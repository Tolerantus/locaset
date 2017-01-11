package ru.inventions.tolerantus.locaset;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.http.RequestQueue;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;

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
            dao.updateLocation(id, locationName, marker.getPosition().latitude, marker.getPosition().longitude, getAltitude(), volume);
            Toast.makeText(this, "Location was successfully saved", Toast.LENGTH_SHORT).show();
        }
    }

    private double getAltitude() {
        double result = 0d;
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        String url = "https://maps.googleapis.com/maps/api/elevation/json?locations=" + marker.getPosition().latitude + "," + marker.getPosition().longitude + "&key=" + getString(R.string.API_key);
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpGet, localContext);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                int r = -1;
                StringBuffer respStr = new StringBuffer();
                while ((r = instream.read()) != -1)
                    respStr.append((char) r);
                String tagOpen = "<double>";
                String tagClose = "</double>";
                if (respStr.indexOf(tagOpen) != -1) {
                    int start = respStr.indexOf(tagOpen) + tagOpen.length();
                    int end = respStr.indexOf(tagClose);
                    String value = respStr.substring(start, end);
                    result = Double.parseDouble(value);
                }
                instream.close();
            }
        } catch (Exception e) {
            Log.e("SettingsActivity", "some http error", e);
        }
        return result;
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
