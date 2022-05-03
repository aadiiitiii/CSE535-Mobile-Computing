package com.example.covid_symptoms_tracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;

import static java.lang.Math.abs;

/**
 * Main Screen with buttons for recording video, calculating breathing and heart rates, uploading signs and uploading symptoms
 */
public class HomeScreen extends AppCompatActivity {

    private static final int VIDEO_CAPTURE = 101;
    private Uri fileUri;
    private int window_size = 9;
    long startExececutionTime;
    private TextView heartRateTextView;
    private TextView breathingRateTextView;

    private boolean uploadSignsClicked = false;
    private boolean ongoingHeartRateProcess = false;
    private boolean ongoingBreathingRateProcess = false;

    private CovidAppDatabase db;

    private String rootPath = Environment.getExternalStorageDirectory().getPath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signs_screen);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Button recordButton = (Button) findViewById(R.id.record_video_button);
        Button measureHeartRateButton = (Button) findViewById(R.id.measure_heart_rate_button);
        Button measureBreathingButton = (Button) findViewById(R.id.measure_breathing_button);
        Button uploadSymptomsButton = (Button) findViewById(R.id.upload_symptoms_button);
        Button uploadSignsButton = (Button) findViewById(R.id.upload_signs_button);

        heartRateTextView = (TextView) findViewById(R.id.heart_rate_display);
        breathingRateTextView = (TextView) findViewById(R.id.breathing_rate_display);

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    db = CovidAppDatabase.getInstance(getApplicationContext());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

        if (!hasCamera()) recordButton.setEnabled(false);

        handlePermissions(HomeScreen.this);

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ongoingHeartRateProcess != true) {
                    startRecording();
                } else {
                    Toast.makeText(HomeScreen.this, "Please wait for process to complete before recording a new video!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        measureHeartRateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                File videoFile = new File(rootPath + "/heart_rate.mp4");
                fileUri = Uri.fromFile(videoFile);

                if (ongoingHeartRateProcess != true) {
                    if (!videoFile.exists()) {
                        Toast.makeText(HomeScreen.this, "Please record a video first!", Toast.LENGTH_SHORT).show();
                    } else {
                        ongoingHeartRateProcess = true;
                        heartRateTextView.setText("Calculating...");

                        startExececutionTime = System.currentTimeMillis();
                        System.gc();
                        Intent heartIntent = new Intent(HomeScreen.this, HeartRateService.class);
                        startService(heartIntent);
                    }
                } else {
                    Toast.makeText(HomeScreen.this, "Please wait for process to complete before starting a new one!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        measureBreathingButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (ongoingBreathingRateProcess != true) {
                    Toast.makeText(HomeScreen.this, "Place the phone on your chest \nfor 45s", Toast.LENGTH_LONG).show();
                    ongoingBreathingRateProcess = true;
                    breathingRateTextView.setText("Sensing...");
                    Intent accelIntent = new Intent(HomeScreen.this, BreathingService.class);
                    startService(accelIntent);
                } else {
                    Toast.makeText(HomeScreen.this, "Please wait for process to complete before starting a new one!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        uploadSymptomsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeScreen.this, SymptomsScreen.class);
                intent.putExtra("uploadSignsClicked", uploadSignsClicked);
                startActivity(intent);
            }
        });

        uploadSignsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                uploadSignsClicked = true;
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        UserDetails data = new UserDetails();
                        data.heartRate = Float.parseFloat(heartRateTextView.getText().toString());
                        data.breathingRate = Float.parseFloat(breathingRateTextView.getText().toString());
                        data.timestamp = new Date(System.currentTimeMillis());
                        db.userInfoDao().insert(data);
                    }
                });
                thread.start();
                Toast.makeText(HomeScreen.this, "Signs uploaded!", Toast.LENGTH_SHORT).show();
            }

        });

        LocalBroadcastManager.getInstance(HomeScreen.this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Bundle b = intent.getExtras();
                BreathingRateDetector runnable = new BreathingRateDetector(b.getIntegerArrayList("accelValuesX"));

                Thread thread = new Thread(runnable);
                thread.start();

                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                breathingRateTextView.setText(runnable.breathingRate + "");

                Toast.makeText(HomeScreen.this, "Respiratory rate calculated!", Toast.LENGTH_SHORT).show();
                ongoingBreathingRateProcess = false;
                b.clear();
                System.gc();

            }
        }, new IntentFilter("broadcastingAccelData"));


        LocalBroadcastManager.getInstance(HomeScreen.this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Bundle b = intent.getExtras();
                int heartRate = 0;
                int fail = 0;
                int i = 0;
                heartRate = getHeartRate(b, heartRate, i);

                heartRate = (heartRate * 12) / window_size;
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.i("log", "Final heart rate: " + heartRate);
                heartRateTextView.setText(heartRate + "");
                ongoingHeartRateProcess = false;
                System.gc();
                b.clear();
            }

            private int getHeartRate(Bundle b, int heartRate, int i) {
                while (i < window_size) {

                    ArrayList<Integer> heartData = null;
                    heartData = b.getIntegerArrayList("heartData" + i);

                    ArrayList<Integer> denoisedRedness = denoise(heartData, 5);

                    float zeroCrossings = peakFinding(denoisedRedness);
                    heartRate += zeroCrossings / 2;
                    Log.i("log", "heart rate for " + i + ": " + zeroCrossings / 2);

                    String csvFilePath = rootPath + "/heart_rate" + i + ".csv";
                    saveToCSV(heartData, csvFilePath);

                    csvFilePath = rootPath + "/heart_rate_denoised" + i + ".csv";
                    saveToCSV(denoisedRedness, csvFilePath);

                    i++;
                }
                return heartRate;
            }
        }, new IntentFilter("broadcastingHeartData"));

    }

    @Override
    protected void onStart() {
        super.onStart();
        uploadSignsClicked = false;
    }

    public class BreathingRateDetector implements Runnable {

        public float breathingRate;
        ArrayList<Integer> accelValuesX;

        BreathingRateDetector(ArrayList<Integer> accelValuesX) {
            this.accelValuesX = accelValuesX;
        }

        @Override
        public void run() {

            String csvFilePath = rootPath + "/x_values.csv";
            saveToCSV(accelValuesX, csvFilePath);

            ArrayList<Integer> accelValuesXDenoised = denoise(accelValuesX, 10);

            csvFilePath = rootPath + "/x_values_denoised.csv";
            saveToCSV(accelValuesXDenoised, csvFilePath);

            int zeroCrossings = peakFinding(accelValuesXDenoised);
            breathingRate = (zeroCrossings * 60) / 90;
            Log.i("log", "Respiratory rate" + breathingRate);
        }
    }

    public ArrayList<Integer> denoise(ArrayList<Integer> data, int filter) {
        ArrayList<Integer> movingAvgArr = new ArrayList<>();
        int movingAvg = 0;

        for (int i = 0; i < data.size(); i++) {
            movingAvg += data.get(i);
            if (i + 1 < filter) {
                continue;
            }
            movingAvgArr.add((movingAvg) / filter);
            movingAvg -= data.get(i + 1 - filter);
        }
        return movingAvgArr;
    }

    public int peakFinding(ArrayList<Integer> data) {
        int diff, prev, slope = 0, zeroCrossings = 0;
        int j = 0;
        prev = data.get(0);

        while (slope == 0 && j + 1 < data.size()) {
            diff = data.get(j + 1) - data.get(j);
            if (diff != 0) {
                slope = diff / abs(diff);
            }
            j++;
        }

        for (int i = 1; i < data.size(); i++) {

            diff = data.get(i) - prev;
            prev = data.get(i);

            if (diff == 0) continue;

            int currSlope = diff / abs(diff);

            if (currSlope == -1 * slope) {
                slope *= -1;
                zeroCrossings++;
            }
        }

        return zeroCrossings;
    }

    public void saveToCSV(ArrayList<Integer> data, String path) {

        File file = new File(path);

        try {
            FileWriter outputFile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputFile);
            String[] header = {"Index", "Data"};
            writer.writeNext(header);
            int i = 0;
            for (int d : data) {
                String dataRow[] = {i + "", d + ""};
                writer.writeNext(dataRow);
                i++;
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handlePermissions(Activity activity) {

        int storagePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int REQUEST_EXTERNAL_STORAGE = 1;

        String[] PERMISSIONS = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        };

        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            Log.i("log", "Read/Write Permissions needed!");
        }

        ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS,
                REQUEST_EXTERNAL_STORAGE
        );
        Log.i("log", "Permissions Granted!");
    }

    private boolean hasCamera() {
        if (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_ANY)) {
            return true;
        } else {
            return false;
        }
    }

    public void startRecording() {

        File mediaFile = new File(rootPath + "/heart_rate.mp4");
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 45);

        fileUri = Uri.fromFile(mediaFile);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        startActivityForResult(intent, VIDEO_CAPTURE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        boolean deleteFile = false;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_CAPTURE) {
            if (resultCode == RESULT_OK) {

                MediaMetadataRetriever videoRetriever = new MediaMetadataRetriever();
                FileInputStream input = null;
                try {
                    input = new FileInputStream(fileUri.getPath());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                try {
                    videoRetriever.setDataSource(input.getFD());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String timeString = videoRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long time = Long.parseLong(timeString) / 1000;

                if (time < 45) {
                    Toast.makeText(this, "Please record for at least 45 seconds! ", Toast.LENGTH_SHORT).show();
                    deleteFile = true;
                } else {
                    Toast.makeText(this, "Video has been saved to:\n" + data.getData(), Toast.LENGTH_SHORT).show();
                }

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Video recording cancelled.", Toast.LENGTH_SHORT).show();
                deleteFile = true;
            } else {
                Toast.makeText(this, "Failed to record video", Toast.LENGTH_SHORT).show();
            }

            if (deleteFile) {
                File fdelete = new File(fileUri.getPath());

                if (fdelete.exists()) {
                    if (fdelete.delete()) {
                        System.out.println("Recording deleted");
                    }
                }
            }
            fileUri = null;
        }
    }
}
