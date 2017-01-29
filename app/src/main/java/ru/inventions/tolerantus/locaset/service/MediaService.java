package ru.inventions.tolerantus.locaset.service;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

/**
 * Created by Aleksandr on 13.01.2017.
 */

public class MediaService {

    private static MediaService instance;

    private MediaService() {

    }

    public void adjustAudio(AudioPreferences preferences, Context context) {
        Log.d(this.getClass().getSimpleName(), "adjusting audio preferences, current preferences id = " + preferences.getPreferenceId());

            Log.d(this.getClass().getSimpleName(), "preferences changes\n " + preferences );
            AudioManager amanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            amanager.setStreamVolume(AudioManager.STREAM_RING, (int) (amanager.getStreamMaxVolume(AudioManager.STREAM_RING) * preferences.getVolume()), 0);
    }

    public static MediaService getInstance() {
        if (instance == null) {
            instance = new MediaService();
        }
        return instance;
    }
}
