package com.s15.codectest.measure;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.SystemClock;

import com.s15.codectest.Clip;
import com.s15.codectest.Config;
import com.s15.codectest.db.BatterySample;
import com.s15.codectest.db.LabDao;
import com.s15.codectest.db.LabDatabase;
import com.s15.codectest.db.LogEntry;
import com.s15.codectest.db.Session;
import com.s15.codectest.db.Trial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Runs a whole session: plays every trial in order, records the battery, and saves the results.
// Runs on MeasurementService's background thread, so waiting with Thread.sleep() is fine.
// Aborting = the thread gets interrupted, so let InterruptedException pass up (don't catch it).
//
// Owner: runner (Luca).
public class SessionRunner {

    // How often the cool-down and the pre-trial check look at the phone again.
    private static final long CHECK_EVERY_MS = 5000;

    private final Context context;
    private final LabDao db;
    private final BatteryMonitor battery;

    public SessionRunner(Context context) {
        this.context = context;
        this.db = LabDatabase.get(context);
        this.battery = new BatteryMonitor(context);
    }

    public void run(long sessionId) throws InterruptedException {
        Session session = db.getSession(sessionId);
        BatterySnapshot start = battery.snapshot();
        session.status = Session.RUNNING;
        session.startedAt = System.currentTimeMillis();
        session.startTempTenths = start.tempTenths;
        db.updateSession(session);
        log(session, null, "Session started. Battery " + start.levelPct + "%, " + start.tempC() + " °C");
        RunnerStatus.start(sessionId);

        try {
            boolean firstTrial = true;
            while (true) {
                List<Trial> trials = db.getTrials(sessionId);
                Trial next = findNextPending(trials);
                if (next == null) break;
                RunnerStatus.trialsDone = countFinished(trials);
                RunnerStatus.trialsTotal = trials.size();

                if (!firstTrial) coolDown(session);
                firstTrial = false;
                waitForConditions(session);
                runTrial(session, next);
                checkOutliers(session, next.clipFile);
            }

            session.status = Session.COMPLETED;
            session.endedAt = System.currentTimeMillis();
            db.updateSession(session);
            log(session, null, "Session finished.");
            RunnerStatus.trialsDone = RunnerStatus.trialsTotal;
            RunnerStatus.finish("Session complete");
        } catch (InterruptedException e) {
            markAborted(session);
            throw e;
        }
    }

