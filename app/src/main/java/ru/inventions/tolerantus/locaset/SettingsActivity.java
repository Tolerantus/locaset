package ru.inventions.tolerantus.locaset;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.http.RequestQueue;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;


import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import ru.inventions.tolerantus.locaset.db.Dao;
import ru.inventions.tolerantus.locaset.service.AltitudeReceivingTask;
import ru.inventions.tolerantus.locaset.util.Validator;

/**
 * Created by Aleksandr on 06.01.2017.
 */

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener {
    private SupportMapFragment mapFragment;
    private GoogleMap map;
    private Dao dao;
    private Validator validator;
    private float latitude;
    private float longitude;
    private Marker marker;
    private GoogleApiClient googleApiClient;
    private Location lastKnownLocation;
    private LocationRequest mLocationRequest;

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save:
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
            AltitudeReceivingTask task = new AltitudeReceivingTask(this, dao, id, locationName, marker.getPosition().latitude, marker.getPosition().longitude, volume);
            task.execute();
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
        initMyLocationSearcherService();
    }

    private void initMyLocationSearcherService() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .build();
        }
        googleApiClient.connect();
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void readDataFromDB(long locationId) {
        if (locationId != -1) {
            Cursor c = dao.getLocationById(locationId);
            if (c.moveToFirst()) {
                String locationName = c.getString(c.getColumnIndex(getString(R.string.location_name_column)));
                ((EditText) findViewById(R.id.et_location_name)).setText(locationName);
                int volume = c.getInt(c.getColumnIndex(getString(R.string.volume_column)));
                ((SeekBar) findViewById(R.id.sb_volume)).setMax(100);
                ((SeekBar) findViewById(R.id.sb_volume)).setProgress(volume);
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
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
            }
            map.setIndoorEnabled(true);
            moveCamera(latitude, longitude);
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
                    if (lastKnownLocation == null) {
                        updateLastKnownLocation();
                    }
                    moveCamera(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    Toast.makeText(SettingsActivity.this, "You somewhere here", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    moveCamera(marker.getPosition().latitude, marker.getPosition().longitude);
                    return true;
                }
            });
        }
    }

    private void moveCamera(double latitude, double longitude) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(15)
                .bearing(45)
                .tilt(20)
                .build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
        map.animateCamera(cameraUpdate);
    }

    private void initMapSettings() {
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Here, thisActivity is the current activity
        Log.d(this.getClass().getSimpleName(), "Google connected");
        updateLastKnownLocation();
    }

    private void updateLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
            lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(
                    googleApiClient);
            Log.d(this.getClass().getSimpleName(), lastKnownLocation.toString());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(this.getClass().getSimpleName(), "Location changed, applying changes");
            lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(
                    googleApiClient);
            Log.d(this.getClass().getSimpleName(), lastKnownLocation.toString());
        }
    }

    @Override
    protected void onDestroy() {
        googleApiClient.connect();
        super.onDestroy();
    }
}
