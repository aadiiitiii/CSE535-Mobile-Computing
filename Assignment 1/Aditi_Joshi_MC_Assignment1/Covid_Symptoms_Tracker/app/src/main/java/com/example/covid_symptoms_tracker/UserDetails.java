package com.example.covid_symptoms_tracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity
public class UserDetails {
    @PrimaryKey (autoGenerate = true)
    public int id;
    public Date timestamp;
    public float heartRate;
    public float breathingRate;
    public float fever = 0;
    public float cough = 0;
    public float tiredness = 0;
    public float shortnessOfBreath = 0;
    public float muscleAches = 0;
    public float chills = 0;
    public float soreThroat = 0;
    public float runnyNose = 0;
    public float headache = 0;
    public float chestPain = 0;

    public UserDetails() {
    }
}
