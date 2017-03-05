package ru.inventions.tolerantus.locaset.service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.misc.TransactionManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import ru.inventions.tolerantus.locaset.db.OrmDbOpenHelper;
import ru.inventions.tolerantus.locaset.service.media.AudioPreferences;
import ru.inventions.tolerantus.locaset.service.media.MyMediaService;
import ru.inventions.tolerantus.locaset.util.AddressUtils;

import static android.content.Context.MODE_PRIVATE;
import static ru.inventions.tolerantus.locaset.service.MyAlarmService.REQUEST_CODE;
import static ru.inventions.tolerantus.locaset.service.ReceiverActionEnum.PLAN;
import static ru.inventions.tolerantus.locaset.util.LogUtils.debug;
import static ru.inventions.tolerantus.locaset.util.LogUtils.error;
import static ru.inventions.tolerantus.locaset.util.LogUtils.warn;

/**
 * Created by Aleksandr on 04.03.2017.
 */

public class Receiver extends BroadcastReceiver implements LocationListener, GoogleApiClient.ConnectionCallbacks {
    public static final int MIN_TIME_REQUEST = 5 * 1000;
    public static final String ACTION_REFRESH_SCHEDULE_ALARM =
            "org.mabna.order.ACTION_REFRESH_SCHEDULE_ALARM";
    public static int numberOfCalls;
    public static Date lastExecutionDate;
    public static String lastDeterminedAddress;
    private static GoogleApiClient googleApiClient;
    private Context _context;
    private LocationRequest _request;
    private Location _location;

    // received _request from the calling service
    @Override
    public void onReceive(final Context context, Intent intent) {
        ReceiverActionEnum action = ReceiverActionEnum.from(intent.getAction());
        switch (action) {
            case PLAN:
                warn("received request");
                MyAlarmService.isAlarmSet = true;
                numberOfCalls++;
                lastExecutionDate = new Date();
                _context = context;
                _request = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(60 * 1000).setFastestInterval(10 * 1000);
                if (googleApiClient == null) {
                    googleApiClient = new GoogleApiClient.Builder(context)
                            .addApi(LocationServices.API)
                            .addConnectionCallbacks(this)
                            .build();
                    debug("google api client was null, initialized");
                }

                if (!googleApiClient.isConnected()) {
                    googleApiClient.connect();
                    debug("google api client was disconnected, now connected");
                }

                setNextAlarm();

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    _location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                    if (_location != null) {
                        lastDeterminedAddress = AddressUtils.getStringAddress(
                                _location.getLatitude(),
                                _location.getLongitude(),
                                new Geocoder(context)
                        );
                    }
                    OrmDbOpenHelper ormDbOpenHelper = OpenHelperManager.getHelper(context, OrmDbOpenHelper.class);
                    processReceivedLocation(_location, ormDbOpenHelper);
                }
                break;
            case CANCEL :
                debug("received cancelAllNotifications request");
                MyAlarmService.isAlarmSet = false;
                terminateServices();
                break;
        }

    }

    private void processReceivedLocation(Location currentLocation, final OrmDbOpenHelper ormDbOpenHelper) {
        Map<Long, Double> crossedZones = new HashMap<>();
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
                debug("Found nearest location with id=" + nearestLocationId + "!!! Distance = " + minDistance);
                makeAudioServiceCall(nearestLocation);
            debug("**************************************************************************************************");
        }
    }

    private void makeAudioServiceCall(ru.inventions.tolerantus.locaset.db.Location location) {
        debug("adjusting audio");
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
            SharedPreferences preferences = _context.getSharedPreferences("global", MODE_PRIVATE);
            p.setPreferenceId(-1);
            p.setLocationName("Somewhere in open space...");
            p.setRingtoneVolume(preferences.getFloat("ringtone", 0));
            p.setMusicVolume(preferences.getFloat("music", 0));
            p.setNotificationVolume(preferences.getFloat("notification", 0));
            p.setVibro(preferences.getBoolean("vibro", false));
        }
        ms.adjustAudio(p, _context);
    }

    private void setNextAlarm() {
        warn("setting next alarm");
        SharedPreferences preferences = _context.getSharedPreferences("global", Context.MODE_PRIVATE);
        int minutes = preferences.getInt("gps_lookup_cycle", 10);
        minutes = minutes > 1 ? minutes : 1;
        debug("replanning alarms with " + minutes + " minutes cycle");
        AlarmManager alarmManager = ((AlarmManager) _context.getSystemService(Context.ALARM_SERVICE));
        Intent intent = new Intent(_context, Receiver.class);
        intent.setAction(PLAN.toString());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(_context, REQUEST_CODE, intent, 0);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis() + minutes * 60 * 1000, pendingIntent);
    }

    private void terminateServices() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        debug("location changed");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, _request, this);
            debug("requested updates");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }
}
