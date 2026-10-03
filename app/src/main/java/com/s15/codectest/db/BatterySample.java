package com.s15.codectest.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// One battery reading taken during a trial.
@Entity
public class BatterySample {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long trialId;
    public long elapsedMs;        // time since the trial started
    public Long currentUa;        // microamps, null if the phone doesn't report it
    public Long chargeCounterUah; // microamp-hours left, null if the phone doesn't report it
    public int voltageMv;
    public int levelPct;
    public int tempTenths;
    public boolean screenOn;
    public boolean airplaneMode;
    public boolean plugged;
}
