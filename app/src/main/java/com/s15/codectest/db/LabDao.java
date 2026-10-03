package com.s15.codectest.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

// Every database query the app uses. Never call these on the main (UI) thread —
// use new Thread(() -> { ... }).start() and runOnUiThread(...) for the result.
@Dao
public interface LabDao {
    @Insert
    long insertSession(Session session);

    @Update
    void updateSession(Session session);

    @Query("SELECT * FROM Session WHERE id = :id")
    Session getSession(long id);

    @Query("SELECT * FROM Session ORDER BY id DESC")
    List<Session> getSessions();

    @Insert
    long insertTrial(Trial trial);

    @Update
    void updateTrial(Trial trial);

    @Query("SELECT * FROM Trial WHERE sessionId = :sessionId ORDER BY orderIndex")
    List<Trial> getTrials(long sessionId);

    @Insert
    void insertSample(BatterySample sample);

    @Query("SELECT * FROM BatterySample WHERE trialId IN (SELECT id FROM Trial WHERE sessionId = :sessionId) ORDER BY trialId, elapsedMs")
    List<BatterySample> getSamples(long sessionId);

    @Insert
    void insertLog(LogEntry entry);

    @Query("SELECT * FROM LogEntry WHERE sessionId = :sessionId ORDER BY time")
    List<LogEntry> getLog(long sessionId);
}
