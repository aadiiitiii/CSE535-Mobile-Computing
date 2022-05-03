package com.example.smarthomegesturecontrol;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class Screen3_Record_Video extends AppCompatActivity {

    Button upload_button;
    static final int REQUEST_VIDEO_CAPTURE;
    String gestureName;

    static {
        REQUEST_VIDEO_CAPTURE = 1;
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    Integer practice_number=0;
    Uri videoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen3_record_video);
        upload_button = (Button) findViewById(R.id.upload_button);
        dispatchTakeVideoIntent();
        gestureName = getIntent().getStringExtra(Screen2_Display_Video.GESTURE_NAME);
        verifyStoragePermissions(Screen3_Record_Video.this);
        practice_number = getIntent().getIntExtra(Screen2_Display_Video.PRACTICE_NUMBER, 0);
        practice_number=practice_number%3;

        upload_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new flask_file_upload().execute();
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(gestureName, Context.MODE_PRIVATE);
                practice_number = sharedPref.getInt(gestureName, 1);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(gestureName, (practice_number+1)%3);
                editor.apply();
                startActivity(new Intent(Screen3_Record_Video.this, MainActivity.class));
            }
        });
    }

    private void verifyStoragePermissions(Screen3_Record_Video screen3_record_video) {
        int permission = ActivityCompat.checkSelfPermission(screen3_record_video, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(screen3_record_video, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
    }

    private void dispatchTakeVideoIntent() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 5);
        Log.v("hello", "hello");
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            videoUri = intent.getData();
            Log.v("uri:", String.valueOf(videoUri));
        }
    }

    private String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }
    private class flask_file_upload extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... params) {
            try {
                String source_file_uri = getRealPathFromURI(getApplicationContext(), videoUri);
                Log.v("Source file uri:", videoUri.getPath());

                HttpURLConnection connector = null;
                DataOutputStream data_output_stream = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;
                File sourceFile = new File(source_file_uri);

                String gesture_file_name = gestureName + "_" + "PRACTICE_" + practice_number + "_JOSHI.mp4";
                Log.v("file name:", gesture_file_name);
                if (sourceFile.isFile()) {
                    try {
                        String upload_server_uri = "http://192.168.0.25:5000/api/upload";
                        Log.v("server uri:", upload_server_uri);

                        FileInputStream fileInputStream = new FileInputStream(sourceFile);
                        URL url = new URL(upload_server_uri);
                        Log.v("url:", url.toString());

                        connector = (HttpURLConnection) url.openConnection();
                        connector.setDoInput(true);
                        connector.setDoOutput(true);
                        connector.setUseCaches(false);
                        connector.setRequestMethod("POST");
                        connector.setRequestProperty("Connection", "Keep-Alive");
                        connector.setRequestProperty("ENCTYPE", "multipart/form-data");
                        connector.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                        connector.setRequestProperty("myfile", gesture_file_name);

                        Log.v("connection:", connector.toString());

                        data_output_stream = new DataOutputStream(connector.getOutputStream());

                        data_output_stream.writeBytes(twoHyphens + boundary + lineEnd);
                        data_output_stream.writeBytes("Content-Disposition: form-data; name=\"myfile\";filename=\"" + gesture_file_name + "\"" + lineEnd);
                        data_output_stream.writeBytes(lineEnd);

                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];

                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                        while (bytesRead > 0) {
                            data_output_stream.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math.min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                        }

                        data_output_stream.writeBytes(lineEnd);
                        data_output_stream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                        int serverResponseCode = 0;
                        serverResponseCode = connector.getResponseCode();
                        String serverResponseMessage = connector.getResponseMessage();

                        Log.v("server response code:", serverResponseMessage);

                        if (serverResponseCode == 200) {
                            Log.v(" File uploaded : ", gesture_file_name);

                        }

                        fileInputStream.close();
                        data_output_stream.flush();
                        data_output_stream.close();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (ProtocolException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            return "DONE";
        }

        @Override
        protected void onPostExecute(String result) {
            Context context = getApplicationContext();
            CharSequence text = gestureName + "_" + "PRACTICE_" + practice_number + "_JOSHI.mp4 "+"Video uploaded!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }

        @Override
        protected void onPreExecute() {
            Context context = getApplicationContext();
            CharSequence text = "Video uploading started!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }
}