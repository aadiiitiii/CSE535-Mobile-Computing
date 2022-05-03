package com.example.covid_symptoms_tracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

public class BreathingService extends Service implements SensorEventListener {

    private SensorManager SensorAccelerometerManager;
    private Sensor SensorAccelerometer;
    private ArrayList<Integer> AccelerometerValueX = new ArrayList<>();
    private ArrayList<Integer> AccelerometerValueY = new ArrayList<>();
    private ArrayList<Integer> AccelerometerValueZ = new ArrayList<>();

    @Override
    public void onCreate() {
        Log.i("log", "Accelerometer Service has been started");
        SensorAccelerometerManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        SensorAccelerometer = SensorAccelerometerManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        SensorAccelerometerManager.registerListener(this, SensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AccelerometerValueX.clear();
        AccelerometerValueY.clear();
        AccelerometerValueZ.clear();
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        Sensor genericSensor = sensorEvent.sensor;
        if (genericSensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            AccelerometerValueX.add((int)(sensorEvent.values[0] * 100));
            AccelerometerValueY.add((int)(sensorEvent.values[1] * 100));
            AccelerometerValueZ.add((int)(sensorEvent.values[2] * 100));

            if(AccelerometerValueX.size() >= 230){
                stopSelf();
            }
        }
    }

    @Override
    public void onDestroy() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                SensorAccelerometerManager.unregisterListener(BreathingService.this);
                Log.i("service", "Service stopping");
                Intent intent = new Intent("broadcastingAccelData");
                Bundle b = new Bundle();
                b.putIntegerArrayList("accelValuesX", AccelerometerValueX);
                intent.putExtras(b);
                LocalBroadcastManager.getInstance(BreathingService.this).sendBroadcast(intent);
            }
        });
        thread.start();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
