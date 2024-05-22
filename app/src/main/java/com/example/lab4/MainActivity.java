package com.example.lab4;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;




public class MainActivity extends AppCompatActivity {
    private EditText urlEditText;
    private TextView fileSizeTextView;
    private TextView fileTypeTextView;
    private TextView bytesDownloadedTextView;
    private BroadcastReceiver progressReceiver;
   // private FileInformation fileInformation;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private static final String KEY_FILE_SIZE = "file_size";
    private static final String KEY_FILE_TYPE = "file_type";
    private static final String KEY_BYTES_DOWNLOADED = "bytes_downloaded";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        urlEditText = findViewById(R.id.urlEditText);
        urlEditText.setText(R.string.default_web);
        fileSizeTextView = findViewById(R.id.fileSizeTextView);
        fileTypeTextView = findViewById(R.id.fileTypeTextView);
        bytesDownloadedTextView = findViewById(R.id.bytesDownloadedTextView);

        if (savedInstanceState != null) {
            fileSizeTextView.setText(savedInstanceState.getString(KEY_FILE_SIZE));
            fileTypeTextView.setText(savedInstanceState.getString(KEY_FILE_TYPE));
            bytesDownloadedTextView.setText(savedInstanceState.getString(KEY_BYTES_DOWNLOADED));
        }

        Button getInfoButton = findViewById(R.id.getInfoButton);
        getInfoButton.setOnClickListener(view -> {
            String url = urlEditText.getText().toString();
            new DownloadTask().execute(url);
        });

        Button downloadButton = findViewById(R.id.downloadButton);
        downloadButton.setOnClickListener(v -> {
            String url = urlEditText.getText().toString();
            if (checkPermission()) {
                startDownloadService(url);
            } else {
                requestPermission();
            }
        });

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int totalBytes = intent.getIntExtra(DownloadService.EXTRA_TOTAL_BYTES, 0);
                int bytesDownloaded = intent.getIntExtra(DownloadService.EXTRA_BYTES_DOWNLOADED, 0);

                // update UI
                updateProgress(totalBytes, bytesDownloaded);
            }
        };
        registerReceiver(progressReceiver, new IntentFilter(DownloadService.ACTION_PROGRESS_UPDATE));
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_FILE_SIZE, fileSizeTextView.getText().toString());
        outState.putString(KEY_FILE_TYPE, fileTypeTextView.getText().toString());
        outState.putString(KEY_BYTES_DOWNLOADED, bytesDownloadedTextView.getText().toString());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(progressReceiver);
    }


    private void updateProgress(int totalBytes, int bytesDownloaded) {
        bytesDownloadedTextView.setText(String.valueOf(bytesDownloaded));

    }

    private void startDownloadService(String url) {
        Intent intent = new Intent(MainActivity.this, DownloadService.class);
        intent.putExtra(getString(R.string.url), url);
        startService(intent);
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String url = urlEditText.getText().toString();
                startDownloadService(url);
            }
        }
    }

    private class DownloadTask extends AsyncTask<String, Void, FileInformation> {
        @Override
        protected FileInformation doInBackground(String... strings) {
            String urlString = strings[0];
            FileInformation fileInfo = new FileInformation();

            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(getString(R.string.head));
                connection.connect();

                int contentLength = connection.getContentLength();
                String contentType = connection.getContentType();

                fileInfo.setFileSize(contentLength != -1 ? String.valueOf(contentLength) : getString(R.string.unknown));
                fileInfo.setFileType(contentType != null ? contentType : getString(R.string.unknown));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return fileInfo;
        }

        @Override
        protected void onPostExecute(FileInformation fileInformation) {
            fileSizeTextView.setText(fileInformation.getFileSize());
            fileTypeTextView.setText(fileInformation.getFileType());
        }
    }
}
