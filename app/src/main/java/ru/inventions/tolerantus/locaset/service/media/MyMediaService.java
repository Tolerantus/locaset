package ru.inventions.tolerantus.locaset.service.media;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import java.util.Observable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.activity.MainActivity;

import static ru.inventions.tolerantus.locaset.util.LogUtils.debug;

/**
 * Created by Aleksandr on 13.01.2017.
 */

public class MyMediaService extends Observable{

    private static MyMediaService instance;
    public static AtomicBoolean isCurrentPreferenceValid = new AtomicBoolean(false);
    public static final AtomicLong currentPreferenceId = new AtomicLong(-1);

    private MyMediaService() {
    }

    public void adjustAudio(AudioPreferences preferences, Context context) {
        synchronized (currentPreferenceId) {
            if (!isCurrentPreferenceValid.get() || currentPreferenceId.get() != preferences.getPreferenceId()) {
                if (context.getSharedPreferences("global", Context.MODE_PRIVATE).getBoolean("notifications", true)) {
                    NotificationManager notificationManager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
                    Intent mainPageIntent = new Intent(context, MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainPageIntent, 0);
                    Notification.Builder builder = new Notification.Builder(context)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Locaset")
                            .setContentText(preferences.getLocationName())
                            .setContentIntent(pendingIntent);
                    Notification notification = builder.build();
                    notificationManager.notify(777, notification);
                }
                debug("adjusting audio preferences, current preferences id = " + preferences.getPreferenceId());
                debug("preferences changes\n " + preferences);
                AudioManager amanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (!preferences.isVibro() && preferences.getRingtoneVolume() == 0){
                    mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                    amanager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                } else {
                    mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                    amanager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                }
                amanager.setStreamVolume(AudioManager.STREAM_RING, (int) (amanager.getStreamMaxVolume(AudioManager.STREAM_RING) * preferences.getRingtoneVolume()), 0);
                amanager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (amanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * preferences.getMusicVolume()), 0);
                amanager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, (int) (amanager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) * preferences.getNotificationVolume()), 0);
                currentPreferenceId.set(preferences.getPreferenceId());
                isCurrentPreferenceValid.set(true);
            }
        }
    }

    public static MyMediaService getInstance() {
        if (instance == null) {
            instance = new MyMediaService();
        }
        return instance;
    }
}
