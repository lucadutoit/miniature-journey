package com.s15.codectest;

import android.content.Context;
import android.os.Build;

import com.s15.codectest.db.LabDao;
import com.s15.codectest.db.LabDatabase;
import com.s15.codectest.db.LogEntry;
import com.s15.codectest.db.Session;
import com.s15.codectest.db.Trial;
import com.s15.codectest.measure.TrialScheduler;

import java.util.List;

public class SessionMaker {

    // Creates a session and all its trials in the database. Returns the new session's id.
    // Uses the database, so call it from a background thread.
    public static long create(Context context, String name, List<String> clipFiles, boolean pilot) {
        LabDao db = LabDatabase.get(context);

        Session session = new Session();
        session.name = name;
        session.pilot = pilot;
        session.createdAt = System.currentTimeMillis();
        session.device = Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")";
        if (pilot) {
            session.trialDurationMs = Config.PILOT_TRIAL_MINUTES * 60_000L;
            session.repetitions = Config.PILOT_REPETITIONS;
            session.cooldownMs = Config.PILOT_COOLDOWN_SECONDS * 1000L;
        } else {
            session.trialDurationMs = Config.TRIAL_MINUTES * 60_000L;
            session.repetitions = Config.REPETITIONS;
            session.cooldownMs = Config.COOLDOWN_MINUTES * 60_000L;
        }
        session.id = db.insertSession(session);

        List<String> order = TrialScheduler.buildOrder(clipFiles, session.repetitions);
        for (int i = 0; i < order.size(); i++) {
            Trial trial = new Trial();
            trial.sessionId = session.id;
            trial.clipFile = order.get(i);
            trial.orderIndex = i;
            trial.repetition = i / clipFiles.size();
            db.insertTrial(trial);
        }

        LogEntry.add(db, session.id, null, (pilot ? "Pilot session" : "Session") + " created with "
                + clipFiles.size() + " clips x " + session.repetitions + " repetitions = " + order.size() + " trials.");
        return session.id;
    }
}
