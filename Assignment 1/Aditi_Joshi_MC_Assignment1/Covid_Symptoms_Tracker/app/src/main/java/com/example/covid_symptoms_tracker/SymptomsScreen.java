package com.example.covid_symptoms_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Date;

public class SymptomsScreen extends AppCompatActivity {

    private Spinner spinner;
    RatingBar symptomRatingBar;
    private CovidAppDatabase db;
    private final UserDetails data = new UserDetails();
    float[] ratings_cache = new float[10];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptoms_screen);

        symptomRatingBar = findViewById(R.id.rating_spinner);
        Button updateButton = findViewById(R.id.save_symptoms_button);
        Button homeButton = findViewById(R.id.home_button);

        spinner = findViewById(R.id.symptoms_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.symptoms_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    db = CovidAppDatabase.getInstance(getApplicationContext());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

        symptomRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                int i;
                i = spinner.getSelectedItemPosition();
                ratings_cache[i] = v;
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                data.fever = ratings_cache[0];
                data.cough = ratings_cache[1];
                data.tiredness = ratings_cache[2];
                data.shortnessOfBreath = ratings_cache[3];
                data.muscleAches = ratings_cache[4];
                data.chills = ratings_cache[5];
                data.soreThroat = ratings_cache[6];
                data.runnyNose = ratings_cache[7];
                data.headache = ratings_cache[8];
                data.chestPain = ratings_cache[9];
                data.timestamp = new Date(System.currentTimeMillis());

                boolean uploadSignsClicked;
                uploadSignsClicked = getIntent().getExtras().getBoolean("uploadSignsClicked");

                Thread thread;
                if (!uploadSignsClicked) {
                    thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            db.userInfoDao().insert(data);
                        }
                    });
                } else {
                    thread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            UserDetails latestData = db.userInfoDao().getLatestData();
                            data.heartRate = latestData.heartRate;
                            data.breathingRate = latestData.breathingRate;
                            data.id = latestData.id;
                            db.userInfoDao().update(data);
                        }
                    });

                }
                thread.start();
                Toast.makeText(SymptomsScreen.this, "Symptoms have been updated!", Toast.LENGTH_SHORT).show();
            }
        });

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SymptomsScreen.this, HomeScreen.class);
                startActivity(intent);
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                symptomRatingBar.setRating(ratings_cache[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

}