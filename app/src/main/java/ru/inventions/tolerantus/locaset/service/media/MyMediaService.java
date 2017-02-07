package ru.inventions.tolerantus.locaset.service.media;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import java.util.concurrent.atomic.AtomicLong;

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
                Log.d(this.getClass().getSimpleName(), "adjusting audio preferences, current preferences id = " + preferences.getPreferenceId());
                Log.d(this.getClass().getSimpleName(), "preferences changes\n " + preferences);
                AudioManager amanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                amanager.setStreamVolume(AudioManager.STREAM_RING, (int) (amanager.getStreamMaxVolume(AudioManager.STREAM_RING) * preferences.getRingtoneVolume()), 0);
                amanager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (amanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * preferences.getMusicVolume()), 0);
                amanager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, (int) (amanager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) * preferences.getNotificationVolume()), 0);
                if (!preferences.isVibro() && preferences.getRingtoneVolume() == 0){
                    amanager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                } else {
                    amanager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                }
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