    private void runTrial(Session session, Trial trial) throws InterruptedException {
        Clip clip = Clip.find(trial.clipFile);
        setVolume(Config.VOLUME_PERCENT);

        MediaPlayer player;
        try {
            player = startPlaying(clip);
        } catch (IOException e) {
            trial.status = Trial.FAILED;
            trial.note = "Could not play the file: " + e.getMessage();
            db.updateTrial(trial);
            log(session, trial.id, clip.label() + ": playback failed.");
            queueRerun(session, trial, "playback failed");
            return;
        }

        try {
            BatterySnapshot start = battery.snapshot();
            trial.status = Trial.RUNNING;
            trial.startedAt = System.currentTimeMillis();
            trial.startChargeUah = start.chargeCounterUah;
            trial.startTempTenths = start.tempTenths;
            db.updateTrial(trial);
            log(session, trial.id, clip.label() + " rep " + (trial.repetition + 1) + " started.");

            RunnerStatus.message = "Playing " + clip.label();
            RunnerStatus.clip = clip.label();
            RunnerStatus.trialElapsedMs = 0;
            RunnerStatus.trialDurationMs = session.trialDurationMs;
            RunnerStatus.violations = 0;

            long startTime = SystemClock.elapsedRealtime();
            List<BatterySample> samples = new ArrayList<>();
            trial.maxTempTenths = start.tempTenths;

            // Sampling loop: read the battery every Config.SAMPLE_EVERY_SECONDS until the trial is up.
            while (true) {
                long leftMs = session.trialDurationMs - (SystemClock.elapsedRealtime() - startTime);
                if (leftMs <= 0) break;
                Thread.sleep(Math.min(Config.SAMPLE_EVERY_SECONDS * 1000L, leftMs));

                BatterySnapshot now = battery.snapshot();
                long elapsedMs = SystemClock.elapsedRealtime() - startTime;

                BatterySample sample = new BatterySample();
                sample.trialId = trial.id;
                sample.elapsedMs = elapsedMs;
                sample.currentUa = now.currentUa;
                sample.chargeCounterUah = now.chargeCounterUah;
                sample.voltageMv = now.voltageMv;
                sample.levelPct = now.levelPct;
                sample.tempTenths = now.tempTenths;
                sample.screenOn = now.screenOn;
                sample.airplaneMode = now.airplaneMode;
                sample.plugged = now.plugged;
                db.insertSample(sample);
                samples.add(sample);

                if (now.tempTenths > trial.maxTempTenths) trial.maxTempTenths = now.tempTenths;

                RunnerStatus.trialElapsedMs = elapsedMs;
                RunnerStatus.currentMa = now.currentUa == null ? null : Math.abs(now.currentUa) / 1000.0;
                RunnerStatus.tempC = now.tempC();

                // The flags start out true, so they double as "not logged yet" markers.
                if (!now.airplaneMode && trial.airplaneStayedOn) {
                    trial.airplaneStayedOn = false;
                    RunnerStatus.violations++;
                    log(session, trial.id, clip.label() + ": airplane mode was turned off during the trial.");
                }
                if (now.screenOn && trial.screenStayedOff) {
                    trial.screenStayedOff = false;
                    RunnerStatus.violations++;
                    log(session, trial.id, clip.label() + ": the screen was on during the trial.");
                }

                // Nothing can be measured while the battery is charging, so this trial is a write-off.
                if (now.plugged) {
                    trial.status = Trial.FAILED;
                    trial.endedAt = System.currentTimeMillis();
                    trial.note = "The charger was plugged in during the trial";
                    trial.sampleCount = samples.size();
                    db.updateTrial(trial);
                    log(session, trial.id, clip.label() + ": charger plugged in, trial thrown away.");
                    queueRerun(session, trial, "charger plugged in");
                    return;
                }
            }

            BatterySnapshot end = battery.snapshot();
            Metrics m = computeMetrics(start, samples, end);
            trial.status = Trial.COMPLETED;
            trial.endedAt = System.currentTimeMillis();
            trial.endChargeUah = end.chargeCounterUah;
            trial.endTempTenths = end.tempTenths;
            trial.deltaChargeMah = m.deltaChargeMah;
            trial.energyJoules = m.energyJoules;
            trial.meanCurrentMa = m.meanCurrentMa;
            trial.sampleCount = samples.size();
            db.updateTrial(trial);
            log(session, trial.id, clip.label() + " finished: " + m.deltaChargeMah + " mAh, " + m.energyJoules + " J.");
        } finally {
            player.stop();
            player.release();
        }
    }

    // Rest between trials so the next one doesn't start on a battery warmed up by the last one.
    // Waits at least session.cooldownMs, then keeps waiting until the temperature is back within
    // Config.TEMP_TOLERANCE_TENTHS of the session's starting temperature - but never longer than
    // 3 x cooldownMs in total, otherwise a warm room would stall the session forever.
    private void coolDown(Session session) throws InterruptedException {
        long startTime = SystemClock.elapsedRealtime();
        long limitMs = session.cooldownMs * 3;
        Integer target = session.startTempTenths == null
                ? null
                : session.startTempTenths + Config.TEMP_TOLERANCE_TENTHS;

        while (true) {
            long waited = SystemClock.elapsedRealtime() - startTime;
            BatterySnapshot now = battery.snapshot();
            RunnerStatus.tempC = now.tempC();

            boolean coolEnough = target == null || now.tempTenths <= target;
            if (waited >= session.cooldownMs && coolEnough) return;

            if (waited >= limitMs) {
                log(session, null, String.format(Locale.US,
                        "Still %.1f °C after %d s of cool-down (wanted %.1f °C), carrying on anyway.",
                        now.tempC(), limitMs / 1000, target == null ? now.tempC() : target / 10.0));
                return;
            }

            if (waited < session.cooldownMs) {
                RunnerStatus.message = String.format(Locale.US, "Cooling down, %d s left (%.1f °C)",
                        (session.cooldownMs - waited) / 1000, now.tempC());
            } else {
                RunnerStatus.message = String.format(Locale.US,
                        "Waiting for the battery to cool to %.1f °C (now %.1f °C)", target / 10.0, now.tempC());
            }

            long nextStop = waited < session.cooldownMs ? session.cooldownMs : limitMs;
            Thread.sleep(Math.min(CHECK_EVERY_MS, Math.max(1, nextStop - waited)));
        }
    }

