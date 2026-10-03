package com.s15.codectest.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Usage: LabDao db = LabDatabase.get(context);
// If you change a table, increase the version number (this wipes the old test data).
@Database(entities = {Session.class, Trial.class, BatterySample.class, LogEntry.class}, version = 1, exportSchema = false)
public abstract class LabDatabase extends RoomDatabase {
    public abstract LabDao dao();

    private static LabDatabase instance;

    public static synchronized LabDao get(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), LabDatabase.class, "s15_codec_test.db")
                    .fallbackToDestructiveMigration(true)
                    .build();
        }
        return instance.dao();
    }
}
