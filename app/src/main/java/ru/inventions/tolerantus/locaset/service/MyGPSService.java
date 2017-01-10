package ru.inventions.tolerantus.locaset.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


/**
 * Created by Aleksandr on 07.01.2017.
 */

public class MyGPSService extends Service {
    private String t = "Locaset";
    private static boolean isOnline;
    private GoogleApiClient googleApiClient;
    ExecutorService executorService;

    @Override
    public void onCreate() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switchOnService();
        Log.d(t, "starting MyGPSService");

        Runnable task = new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        Log.d(t, "creating GpsLookupTask");
                        GpsLookupTask task = new GpsLookupTask(MyGPSService.this);
                        task.execute();
                        Thread.sleep(60 * 1_000);
                    } catch (InterruptedException e) {
                        Log.d(t, "Thread of MyGPSService has been interrupted!");
                        Thread.currentThread().interrupt();
                    }
                } while (isOnline);
            }
        };
        executorService.submit(task);
        Log.d(t, "MyGPSService finished work");

        // If we get killed, after returning from here, restart
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }


    @Override
    public void onDestroy() {
        shutdownService();
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    public static boolean isServiceOnline() {
        return isOnline;
    }

    public static void shutdownService() {
        isOnline = false;
    }

    public static void switchOnService() {
        isOnline = true;
    }

}
