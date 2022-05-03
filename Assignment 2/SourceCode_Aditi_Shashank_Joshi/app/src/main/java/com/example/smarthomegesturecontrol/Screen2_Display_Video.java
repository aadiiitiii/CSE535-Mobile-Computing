package com.example.smarthomegesturecontrol;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class Screen2_Display_Video extends AppCompatActivity {

    String gestureName = "";
    Button practiceButton;
    String receivedGestureName;
    MediaController mController;

    public static final String GESTURE_NAME = "gesture_name";
    public static Integer practice_number = 0;
    public static final String PRACTICE_NUMBER = "practice_no";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen2_display_video);

        Intent curr = getIntent();
        receivedGestureName = curr.getStringExtra(MainActivity.GESTURE_NAME);
        mController = new MediaController(this);
        gestureName = receivedGestureName;

        TextView textView = (TextView) findViewById(R.id.gesture_display);
        textView.setText("Gesture selected is: "+receivedGestureName);

        VideoView videoView = findViewById(R.id.gesture_video);
        String videoPath = "android.resource://" + getPackageName() + "/raw/" + extractGesture(receivedGestureName);
        videoView.setVideoURI(Uri.parse(videoPath));
        videoView.setMediaController(mController);
        videoView.start();

        practiceButton = (Button) findViewById(R.id.practice_button);
        practiceButton.setOnClickListener(view -> {
            practice_number=(practice_number+1)%3;
            Intent intent = new Intent(Screen2_Display_Video.this,Screen3_Record_Video.class);
            intent.putExtra(GESTURE_NAME, gestureName);
            intent.putExtra(PRACTICE_NUMBER, practice_number);
            startActivity(intent);
        });
    }

    private String extractGesture(String receivedGestureName) {
        switch (receivedGestureName) {

            case "LightOn":
                return "light_on";

            case "LightOff":
                return "light_off";
                
            case "FanOn":
                return "fan_on";
                
            case "FanOff":
                return "fan_off";
                
            case "FanUp":
                return "increase_fan_speed";

            case "FanDown":
                return "decrease_fan_speed";

            case "SetThermo":
                return "set_thermo";

            case "Num0":
                return "num0";

            case "Num1":
                return "num1";

            case "Num2":
                return "num2";

            case "Num3":
                return "num3";

            case "Num4":
                return "num4";

            case "Num5":
                return "num5";

            case "Num6":
                return "num6";

            case "Num7":
                return "num7";

            case "Num8":
                return "num8";

            case "Num9":
                return "num9";

            default:
                throw new IllegalStateException("Unexpected value: " + receivedGestureName);
        }

    }
}