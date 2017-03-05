package ru.inventions.tolerantus.locaset.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.service.MyAlarmService;
import ru.inventions.tolerantus.locaset.service.Receiver;
import ru.inventions.tolerantus.locaset.service.media.MyMediaService;

/**
 * Created by Aleksandr on 25.02.2017.
 */

public class GlobalPreferencesActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private SharedPreferences globalPrefs;
    private SeekBar sbRingtone;
    private SeekBar sbMusic;
    private SeekBar sbNotification;
    private Switch sVibro;

    private int ringMax;
    private int musMax;
    private int notifMax;

    private EditText etGpsLookupCycle;
    private Switch sNotifications;
    private Switch sStickyNotifications;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_preferences);
        globalPrefs = getSharedPreferences("global", MODE_PRIVATE);


        TabHost tabHost = (TabHost) findViewById(R.id.tabhost);
        tabHost.setup();
        TabHost.TabSpec tabSpec = tabHost.newTabSpec("Audio");
        tabSpec.setIndicator("Audio");
        tabSpec.setContent(R.id.tab_audio);
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("Main settings");
        tabSpec.setIndicator("Main settings");
        tabSpec.setContent(R.id.tab_main_options);
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("Technical");
        tabSpec.setIndicator("Technical");
        tabSpec.setContent(R.id.tab_stats);
        tabHost.addTab(tabSpec);

        sbRingtone = ((SeekBar) findViewById(R.id.sb_ringtone));
        sbMusic = ((SeekBar) findViewById(R.id.sb_music));
        sbNotification = ((SeekBar) findViewById(R.id.sb_notification));
        sVibro = ((Switch) findViewById(R.id.s_vibro));

        etGpsLookupCycle = ((EditText) findViewById(R.id.et_gps_check_time));
        sNotifications = ((Switch) findViewById(R.id.sNotifications));
        sStickyNotifications = ((Switch) findViewById(R.id.sStickyNotofications));

        AudioManager audioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
        ringMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        musMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        notifMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);

        sbRingtone.setMax(ringMax);
        sbMusic.setMax(musMax);
        sbNotification.setMax(notifMax);

        float ringtoneVolume = globalPrefs.getFloat("ringtone", 0);
        float musicVolume = globalPrefs.getFloat("music", 0);
        float notificationVolume = globalPrefs.getFloat("notification", 0);
        boolean vibro = globalPrefs.getBoolean("vibro", false);

        sbRingtone.setProgress(((int) (ringMax * ringtoneVolume)));
        sbNotification.setProgress(((int) (notifMax * notificationVolume)));
        sbMusic.setProgress(((int) (musMax * musicVolume)));

        if (sbRingtone.getProgress() > 0) {
            sVibro.setChecked(true);
            sVibro.setEnabled(false);
        } else {
            sVibro.setChecked(vibro);
        }
        sbRingtone.setOnSeekBarChangeListener(this);

        etGpsLookupCycle.setText(Integer.toString(globalPrefs.getInt("gps_lookup_cycle", 60)));
        sNotifications.setChecked(globalPrefs.getBoolean("notifications", true));
        sStickyNotifications.setChecked(globalPrefs.getBoolean("sticky_notifications", false));

        ((TextView) findViewById(R.id.tv_calls)).setText("" + Receiver.numberOfCalls);
        Date lastExecutionDate = Receiver.lastExecutionDate;
        if (lastExecutionDate != null) {
            ((TextView) findViewById(R.id.tv_last_execution_date)).setText(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(lastExecutionDate));
        }
        ((TextView) findViewById(R.id.tv_lastDeterminedAddress)).setText("" + Receiver.lastDeterminedAddress);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getProgress() == 0) {
            sVibro.setEnabled(true);
        } else {
            sVibro.setChecked(true);
            sVibro.setEnabled(false);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void save() {
        SharedPreferences.Editor editor = globalPrefs.edit();
        editor.putFloat("notification", sbNotification.getProgress() * 1f / notifMax);
        editor.putFloat("music", sbMusic.getProgress() * 1f / musMax);
        editor.putFloat("ringtone", sbRingtone.getProgress() * 1f / ringMax);
        editor.putBoolean("vibro", sVibro.isChecked());
        int gpsCycle = etGpsLookupCycle.getText().toString().isEmpty() ?
                0 : Integer.parseInt(etGpsLookupCycle.getText().toString());
        editor.putInt("gps_lookup_cycle", gpsCycle);
        editor.putBoolean("notifications", sNotifications.isChecked());
        editor.putBoolean("sticky_notifications", sStickyNotifications.isChecked());
        editor.commit();
        if (MyMediaService.currentPreferenceId.get() == -1) {
            MyMediaService.isCurrentPreferenceValid.set(false);
        }
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                save();
                break;
        }
        return true;
    }
}
