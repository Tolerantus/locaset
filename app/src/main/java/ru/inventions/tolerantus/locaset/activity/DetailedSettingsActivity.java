package ru.inventions.tolerantus.locaset.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Geocoder;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import java.util.Locale;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Dao;
import ru.inventions.tolerantus.locaset.service.media.MyMediaService;
import ru.inventions.tolerantus.locaset.util.AddressUtils;
import ru.inventions.tolerantus.locaset.util.CvBuilder;

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

    private Long locationId;

    private Dao dao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_settings);
        dao = new Dao(this);

        initViews();

        Intent startIntent = getIntent();
        locationId = startIntent.getLongExtra("locationId", -1);
        if (locationId != -1) {
            initViewsContent();
        }
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
        Cursor locationData = dao.getLocationById(locationId);
        if (locationData.moveToFirst()) {
            etLocationName.setText(locationData.getString(locationData.getColumnIndex(getString(R.string.location_name_column))));
            etLatitude.setText(locationData.getString(locationData.getColumnIndex(getString(R.string.latitude_column))));
            etLongitude.setText(locationData.getString(locationData.getColumnIndex(getString(R.string.longitude_column))));
            etAltitude.setText(locationData.getString(locationData.getColumnIndex(getString(R.string.altitude_column))));
            initialLatitude = etLatitude.getText().toString();
            initialLongitude = etLongitude.getText().toString();
            etRadius.setText(locationData.getString(locationData.getColumnIndex(getString(R.string.radius))));

            sbRingtone.setProgress(((int) (ringMax * locationData.getFloat(locationData.getColumnIndex(getString(R.string.ringtone_volume_column))))));
            sbNotification.setProgress(((int) (notifMax * locationData.getFloat(locationData.getColumnIndex(getString(R.string.notification_volume))))));
            sbMusic.setProgress(((int) (musicMax * locationData.getFloat(locationData.getColumnIndex(getString(R.string.music_volume))))));

            if (sbRingtone.getProgress() > 0) {
                sVibro.setChecked(true);
                sVibro.setEnabled(false);
            } else {
                sVibro.setChecked(locationData.getInt(locationData.getColumnIndex(getString(R.string.vibration))) != 0);
            }
        }
    }

    private void save() {
        String locationName = etLocationName.getText().toString();
        Double latitude = Double.parseDouble(etLatitude.getText().toString());
        Double radius = Double.parseDouble(etRadius.getText().toString());
        Double longitude = Double.parseDouble(etLongitude.getText().toString());
        Double altitude = Double.parseDouble(etAltitude.getText().toString());
        Double ringtoneVolume = sbRingtone.getProgress() * 1.0 / ringMax;
        Double musicVolume = sbMusic.getProgress() * 1.0 / musicMax;
        Double notificationVolume = sbNotification.getProgress() * 1.0 / notifMax;
        boolean vibro = sVibro.isChecked();
        CvBuilder cvBuilder = CvBuilder.create()
                .append(getString(R.string.location_name_column), locationName)
                .append(getString(R.string.radius), radius.intValue())
                .append(getString(R.string.latitude_column), latitude)
                .append(getString(R.string.longitude_column), longitude)
                .append(getString(R.string.altitude_column), altitude)
                .append(getString(R.string.ringtone_volume_column), ringtoneVolume)
                .append(getString(R.string.music_volume), musicVolume)
                .append(getString(R.string.notification_volume), notificationVolume)
                .append(getString(R.string.vibration), vibro?1:0);
        if (isMarkerMoved() || !isAddressInitialized(locationId)) {
            String newAddress = AddressUtils.getStringAddress(latitude, longitude, this);
            cvBuilder.append(getString(R.string.address), newAddress);
        }

        dao.updateLocation(locationId, cvBuilder.get());

        if (MyMediaService.currentPreferenceId.get() == locationId) {
            MyMediaService.currentPreferenceId.set(-1);
        }
        Log.d(this.getClass().getSimpleName(), "Saving location");
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        save();
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

    private boolean isAddressInitialized(long id) {
        Cursor c = dao.getLocationById(id);
        if (c.moveToFirst()) {
            String address = c.getString(c.getColumnIndex(getString(R.string.address)));
            if (address == null || address.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}


