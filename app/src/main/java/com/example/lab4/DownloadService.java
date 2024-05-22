package com.example.lab4;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends IntentService {

    public static final String ACTION_PROGRESS_UPDATE = "com.example.lab4.PROGRESS_UPDATE";
    //public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_TOTAL_BYTES = "totalBytes";
    public static final String EXTRA_BYTES_DOWNLOADED = "bytesDownloaded";

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "download_channel";

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            String url = intent.getStringExtra("url");
            downloadFile(url);
        }
    }

    private void downloadFile(String urlString) {
        int totalBytes, bytesDownloaded;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // total file size
            totalBytes = connection.getContentLength();
            bytesDownloaded = 0;

            // file output stream
            File file = new File(Environment.getExternalStorageDirectory(), "currentprice.json");
            FileOutputStream outputStream = new FileOutputStream(file);
            InputStream inputStream = new BufferedInputStream(url.openStream(), 8192);

            byte[] data = new byte[1024];
            int count;
            while ((count = inputStream.read(data)) != -1) {
                bytesDownloaded += count;
                outputStream.write(data, 0, count);

                // progress update
                sendProgressUpdate(totalBytes, bytesDownloaded);
                showDownloadProgressNotification(totalBytes, bytesDownloaded);
            }

            // closing streams
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            //  download complete notification
            showDownloadCompleteNotification(file);

        } catch (IOException e) {
            Log.e(getString(R.string.downloadservice), getString(R.string.error_downloading_file) + e.getMessage());
        }
    }

    private void sendProgressUpdate(int totalBytes, int bytesDownloaded) {
        Intent progressIntent = new Intent();
        progressIntent.setAction(ACTION_PROGRESS_UPDATE);
        progressIntent.putExtra(EXTRA_TOTAL_BYTES, totalBytes);
        progressIntent.putExtra(EXTRA_BYTES_DOWNLOADED, bytesDownloaded);
        sendBroadcast(progressIntent);
    }

    private void showDownloadProgressNotification(int totalBytes, int bytesDownloaded) {
        //  calculate progress percentage
        int progress = ((bytesDownloaded * 100) / totalBytes);

        // build and show notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (progress == 100) {
            notificationManager.cancel(NOTIFICATION_ID); // cancel the notification when progress reaches 100%
            return;
        }

        Notification notification = buildProgressNotification(progress);

        //  intent to open MainActivity when notification is clicked
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // attaching  PendingIntent to the notification
        notification.contentIntent = pendingIntent;

        // notifying the user
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    //progress notification
    private Notification buildProgressNotification(int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.download_progress))
                .setContentText(getString(R.string.download_in_progress_text))
                .setSmallIcon(R.drawable.ic_download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setProgress(100, progress, false);
        return builder.build();
    }


    private void showDownloadCompleteNotification(File file) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // registering the channel with the system
            NotificationManager notificationManagerCompat = getSystemService(NotificationManager.class);
            if (notificationManagerCompat != null) {
                notificationManagerCompat.createNotificationChannel(channel);
            }
        }

        // intent to open the main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // building the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.download_complete)
                .setContentTitle(getString(R.string.download_complete))
                .setContentText(getString(R.string.file_downloaded))
                .setContentIntent(pendingIntent)
                .setProgress(0, 0, false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // showing the notification
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    /*private Notification buildNotification(int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Nazwa aplikacji")
                .setContentText("Pobieranie pliku")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, progress, false);
        return builder.build();
    }*/

   /* private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Download Channel";
            String description = "Channel for download notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }*/
}