    // A trial is only worth running with airplane mode on and the charger out, so wait for the user
    // to sort that out first. The problem is logged once, not on every check.
    private void waitForConditions(Session session) throws InterruptedException {
        boolean logged = false;
        while (true) {
            BatterySnapshot now = battery.snapshot();
            List<String> todo = new ArrayList<>();
            if (!now.airplaneMode) todo.add("turn on airplane mode");
            if (now.plugged) todo.add("unplug the charger");
            if (todo.isEmpty()) return;

            String what = String.join(" and ", todo);
            if (!logged) {
                log(session, null, "Waiting for the user to " + what + " before the next trial.");
                logged = true;
            }
            RunnerStatus.message = "Please " + what + " to carry on.";
            Thread.sleep(CHECK_EVERY_MS);
        }
    }

    public static class Metrics {
        public Double deltaChargeMah;
        public Double energyJoules;
        public Double meanCurrentMa;
    }

    // One current/voltage reading at a point in time: either the start snapshot or a sample.
    private static class Reading {
        final long timeMs;
        final Long currentUa;
        final int voltageMv;

        Reading(long timeMs, Long currentUa, int voltageMv) {
            this.timeMs = timeMs;
            this.currentUa = currentUa;
            this.voltageMv = voltageMv;
        }
    }

    // The trial's results. Some phones report the current as negative while discharging, so only
    // the size of it is used. Anything the phone doesn't report stays null instead of becoming 0.
    public static Metrics computeMetrics(BatterySnapshot start, List<BatterySample> samples, BatterySnapshot end) {
        Metrics m = new Metrics();

        if (start.chargeCounterUah != null && end.chargeCounterUah != null) {
            m.deltaChargeMah = (start.chargeCounterUah - end.chargeCounterUah) / 1000.0;
        }

        List<Reading> readings = new ArrayList<>();
        readings.add(new Reading(0, start.currentUa, start.voltageMv));
        if (samples != null) {
            for (BatterySample s : samples) {
                readings.add(new Reading(s.elapsedMs, s.currentUa, s.voltageMv));
            }
        }

        double currentSum = 0;
        int currentCount = 0;
        for (Reading r : readings) {
            if (r.currentUa == null) continue;
            currentSum += Math.abs(r.currentUa) / 1000.0;
            currentCount++;
        }
        if (currentCount > 0) m.meanCurrentMa = currentSum / currentCount;

        // Energy = the area under the power curve, by the trapezoid rule over neighbouring
        // readings. One microamp x one millivolt x one second is 1e-9 of a joule.
        double joules = 0;
        boolean anyPair = false;
        for (int i = 1; i < readings.size(); i++) {
            Reading before = readings.get(i - 1);
            Reading after = readings.get(i);
            if (before.currentUa == null || after.currentUa == null) continue;
            double powerBefore = Math.abs(before.currentUa) * (double) before.voltageMv;
            double powerAfter = Math.abs(after.currentUa) * (double) after.voltageMv;
            double seconds = (after.timeMs - before.timeMs) / 1000.0;
            joules += (powerBefore + powerAfter) / 2 * seconds * 1e-9;
            anyPair = true;
        }
        if (anyPair) m.energyJoules = joules;

        return m;
    }

