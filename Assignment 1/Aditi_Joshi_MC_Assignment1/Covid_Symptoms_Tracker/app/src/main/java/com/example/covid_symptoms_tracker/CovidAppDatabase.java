package com.example.covid_symptoms_tracker;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {UserDetails.class}, version = 1)
@TypeConverters(TimeConversion.class)
public abstract class CovidAppDatabase extends RoomDatabase {
    public abstract UserDetailsDao userInfoDao();
    private static CovidAppDatabase dbInstance;
    public static synchronized CovidAppDatabase getInstance(Context context) {
        CovidAppDatabase result;

        if (dbInstance != null) {
            result = dbInstance;
        } else {
            dbInstance = Room.databaseBuilder(context.getApplicationContext(), CovidAppDatabase.class, "joshi").build();
            result = dbInstance;
        }
        return result;
    }
}
