package com.example.covid_symptoms_tracker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDetailsDao {
    @Query("SELECT COUNT(*) FROM UserDetails")
    int count();

    //Get latest data row
    @Query("SELECT * FROM UserDetails where timestamp=(SELECT MAX(timestamp) FROM UserDetails)")
    UserDetails getLatestData();

    @Insert
    void insert(UserDetails userDetails);

    @Update
    void update(UserDetails userDetails);
}
