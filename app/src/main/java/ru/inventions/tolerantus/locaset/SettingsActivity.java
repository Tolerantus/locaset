package ru.inventions.tolerantus.locaset;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import ru.inventions.tolerantus.locaset.db.Dao;
import ru.inventions.tolerantus.locaset.util.Validator;

/**
 * Created by Aleksandr on 06.01.2017.
 */

public class SettingsActivity extends AppCompatActivity  implements View.OnClickListener, OnMapReadyCallback {
    private SupportMapFragment mapFragment;
    private GoogleMap map;
    private Dao dao;
    private Validator validator;
    private float latitude;
    private float longitude;
    private Marker marker;

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save :
                saveLocation();
        }
    }

    private void saveLocation() {
        long id = getIntent().getLongExtra(getString(R.string.location_name_column), -1);
        if (id == -1) {
            throw new IllegalArgumentException("Incorrect location id value!!!");
        }
        String locationName = ((EditText) findViewById(R.id.et_location_name)).getText().toString();
        int volume = ((SeekBar) findViewById(R.id.sb_volume)).getProgress();
        if (validator.validateLocationName(locationName)) {
            dao.updateLocation(id, locationName, marker.getPosition().latitude, marker.getPosition().longitude, volume);
            Toast.makeText(this, "Location was successfully saved", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        dao = new Dao(this);
        validator = new Validator();
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        readDataFromDB(getIntent().getLongExtra(getString(R.string.location_name_column), -1));
        initMapSettings();

        findViewById(R.id.save).setOnClickListener(this);
        checkSelfPermission("android.permission.ACCESS_FINE_LOCATION");
    }

    private void readDataFromDB(long locationId) {
        if (locationId != -1) {
            Cursor c = dao.getLocationById(locationId);
            if (c.moveToFirst()) {
                String locationName = c.getString(c.getColumnIndex(getString(R.string.location_name_column)));
                ((EditText) findViewById(R.id.et_location_name)).setText(locationName);
                int volume = c.getInt(c.getColumnIndex(getString(R.string.volume_column)));
                ((SeekBar)findViewById(R.id.sb_volume)).setMax(100);
                ((SeekBar)findViewById(R.id.sb_volume)).setProgress(volume);
                latitude = c.getFloat(c.getColumnIndex(getString(R.string.latitude_column)));
                longitude = c.getFloat(c.getColumnIndex(getString(R.string.longitude_column)));
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (map != null) {
            map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
            }
            map.setIndoorEnabled(true);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(latitude, longitude))
                    .zoom(5)
                    .bearing(45)
                    .tilt(20)
                    .build();
            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
            map.animateCamera(cameraUpdate);
            marker = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)));
            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    marker.remove();
                    marker = map.addMarker(new MarkerOptions().position(latLng));
                }
            });
            map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(new LatLng(latitude, longitude))
                            .zoom(20)
                            .bearing(45)
                            .tilt(20)
                            .build();
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
                    map.animateCamera(cameraUpdate);
                    return true;
                }
            });
        }
    }

    private void initMapSettings() {
        mapFragment.getMapAsync(this);
    }
}
