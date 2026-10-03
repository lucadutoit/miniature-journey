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

// Runs a whole session: plays every trial in order, records the battery, and saves the results.
// Runs on MeasurementService's background thread, so waiting with Thread.sleep() is fine.
// Aborting = the thread gets interrupted, so let InterruptedException pass up (don't catch it).
//
// Owner: runner (Luca). Starting/stopping, playback and saving are done. The TODO(runner) parts are left.
public class SessionRunner {

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

            // TODO(runner): replace this sleep with the sampling loop:
            //  - until session.trialDurationMs has passed (SystemClock.elapsedRealtime() - startTime):
            //    sleep Config.SAMPLE_EVERY_SECONDS, take battery.snapshot(), make a BatterySample
            //    (trialId, elapsedMs, current, charge, voltage, level, temp, screen, airplane, plugged),
            //    save it with db.insertSample() and add it to `samples`.
            //  - if airplane mode turns off or the screen turns on: set trial.airplaneStayedOn /
            //    trial.screenStayedOff to false, add 1 to RunnerStatus.violations and log it (once each).
            //  - if the charger gets plugged in: stop the loop, mark the trial FAILED, log it,
            //    call queueRerun(...) and return (the readings are useless while charging).
            //  - after every sample update RunnerStatus.trialElapsedMs, currentMa (|currentUa| / 1000) and tempC.
            //  - set trial.maxTempTenths to the highest sample temperature.
            Thread.sleep(session.trialDurationMs);

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

    // TODO(runner): cool down between trials.
    //  Wait at least session.cooldownMs. After that keep waiting until the battery temperature is
    //  back within Config.TEMP_TOLERANCE_TENTHS of session.startTempTenths, but never longer than
    //  3 x cooldownMs in total (if it's still too warm then, log that and carry on).
    //  Check every 5 seconds and update RunnerStatus.message / tempC so the screen shows it.
    private void coolDown(Session session) throws InterruptedException {
    }

    // TODO(runner): wait until airplane mode is ON and the charger is unplugged before a trial.
    //  Check every 5 seconds. Log the problem once (not every check) and put a message in
    //  RunnerStatus.message telling the user what to fix.
    private void waitForConditions(Session session) throws InterruptedException {
    }

    public static class Metrics {
        public Double deltaChargeMah;
        public Double energyJoules;
        public Double meanCurrentMa;
    }

    // TODO(runner): work out the trial's results (MetricsTest checks this).
    //  - deltaChargeMah = (start.chargeCounterUah - end.chargeCounterUah) / 1000.0, or null if either is null.
    //  - Readings = the start snapshot (time 0) + every sample (time = elapsedMs). Use the absolute
    //    value of the current because some phones report it as negative.
    //  - meanCurrentMa = average |current| / 1000 over readings that have a current (null if none).
    //  - energyJoules = trapezoid rule over neighbouring readings that both have a current:
    //    add (|I1| * V1 + |I2| * V2) / 2 * seconds between them * 1e-9   (null if nothing was added).
    public static Metrics computeMetrics(BatterySnapshot start, List<BatterySample> samples, BatterySnapshot end) {
        return new Metrics();
    }

    // TODO(runner): outlier check after each trial (proposal: > 3 SD gets flagged and repeated).
    //  Take this clip's COMPLETED trials in the session (need at least 3). Use their deltaChargeMah
    //  values (or energyJoules if the phone has no charge counter). Call OutlierEngine.findOutliers(values,
    //  Config.OUTLIER_SD). For each outlier: status = Trial.OUTLIER, write a note, log it, queueRerun(...).
    private void checkOutliers(Session session, String clipFile) {
    }

    // TODO(runner): add a repeat of `original` at the end of the session.
    //  If this clip already has Config.MAX_RERUNS_PER_CLIP re-runs, just log that and stop.
    //  Otherwise insert a new PENDING Trial (same clipFile and repetition, isRerun = true,
    //  orderIndex = highest orderIndex in the session + 1) and log why.
    private void queueRerun(Session session, Trial original, String reason) {
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
