package com.s15.codectest.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// One playback of one clip inside a session.
@Entity
public class Trial {
    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String COMPLETED = "COMPLETED";
    public static final String OUTLIER = "OUTLIER"; // kept in the data, but left out of the statistics
    public static final String FAILED = "FAILED";
    public static final String ABORTED = "ABORTED";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public long sessionId;
    public String clipFile;
    public int orderIndex;
    public int repetition;
    public boolean isRerun;
    public String status = PENDING;
    public Long startedAt;
    public Long endedAt;

    public Long startChargeUah;
    public Long endChargeUah;
    public Double deltaChargeMah; // main result: battery used, in mAh
    public Double energyJoules;   // second result: energy used, in J
    public Double meanCurrentMa;
    public Integer startTempTenths;
    public Integer maxTempTenths;
    public Integer endTempTenths;
    public int sampleCount;

    public boolean airplaneStayedOn = true;
    public boolean screenStayedOff = true;
    public String note;
}
