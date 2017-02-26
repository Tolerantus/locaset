package ru.inventions.tolerantus.locaset.async;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.OrmDbOpenHelper;
import ru.inventions.tolerantus.locaset.service.media.AudioPreferences;
import ru.inventions.tolerantus.locaset.service.media.MyMediaService;

import static android.content.Context.MODE_PRIVATE;
import static ru.inventions.tolerantus.locaset.util.LogUtils.debug;
import static ru.inventions.tolerantus.locaset.util.LogUtils.error;

/**
 * Created by Aleksandr on 09.01.2017.
 */

public class GpsLookupTask extends AsyncTask<Void, ru.inventions.tolerantus.locaset.db.Location, Void> implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private OrmDbOpenHelper ormDbOpenHelper;
    private GoogleApiClient googleApiClient;
    private Location currentLocation;
    private LocationRequest mLocationRequest;
    private Context context;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    private Map<Long, Double> crossedZones = new HashMap<>();

    public GpsLookupTask(Context context) {
        debug("creating GpsLookupTask");
        this.context = context;
        ormDbOpenHelper = OpenHelperManager.getHelper(context, OrmDbOpenHelper.class);
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
                .setFastestInterval(10 * 1000);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        debug("Google connected");
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            debug("starting location lookup");
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
        }
    }

    private void stopLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            debug("stopping location lookup");
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    private void rememberLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            debug("updating last know location by demand");
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

    @Override
    protected Void doInBackground(Void... voids) {
        debug("starting background job");
        while (!isCancelled()) {
            debug("Repeating background job execution");
            if (currentLocation != null) {
                final List<ru.inventions.tolerantus.locaset.db.Location> locations = new ArrayList<>();
                try {
                    TransactionManager.callInTransaction(ormDbOpenHelper.getConnectionSource(), new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            locations.addAll(ormDbOpenHelper.getDao().queryForAll());
                            return null;
                        }
                    });
                } catch (SQLException e) {
                    error("error during reading locations:" + e.getMessage());
                }
                for (ru.inventions.tolerantus.locaset.db.Location location : locations) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    double radius = location.getRadius();
                    double altitude = location.getAltitude();
                    Location googleLocation = new Location("");
                    googleLocation.setLatitude(latitude);
                    googleLocation.setLongitude(longitude);
                    googleLocation.setAltitude(altitude);

                    float distance = currentLocation.distanceTo(googleLocation);
                    debug("computed distance = " + distance);
                    if (distance < radius) {
                        crossedZones.put(location.getId(), (double) distance);
                    }
                }
//              default location id
                long nearestLocationId = -1;
                Double minDistance = null;
                for (Long locationId : crossedZones.keySet()) {
                    if (minDistance == null) {
                        minDistance = crossedZones.get(locationId);
                        nearestLocationId = locationId;
                    } else if (minDistance >= Math.min(minDistance, crossedZones.get(locationId))) {
                        nearestLocationId = locationId;
                        minDistance = Math.min(minDistance, crossedZones.get(locationId));
                    }
                }
                ru.inventions.tolerantus.locaset.db.Location nearestLocation = null;
                for (ru.inventions.tolerantus.locaset.db.Location location : locations) {
                    if (location.getId() == nearestLocationId) {
                        nearestLocation = location;
                        break;
                    }
                }
                debug("**************************************************************************************************");
                if (nearestLocation != null) {
                    debug("Found nearest location with id=" + nearestLocationId + "!!! Distance = " + minDistance);
                } else {
                    debug("No one active location has been found");
                }
                debug("**************************************************************************************************");
                publishProgress(nearestLocation);
            }
            try {
                SharedPreferences sharedPreferences = context.getSharedPreferences("global", MODE_PRIVATE);
                int seconds = sharedPreferences.getInt("gps_lookup_cycle", 60);
                seconds = seconds > 10 ? seconds : 10;
                debug("going to sleep for " + seconds + " secs");
                Thread.sleep(seconds * 1_000);
            } catch (InterruptedException e) {
                error("Caught InterruptedException during doInBackground method: " + e.getMessage());
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(ru.inventions.tolerantus.locaset.db.Location... values) {
        if (values.length > 0) {
            makeAudioServiceCall(values[0]);
        } else {
            makeAudioServiceCall(null);
        }
        super.onProgressUpdate(values);
    }

    private void makeAudioServiceCall(ru.inventions.tolerantus.locaset.db.Location location) {
        MyMediaService ms = MyMediaService.getInstance();
        AudioPreferences p = new AudioPreferences();
        if (location != null) {
            p.setPreferenceId(location.getId());
            p.setLocationName(location.getName());
            p.setRingtoneVolume(location.getRingtoneVol());
            p.setMusicVolume(location.getMusicVol());
            p.setNotificationVolume(location.getNotificationVol());
            p.setVibro(location.isVibro());
        } else {
            SharedPreferences preferences = context.getSharedPreferences("global", MODE_PRIVATE);
            p.setPreferenceId(-1);
            p.setLocationName("Somewhere in open space...");
            p.setRingtoneVolume(preferences.getFloat("ringtone", 0));
            p.setMusicVolume(preferences.getFloat("music", 0));
            p.setNotificationVolume(preferences.getFloat("notification", 0));
            p.setVibro(preferences.getBoolean("vibro", false));
        }
        ms.adjustAudio(p, context);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        debug("Stopping GpsLookupTask execution, disconnecting Google");
        super.onPostExecute(aVoid);
        terminateImportantServices();
    }

    @Override
    public void onLocationChanged(Location location) {
        debug("location has been changed, updating last known location variable");
        currentLocation = location;
        debug(currentLocation.toString());
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        debug("Cancelling task, disconnecting Google");
        terminateImportantServices();
    }

    private void terminateImportantServices() {
        stopLocationUpdates();
        if (ormDbOpenHelper != null) {
            OpenHelperManager.releaseHelper();
            ormDbOpenHelper = null;
        }
        googleApiClient.disconnect();
        MyMediaService.isCurrentPreferenceValid.set(false);
    }
}
