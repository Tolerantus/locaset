package ru.inventions.tolerantus.locaset.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import ru.inventions.tolerantus.locaset.R;

/**
 * Created by Aleksandr on 06.01.2017.
 */

public class Dao {

    private DBHelper helper;
    private SQLiteDatabase db;
    private Context context;

    public Dao(Context context) {
        helper = new DBHelper(context);
        db = helper.getDB();
        this.context = context;
    }

    public Cursor getAllLocations() {
        return db.query(context.getString(R.string.location_table), null, null, null, null, null, null);
    }

    public void deleteLocationById(long id) {
        db.delete(context.getString(R.string.location_table), "_id = " + id, null);
    }

    public Cursor getLocationById(long id) {
        return db.query(context.getString(R.string.location_table), null, "_id = " + id, null, null, null, null);
    }

    public void updateLocation(long id, String locationName, int radius, double latitude, double longitude, double altitude, double ringtone, double music, double notification, boolean vibro, String address) {
        ContentValues cv = new ContentValues();
        cv.put(context.getString(R.string.location_name_column), locationName);
        cv.put(context.getString(R.string.radius), radius);
        cv.put(context.getString(R.string.latitude_column), latitude);
        cv.put(context.getString(R.string.longitude_column), longitude);
        cv.put(context.getString(R.string.altitude_column), altitude);
        cv.put(context.getString(R.string.ringtone_volume_column), ringtone);
        cv.put(context.getString(R.string.music_volume), music);
        cv.put(context.getString(R.string.notification_volume), notification);
        cv.put(context.getString(R.string.vibration), vibro?1:0);
        cv.put(context.getString(R.string.address), address);
        db.update(context.getString(R.string.location_table), cv, "_id = " + id, null);
    }

    public void updateLocation(long id, String locationName, int radius, double latitude, double longitude, double altitude, double ringtone, double music, double notification, boolean vibro) {
        ContentValues cv = new ContentValues();
        cv.put(context.getString(R.string.location_name_column), locationName);
        cv.put(context.getString(R.string.radius), radius);
        cv.put(context.getString(R.string.latitude_column), latitude);
        cv.put(context.getString(R.string.longitude_column), longitude);
        cv.put(context.getString(R.string.altitude_column), altitude);
        cv.put(context.getString(R.string.ringtone_volume_column), ringtone);
        cv.put(context.getString(R.string.music_volume), music);
        cv.put(context.getString(R.string.notification_volume), notification);
        cv.put(context.getString(R.string.vibration), vibro?1:0);
        db.update(context.getString(R.string.location_table), cv, "_id = " + id, null);
    }

    public void updateLocation(long id, double latitude, double longitude, double altitude, String address) {
        ContentValues cv = new ContentValues();
        cv.put(context.getString(R.string.latitude_column), latitude);
        cv.put(context.getString(R.string.longitude_column), longitude);
        cv.put(context.getString(R.string.altitude_column), altitude);
        cv.put(context.getString(R.string.address), address);
        db.update(context.getString(R.string.location_table), cv, "_id = " + id, null);
    }

    public long createLocation(String locationName, double latitude, double longitude, double altitude) {
        return createLocation(locationName, 50, latitude, longitude, altitude, 0.5, 0.5, 0.5, true);
    }

    private long createLocation(String locationName, int radius, double latitude, double longitude, double altitude, double ringtone, double system, double notification, boolean vibro) {
        ContentValues cv = new ContentValues();
        cv.put(context.getString(R.string.location_name_column), locationName);
        cv.put(context.getString(R.string.radius), radius);
        cv.put(context.getString(R.string.latitude_column), latitude);
        cv.put(context.getString(R.string.longitude_column), longitude);
        cv.put(context.getString(R.string.altitude_column), altitude);
        cv.put(context.getString(R.string.ringtone_volume_column), ringtone);
        cv.put(context.getString(R.string.music_volume), system);
        cv.put(context.getString(R.string.notification_volume), notification);
        cv.put(context.getString(R.string.vibration), vibro?1:0);
        cv.put(context.getString(R.string.address), "");
        return db.insert(context.getString(R.string.location_table), null, cv);
    }

    public DBHelper getHelper() {
        return helper;
    }
}
