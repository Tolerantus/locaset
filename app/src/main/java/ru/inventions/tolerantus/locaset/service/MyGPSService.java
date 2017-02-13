package ru.inventions.tolerantus.locaset.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import ru.inventions.tolerantus.locaset.async.GpsLookupTask;
import ru.inventions.tolerantus.locaset.async.MyCachedThreadPoolProvider;

import static ru.inventions.tolerantus.locaset.util.LogUtils.debug;

/**
 * Created by Aleksandr on 07.01.2017.
 */

public class MyGPSService extends Service {
    private static boolean isOnline;
    private GoogleApiClient googleApiClient;
    private GpsLookupTask task;

    @Override
    public void onCreate() {
        debug("creating MyGPSService");
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isOnline) {
            switchOnService();
            debug("starting MyGPSService");
            task = new GpsLookupTask(MyGPSService.this);
            task.executeOnExecutor(MyCachedThreadPoolProvider.getInstance());
        }
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
        debug("destroying MyGPSService");
        shutdownService();
        if (task != null) {
            task.cancel(false);
        }
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
