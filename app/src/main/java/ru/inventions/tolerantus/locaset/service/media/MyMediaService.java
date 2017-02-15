package ru.inventions.tolerantus.locaset.service.media;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;

import java.util.concurrent.atomic.AtomicLong;

import static ru.inventions.tolerantus.locaset.util.LogUtils.debug;

/**
 * Created by Aleksandr on 13.01.2017.
 */

public class MyMediaService {

    private static MyMediaService instance;
    public static final AtomicLong currentPreferenceId = new AtomicLong(-1);

    private MyMediaService() {

    }

    public void adjustAudio(AudioPreferences preferences, Context context) {
        synchronized (currentPreferenceId) {
            if (currentPreferenceId.get() != preferences.getPreferenceId()) {
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
