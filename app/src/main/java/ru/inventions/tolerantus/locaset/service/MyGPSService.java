package ru.inventions.tolerantus.locaset.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.activity.MainActivity;
import ru.inventions.tolerantus.locaset.async.GpsLookupTask;
import ru.inventions.tolerantus.locaset.async.ThreadPoolProvider;

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
        Intent mainPageIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mainPageIntent, 0);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Locaset")
                .setContentText("Searching...")
                .setContentIntent(pendingIntent);
        Notification notification = builder.build();
        startForeground(777, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (task == null) {
            startUpService();
            debug("starting MyGPSService");
            task = new GpsLookupTask(MyGPSService.this);
            task.executeOnExecutor(ThreadPoolProvider.getCachedInstance());
        }
        return START_STICKY;
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
        stopForeground(true);
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

    public static void startUpService() {
        isOnline = true;
    }

}
