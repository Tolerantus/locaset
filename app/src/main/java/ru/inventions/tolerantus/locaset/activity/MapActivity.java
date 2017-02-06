package ru.inventions.tolerantus.locaset.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.concurrent.Executors;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Dao;
import ru.inventions.tolerantus.locaset.service.LocationPreferencesSavingTask;
import ru.inventions.tolerantus.locaset.util.MyCachedThreadPoolProvider;
import ru.inventions.tolerantus.locaset.util.Validator;

/**
 * Created by Aleksandr on 06.01.2017.
 */

public class MapActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener {
    private SupportMapFragment mapFragment;
    private GoogleMap map;
    private Dao dao;
    private Validator validator;
    private float latitude;
    private float longitude;
    private int radius;
    private Circle zone;
    private Marker marker;
    private GoogleApiClient googleApiClient;
    private Location lastKnownLocation;
    private LocationRequest mLocationRequest;
    private String locationName;
    private Long locationId;

    private boolean locationChanged;

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

        }
    }

    private LocationPreferencesSavingTask saveLocation(Intent afterSavingIntent) {
        LocationPreferencesSavingTask task = null;
        locationId = getIntent().getLongExtra(getString(R.string.location_id), -1);
        if (locationId == -1) {
            throw new IllegalArgumentException("Incorrect location id value!!!");
        }
        task = new LocationPreferencesSavingTask(this, dao, locationId, marker.getPosition().latitude, marker.getPosition().longitude, afterSavingIntent);
        task.executeOnExecutor(MyCachedThreadPoolProvider.getInstance());
        return task;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Details");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                Intent audioIntent = new Intent(this, DetailedSettingsActivity.class);
                audioIntent.putExtra(getString(R.string.location_id), getIntent().getLongExtra(getString(R.string.location_id), -1));
                if (locationChanged) {
                    saveLocation(audioIntent);
                } else {
                    startActivity(audioIntent);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_review);
        dao = new Dao(this);
        validator = new Validator();
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        locationId = getIntent().getLongExtra(getString(R.string.location_id), -1);

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
                locationName = c.getString(c.getColumnIndex(getString(R.string.location_name_column)));
                latitude = c.getFloat(c.getColumnIndex(getString(R.string.latitude_column)));
                longitude = c.getFloat(c.getColumnIndex(getString(R.string.longitude_column)));
                radius = c.getInt(c.getColumnIndex(getString(R.string.radius)));
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
            map.getUiSettings().setZoomControlsEnabled(true);
            map.setIndoorEnabled(true);
            addMarker(new LatLng(latitude, longitude));
            moveCamera(latitude, longitude);

            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    Log.d(this.getClass().getSimpleName(), "New marker has been created");
                    addMarker(latLng);
                    locationChanged = true;
                }
            });
            map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    Log.d(this.getClass().getSimpleName(), "My location button has been pressed");
                    if (lastKnownLocation != null) {
                        moveCamera(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    }
                    return true;
                }
            });
            map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    moveCamera(latLng.latitude, latLng.longitude);
                }
            });
        }
    }

    private void addMarker(LatLng position) {
        if (marker != null) {
            marker.remove();
        }
        if (zone != null) {
            zone.remove();
        }
        marker = map.addMarker(new MarkerOptions().position(position).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        marker.setTitle(locationName);
        marker.showInfoWindow();
        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(position.latitude, position.longitude)).radius(radius)
                .strokeColor(Color.RED)
                .strokeWidth(2);

        zone =  map.addCircle(circleOptions);
    }

    private void moveCamera(double latitude, double longitude) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(15)
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
            if (lastKnownLocation != null) {
                Log.d(this.getClass().getSimpleName(), lastKnownLocation.toString());
            } else {
                Log.d(this.getClass().getSimpleName(), "Last known location hasn't been initialized yet");
            }
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
            if (lastKnownLocation != null) {
                Log.d(this.getClass().getSimpleName(), lastKnownLocation.toString());
            } else {
                Log.d(this.getClass().getSimpleName(), "Last known location hasn't been initialized yet");
            }
        }
    }

    @Override
    protected void onDestroy() {
        Intent nowhere = null;
        saveLocation(nowhere);
        googleApiClient.disconnect();
        super.onDestroy();
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        readDataFromDB(locationId);
        Log.d(this.getClass().getSimpleName(), locationName);
        initMapSettings();
        locationChanged = false;
        super.onResume();
    }
}