    // After every trial, check this clip's finished trials against each other (proposal: anything
    // more than Config.OUTLIER_SD SD out gets flagged and repeated, never deleted).
    private void checkOutliers(Session session, String clipFile) {
        List<Trial> done = new ArrayList<>();
        for (Trial t : db.getTrials(session.id)) {
            if (t.clipFile.equals(clipFile) && t.status.equals(Trial.COMPLETED)) done.add(t);
        }
        if (done.size() < 3) return;

        // Charge used is the main measure; phones without a charge counter fall back to energy.
        List<Double> values = valuesOf(done, true);
        if (values == null) values = valuesOf(done, false);
        if (values == null) return; // this phone reports neither, so there is nothing to compare

        Clip clip = Clip.find(clipFile);
        String label = clip == null ? clipFile : clip.label();

        for (int i : OutlierEngine.findOutliers(values, Config.OUTLIER_SD)) {
            Trial outlier = done.get(i);
            outlier.status = Trial.OUTLIER;
            outlier.note = "More than " + Config.OUTLIER_SD + " SD from the other " + label + " trials";
            db.updateTrial(outlier);
            log(session, outlier.id, label + " rep " + (outlier.repetition + 1) + " is an outlier ("
                    + values.get(i) + " against the other " + (values.size() - 1)
                    + " trials), so it is left out of the statistics.");
            queueRerun(session, outlier, "outlier");
        }
    }

    // The deltaChargeMah (or energyJoules) of every trial, or null if any one of them is missing it.
    private List<Double> valuesOf(List<Trial> trials, boolean charge) {
        List<Double> values = new ArrayList<>();
        for (Trial t : trials) {
            Double value = charge ? t.deltaChargeMah : t.energyJoules;
            if (value == null) return null;
            values.add(value);
        }
        return values;
    }

    // Adds a repeat of `original` at the end of the session, up to Config.MAX_RERUNS_PER_CLIP per clip.
    private void queueRerun(Session session, Trial original, String reason) {
        int reruns = 0;
        int lastOrderIndex = 0;
        for (Trial t : db.getTrials(session.id)) {
            if (t.isRerun && t.clipFile.equals(original.clipFile)) reruns++;
            if (t.orderIndex > lastOrderIndex) lastOrderIndex = t.orderIndex;
        }

        Clip clip = Clip.find(original.clipFile);
        String label = clip == null ? original.clipFile : clip.label();

        if (reruns >= Config.MAX_RERUNS_PER_CLIP) {
            log(session, original.id, label + " already has " + reruns + " re-runs ("
                    + Config.MAX_RERUNS_PER_CLIP + " allowed), so no re-run was added for: " + reason + ".");
            return;
        }

        Trial rerun = new Trial();
        rerun.sessionId = session.id;
        rerun.clipFile = original.clipFile;
        rerun.repetition = original.repetition;
        rerun.isRerun = true;
        rerun.orderIndex = lastOrderIndex + 1;
        db.insertTrial(rerun);
        log(session, original.id, "Re-run of " + label + " rep " + (original.repetition + 1)
                + " added at the end of the session (" + reason + ").");
    }

    private void markAborted(Session session) {
        for (Trial t : db.getTrials(session.id)) {
            if (t.status.equals(Trial.RUNNING)) {
                t.status = Trial.ABORTED;
                db.updateTrial(t);
            }
        }
        session.status = Session.ABORTED;
        session.endedAt = System.currentTimeMillis();
        db.updateSession(session);
        log(session, null, "Session aborted by the user.");
        RunnerStatus.finish("Session aborted");
    }

    private MediaPlayer startPlaying(Clip clip) throws IOException {
        MediaPlayer player = new MediaPlayer();
        AssetFileDescriptor file = context.getAssets().openFd("audio/" + clip.file);
        player.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
        file.close();
        player.setLooping(true);
        player.prepare();
        player.start();
        return player;
    }

    private void setVolume(int percent) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, max * percent / 100, 0);
    }

    private Trial findNextPending(List<Trial> trials) {
        for (Trial t : trials) {
            if (t.status.equals(Trial.PENDING)) return t; // list is already sorted by orderIndex
        }
        return null;
    }

    private int countFinished(List<Trial> trials) {
        int count = 0;
        for (Trial t : trials) {
            if (!t.status.equals(Trial.PENDING) && !t.status.equals(Trial.RUNNING)) count++;
        }
        return count;
    }

    private void log(Session session, Long trialId, String message) {
        LogEntry.add(db, session.id, trialId, message);
    }
}
