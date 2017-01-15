package ru.inventions.tolerantus.locaset.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Dao;

/**
 * Created by Aleksandr on 09.01.2017.
 */

public class GpsLookupTask extends AsyncTask<Void, Float, Void> implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private Dao dao;
    private GoogleApiClient googleApiClient;
    private Location lastKnownLocation;
    private LocationRequest mLocationRequest;
    private Context context;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Here, thisActivity is the current activity
        Log.d(this.getClass().getSimpleName(), "Google connected");
        updateLastKnownLocation();
    }

    private void updateLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            Log.d(this.getClass().getSimpleName(), "updateLastKnownLocation invocation");
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
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Log.d(this.getClass().getSimpleName(), "starting background job");
        while (!isCancelled()) {
            Log.d(this.getClass().getSimpleName(), "Repeating background job execution");
            if (lastKnownLocation != null && dao != null) {
                Cursor allSavedLocations = dao.getAllLocations();
                if (allSavedLocations.moveToFirst()) {
                    do {
                        double latitude = allSavedLocations.getDouble(allSavedLocations.getColumnIndex(context.getString(R.string.latitude_column)));
                        double longitude = allSavedLocations.getDouble(allSavedLocations.getColumnIndex(context.getString(R.string.longitude_column)));

                        Location savedLocation = new Location("");
                        savedLocation.setLatitude(latitude);
                        savedLocation.setLongitude(longitude);
                        savedLocation.setAltitude(0);

                        float distance = lastKnownLocation.distanceTo(savedLocation);
                        Log.d(this.getClass().getSimpleName(), "computed distance = " + distance);
                        if (distance < context.getResources().getInteger(R.integer.location_radius_in_meters)) {
                            publishProgress(distance);
                        }
                    } while (allSavedLocations.moveToNext());
                }
            }
            try {
                Log.d(this.getClass().getSimpleName(), "going to sleep for 60 sec");
                Thread.sleep(60 * 1_000);
            } catch (InterruptedException e) {
                Log.e(this.getClass().getSimpleName(), "Caught InterruptedException during doInBackground method", e);
            }
            Log.d(this.getClass().getSimpleName(), "waking up");
        }
        Log.d(this.getClass().getSimpleName(), this.getClass().getSimpleName() + " was cancelled");
        return null;
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        Log.d(this.getClass().getSimpleName(), "***********************************");
        Log.d(this.getClass().getSimpleName(), "Found nearest location!!! Distance = " + values[0]);
        Log.d(this.getClass().getSimpleName(), "***********************************");
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Log.d(this.getClass().getSimpleName(), "Stopping GpsLookupTask execution, disconnecting Google");
        super.onPostExecute(aVoid);
        googleApiClient.disconnect();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(this.getClass().getSimpleName(), "onLocationChanged invocation");
        if (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(
                    googleApiClient);
            Log.d(this.getClass().getSimpleName(), lastKnownLocation.toString());
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.d(this.getClass().getSimpleName(), "Cancelling task, disconnecting Google");
        googleApiClient.disconnect();
    }
}
