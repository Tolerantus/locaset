package ru.inventions.tolerantus.locaset.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Dao;

/**
 * Created by Aleksandr on 23.01.2017.
 */

public class DetailedSettingsActivity extends AppCompatActivity{

    private EditText etLocationName;
    private EditText etLatitude;
    private EditText etLongitude;
    private EditText etAltitude;

    private SeekBar sbRingtone;
    private int max;

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

        sbRingtone = ((SeekBar) findViewById(R.id.sb_ringtone));
        AudioManager audioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
        max = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        sbRingtone.setMax(max);
    }

    private void initViewsContent() {
        Cursor locationData = dao.getLocationById(locationId);
        if (locationData.moveToFirst()) {
            etLocationName.setText(locationData.getString(locationData.getColumnIndex(getString(R.string.location_name_column))));
            etLatitude.setText(locationData.getString(locationData.getColumnIndex(getString(R.string.latitude_column))));
            etLongitude.setText(locationData.getString(locationData.getColumnIndex(getString(R.string.longitude_column))));
            etAltitude.setText(locationData.getString(locationData.getColumnIndex(getString(R.string.altitude_column))));
            sbRingtone.setProgress(((int) (max * locationData.getFloat(locationData.getColumnIndex(getString(R.string.ringtone_volume_column))))));
        }
    }

    private void save() {
        String locationName = etLocationName.getText().toString();
        Double latitude = Double.parseDouble(etLatitude.getText().toString());
        Double longitude = Double.parseDouble(etLongitude.getText().toString());
        Double altitude = Double.parseDouble(etAltitude.getText().toString());
        Double ringtoneVolume = sbRingtone.getProgress()*1.0/max;
        dao.updateLocation(locationId, locationName, latitude, longitude, altitude, ringtoneVolume);
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
}


