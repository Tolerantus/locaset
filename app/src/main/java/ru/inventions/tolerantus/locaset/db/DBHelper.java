package ru.inventions.tolerantus.locaset.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.inventions.tolerantus.locaset.R;

/**
 * Created by Aleksandr on 06.01.2017.
 */

public class DBHelper extends SQLiteOpenHelper {

    private Context context;

    private SQLiteDatabase db;

    public DBHelper(Context context) {
        super(context, context.getString(R.string.db_name), null, context.getResources().getInteger(R.integer.db_version));
        this.context = context;
    }

    public void open() {
        if (db == null || !db.isOpen()) {
            db = getWritableDatabase();
        }
    }

    public SQLiteDatabase getDB() {
        open();
        return db;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table " + context.getString(R.string.location_table)
                + "( _id integer primary key autoincrement, "
                + context.getString(R.string.location_name_column)
                + " text, "
                + context.getString(R.string.ringtone_volume_column)
                + " real, "
                + context.getString(R.string.music_volume)
                + " real, "
                + context.getString(R.string.notification_volume)
                + " real, "
                + context.getString(R.string.vibration)
                + " integer, "
                + context.getString(R.string.radius)
                + " integer, "
                + context.getString(R.string.longitude_column)
                + " real, "
                + context.getString(R.string.latitude_column)
                + " real, "
                + context.getString(R.string.altitude_column)
                + " real, " +
                context.getString(R.string.address) +
                " text)");
        ContentValues cv = new ContentValues();
        cv.put(context.getString(R.string.location_name_column), "test_location");
        cv.put(context.getString(R.string.ringtone_volume_column), 0.5);
        cv.put(context.getString(R.string.notification_volume), 0.5);
        cv.put(context.getString(R.string.music_volume), 0.5);
        cv.put(context.getString(R.string.vibration), 1);
        cv.put(context.getString(R.string.radius), 100);
        cv.put(context.getString(R.string.longitude_column), 30);
        cv.put(context.getString(R.string.latitude_column), 60);
        cv.put(context.getString(R.string.altitude_column), 10);
        cv.put(context.getString(R.string.address), "Default address");

        sqLiteDatabase.insert(context.getString(R.string.location_table), null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
