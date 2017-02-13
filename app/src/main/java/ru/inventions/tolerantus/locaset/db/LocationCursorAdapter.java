package ru.inventions.tolerantus.locaset.db;

import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import ru.inventions.tolerantus.locaset.R;

/**
 * Created by Aleksandr on 06.01.2017.
 */

public class LocationCursorAdapter extends CursorAdapter {
    private LayoutInflater cursorInflater;

    // Default constructor
    public LocationCursorAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        cursorInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    }

    public void bindView(View view, Context context, Cursor cursor) {
        TextView tv_name = (TextView) view.findViewById(R.id.tv_location_name);
        TextView tv_city = (TextView) view.findViewById(R.id.address);

        String name = cursor.getString(cursor.getColumnIndex(context.getString(R.string.location_name_column)));
        String address = cursor.getString(cursor.getColumnIndex(context.getString(R.string.address)));

        tv_name.setText(name);
        if (address == null || address.isEmpty()) {
            tv_city.setText("...");
        } else {
            tv_city.setText(address);
        }
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // R.layout.list_row is your xml layout for each row
        return cursorInflater.inflate(R.layout.activity_main_list_item, parent, false);
    }
}
