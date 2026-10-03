package com.s15.codectest.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Session {
    public static final String PLANNED = "PLANNED";
    public static final String RUNNING = "RUNNING";
    public static final String COMPLETED = "COMPLETED";
    public static final String ABORTED = "ABORTED";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String status = PLANNED;
    public boolean pilot;
    public long createdAt;
    public Long startedAt;
    public Long endedAt;
    public long trialDurationMs;
    public long cooldownMs;
    public int repetitions;
    public String device;
    public Integer startTempTenths; // battery temperature at the start, in tenths of a °C
}
