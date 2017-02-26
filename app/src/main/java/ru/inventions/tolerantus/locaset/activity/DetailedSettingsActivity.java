package ru.inventions.tolerantus.locaset.activity;

import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.sql.SQLException;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.async.AddressRefreshTask;
import ru.inventions.tolerantus.locaset.async.CoordinatesSavingTask;
import ru.inventions.tolerantus.locaset.async.LocationUpdateTask;
import ru.inventions.tolerantus.locaset.async.ThreadPoolProvider;
import ru.inventions.tolerantus.locaset.db.Location;
import ru.inventions.tolerantus.locaset.db.OrmDbOpenHelper;
import ru.inventions.tolerantus.locaset.util.LocationActionEnum;

import static ru.inventions.tolerantus.locaset.util.LogUtils.debug;
import static ru.inventions.tolerantus.locaset.util.LogUtils.error;

/**
 * Created by Aleksandr on 23.01.2017.
 */

public class DetailedSettingsActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private EditText etLocationName;
    private EditText etLatitude;
    private EditText etLongitude;
    private EditText etAltitude;
    private EditText etRadius;

    private String initialLatitude;
    private String initialLongitude;

    private SeekBar sbRingtone;
    private int ringMax;
    private SeekBar sbMusic;
    private int musicMax;
    private SeekBar sbNotification;
    private int notifMax;
    private Switch sVibro;

    private Location location;

    private Long locationId;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_settings);

        initViews();

        Intent startIntent = getIntent();
        locationId = startIntent.getLongExtra("locationId", -1);

        if (locationId != -1) {
            initTabs();
            initViewsContent();
        }
    }

    private void initTabs() {
        TabHost tabHost = (TabHost) findViewById(R.id.tabhost);
        tabHost.setup();
        TabHost.TabSpec tabSpec;

        tabSpec = tabHost.newTabSpec("Coordinates");
        tabSpec.setIndicator("Coordinates");
        // указываем id компонента из FrameLayout, он и станет содержимым
        tabSpec.setContent(R.id.scroll_geo_settings);
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("Audio");
        tabSpec.setIndicator("Audio");
        // указываем id компонента из FrameLayout, он и станет содержимым
        tabSpec.setContent(R.id.audio_settings);
        tabHost.addTab(tabSpec);
    }

    private void initViews() {
        etLocationName = ((EditText) findViewById(R.id.et_location_name));
        etLatitude = ((EditText) findViewById(R.id.et_latitude));
        etLongitude = ((EditText) findViewById(R.id.et_longitude));
        etAltitude = ((EditText) findViewById(R.id.et_altitude));
        etRadius = ((EditText) findViewById(R.id.et_radius));

        AudioManager audioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
        sbRingtone = ((SeekBar) findViewById(R.id.sb_ringtone));
        ringMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        sbNotification = ((SeekBar) findViewById(R.id.sb_notification));
        notifMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        sbMusic = ((SeekBar) findViewById(R.id.sb_system));
        musicMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        sVibro = (Switch) findViewById(R.id.s_vibration);

        sbRingtone.setMax(ringMax);
        sbNotification.setMax(notifMax);
        sbMusic.setMax(musicMax);

        sbRingtone.setOnSeekBarChangeListener(this);
    }

    private void initViewsContent() {
        OrmDbOpenHelper ormDbOpenHelper = OpenHelperManager.getHelper(this, OrmDbOpenHelper.class);
        try {
            Location location = ormDbOpenHelper.getDao().queryForId(locationId);
            this.location = location;
            etLocationName.setText(location.getName());
            etLatitude.setText(Double.toString(location.getLatitude()));
            etLongitude.setText(Double.toString(location.getLongitude()));
            etAltitude.setText(Double.toString(location.getAltitude()));
            initialLatitude = etLatitude.getText().toString();
            initialLongitude = etLongitude.getText().toString();
            etRadius.setText(Double.toString(location.getRadius()));

            sbRingtone.setProgress(((int) (ringMax * location.getRingtoneVol())));
            sbNotification.setProgress(((int) (notifMax * location.getNotificationVol())));
            sbMusic.setProgress(((int) (musicMax * location.getMusicVol())));

            if (sbRingtone.getProgress() > 0) {
                sVibro.setChecked(true);
                sVibro.setEnabled(false);
            } else {
                sVibro.setChecked(location.isVibro());
            }
        } catch (SQLException e) {
            error("error during location reading:" + e.getMessage());
        }
    }

    private void save() {
        location.setName(etLocationName.getText().toString());
        location.setLatitude(Double.parseDouble(etLatitude.getText().toString()));
        location.setRadius(Double.parseDouble(etRadius.getText().toString()));
        location.setLongitude(Double.parseDouble(etLongitude.getText().toString()));
        location.setAltitude(Double.parseDouble(etAltitude.getText().toString()));
        location.setRingtoneVol(sbRingtone.getProgress() * 1f / ringMax);
        location.setMusicVol(sbMusic.getProgress() * 1f / musicMax);
        location.setNotificationVol(sbNotification.getProgress() * 1f / notifMax);
        location.setVibro(sVibro.isChecked());

        LocationUpdateTask task = new LocationUpdateTask(location, LocationActionEnum.UPDATE, this, null, null);
        task.executeOnExecutor(ThreadPoolProvider.getCachedInstance());

        debug("Saving location");
        Toast.makeText(this, "Saving location", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
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

    private boolean isMarkerMoved() {
        return !initialLatitude.equals(etLatitude.getText().toString()) || !initialLongitude.equals(etLongitude.getText().toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}


