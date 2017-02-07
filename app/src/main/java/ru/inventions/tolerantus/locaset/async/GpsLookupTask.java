package ru.inventions.tolerantus.locaset.async;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.Map;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Dao;
import ru.inventions.tolerantus.locaset.service.media.AudioPreferences;
import ru.inventions.tolerantus.locaset.service.media.MyMediaService;

/**
 * Created by Aleksandr on 09.01.2017.
 */

public class GpsLookupTask extends AsyncTask<Void, Long, Void> implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private Dao dao;
    private GoogleApiClient googleApiClient;
    private Location currentLocation;
    private LocationRequest mLocationRequest;
    private Context context;

    private Map<Long, Float> crossedZones = new HashMap<>();

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(this.getClass().getSimpleName(), "Google connected");
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(this.getClass().getSimpleName(), "starting location lookup");
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
        }
    }

    private void stopLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(this.getClass().getSimpleName(), "stopping location lookup");
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    private void rememberLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(this.getClass().getSimpleName(), "updating last know location by demand");
            currentLocation = LocationServices.FusedLocationApi.getLastLocation(
                    googleApiClient);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public GpsLookupTask(Context context) {
        Log.d(this.getClass().getSimpleName(), "creating GpsLookupTask");
        this.context = context;
        this.dao = new Dao(context);
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(60 * 1000)
                .setFastestInterval(1 * 1000);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Log.d(this.getClass().getSimpleName(), "starting background job");
        while (!isCancelled()) {
            Log.d(this.getClass().getSimpleName(), "Repeating background job execution");
            if (currentLocation != null && dao != null) {
                Cursor allSavedLocations = dao.getAllLocations();
                if (allSavedLocations.moveToFirst()) {
                    do {
                        double latitude = allSavedLocations.getDouble(allSavedLocations.getColumnIndex(context.getString(R.string.latitude_column)));
                        double longitude = allSavedLocations.getDouble(allSavedLocations.getColumnIndex(context.getString(R.string.longitude_column)));
                        int radius = allSavedLocations.getInt(allSavedLocations.getColumnIndex(context.getString(R.string.radius)));
                        double altitude = allSavedLocations.getDouble(allSavedLocations.getColumnIndex(context.getString(R.string.altitude_column)));
                        Long id = allSavedLocations.getLong(allSavedLocations.getColumnIndex("_id"));
                        Location savedLocation = new Location("");
                        savedLocation.setLatitude(latitude);
                        savedLocation.setLongitude(longitude);
                        savedLocation.setAltitude(altitude);

                        float distance = currentLocation.distanceTo(savedLocation);
                        Log.d(this.getClass().getSimpleName(), "computed distance = " + distance);
                        if (distance < radius) {
                            crossedZones.put(id, distance);
                        }
                    } while (allSavedLocations.moveToNext());

                    long nearestLocationId = -1;
                    Float minDistance = null;
                    for (Long id : crossedZones.keySet()) {
                        if (minDistance == null) {
                            minDistance = crossedZones.get(id);
                            nearestLocationId = id;
                        } else if (minDistance >= Math.min(minDistance, crossedZones.get(id))){
                            nearestLocationId = id;
                            minDistance = Math.min(minDistance, crossedZones.get(id));
                        }
                    }
                    Log.d(this.getClass().getSimpleName(), "*************************************************");
                    Log.d(this.getClass().getSimpleName(), "Found nearest location with id=" + nearestLocationId+ "!!! Distance = " + minDistance);
                    Log.d(this.getClass().getSimpleName(), "*************************************************");
                    publishProgress(nearestLocationId);
                }
            } else if (currentLocation == null) {
                rememberLastKnownLocation();
            }
            try {
                Log.d(this.getClass().getSimpleName(), "going to sleep for 60 sec");
                Thread.sleep(60 * 1_000);
            } catch (InterruptedException e) {
                Log.e(this.getClass().getSimpleName(), "Caught InterruptedException during doInBackground method", e);
            }
            Log.d(this.getClass().getSimpleName(), "waking up");
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Long... values) {

        makeAudioServiceCall(values[0]);
        super.onProgressUpdate(values);
    }

    private void makeAudioServiceCall(long locationId) {
        Cursor c = dao.getLocationById(locationId);
        if (c.moveToFirst()) {
            MyMediaService ms = MyMediaService.getInstance();
            AudioPreferences p = new AudioPreferences();
            p.setPreferenceId(c.getLong(c.getColumnIndex("_id")));
            p.setRingtoneVolume(c.getFloat(c.getColumnIndex(context.getString(R.string.ringtone_volume_column))));
            p.setMusicVolume(c.getFloat(c.getColumnIndex(context.getString(R.string.music_volume))));
            p.setNotificationVolume(c.getFloat(c.getColumnIndex(context.getString(R.string.notification_volume))));
            p.setVibro(c.getInt(c.getColumnIndex(context.getString(R.string.vibration))) != 0);
            ms.adjustAudio(p, context);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Log.d(this.getClass().getSimpleName(), "Stopping GpsLookupTask execution, disconnecting Google");
        super.onPostExecute(aVoid);
        stopLocationUpdates();
        googleApiClient.disconnect();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(this.getClass().getSimpleName(), "location has been changed, updating last known location variable");
        currentLocation = location;
        Log.d(this.getClass().getSimpleName(), currentLocation.toString());
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.d(this.getClass().getSimpleName(), "Cancelling task, disconnecting Google");
        stopLocationUpdates();
        googleApiClient.disconnect();
    }
}