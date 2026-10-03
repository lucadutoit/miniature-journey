package com.s15.codectest.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// The audit log: everything that happens in a session (starts, failures, outliers, re-runs)
// is written here so we can report it honestly.
@Entity
public class LogEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long sessionId;
    public Long trialId;
    public long time;
    public String message;

    public static void add(LabDao db, long sessionId, Long trialId, String message) {
        LogEntry e = new LogEntry();
        e.sessionId = sessionId;
        e.trialId = trialId;
        e.time = System.currentTimeMillis();
        e.message = message;
        db.insertLog(e);
    }
}
