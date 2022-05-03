package com.example.smarthomegesturecontrol;

import static android.widget.Toast.*;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    public static final String GESTURE_NAME;

    static {
        GESTURE_NAME = "gesture_name";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinner;
        spinner = (Spinner) findViewById(R.id.gestures_spinner);

        ArrayAdapter<CharSequence> adapter;
        adapter = ArrayAdapter.createFromResource(this,R.array.gesture_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (i == 0) return;

        Intent Screen2_Display_Video;
        Screen2_Display_Video = new Intent(MainActivity.this, Screen2_Display_Video.class);
        Screen2_Display_Video.putExtra(GESTURE_NAME, adapterView.getItemAtPosition(i).toString());

        startActivity(Screen2_Display_Video);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        Context context;
        context = getApplicationContext();

        CharSequence text;
        text = "Choose an option!";

        Toast toast;
        toast = makeText(context, text, LENGTH_SHORT);
        toast.show();
    }
}