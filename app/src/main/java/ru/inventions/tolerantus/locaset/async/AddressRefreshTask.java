package ru.inventions.tolerantus.locaset.async;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.widget.CursorAdapter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Dao;
import ru.inventions.tolerantus.locaset.util.AddressUtils;
import ru.inventions.tolerantus.locaset.util.CvBuilder;
import ru.inventions.tolerantus.locaset.util.LogUtils;
import ru.inventions.tolerantus.locaset.util.NetworkUtils;

import static ru.inventions.tolerantus.locaset.util.LogUtils.debug;
import static ru.inventions.tolerantus.locaset.util.LogUtils.error;

/**
 * Created by Aleksandr on 09.02.2017.
 */

public class AddressRefreshTask extends AsyncTask<Void, Void, Void> {

    private static final String tag = "AddressRefreshTask";

    private Context context;

    private CursorAdapter adapter;

    public AddressRefreshTask(Context context, CursorAdapter adapter) {
        this.context = context;
        this.adapter = adapter;
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute}
     * by the caller of this task.
     * <p>
     * This method can call {@link #publishProgress} to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @Override
    protected Void doInBackground(Void... params) {
        if (!NetworkUtils.isAnyNetwork(context)) {
            error("network wasn't found");
            publishProgress();
            return null;
        }
        debug("Starting address refreshing task");
        Dao dao = new Dao(context);
        Map<Long, String> locationToUpdate = new HashMap<>();
        Cursor c = dao.getAllLocations();
        if (c.moveToFirst()) {
            do {
                double latitude = c.getDouble(c.getColumnIndex(context.getString(R.string.latitude_column)));
                double longitude = c.getDouble(c.getColumnIndex(context.getString(R.string.longitude_column)));
                String refreshedAddress = AddressUtils.getStringAddress(latitude, longitude, new Geocoder(context, Locale.getDefault()));
                locationToUpdate.put(c.getLong(c.getColumnIndex("_id")), refreshedAddress);
            } while (c.moveToNext());
        }
        if (!locationToUpdate.keySet().isEmpty()) {
            debug("Found " + locationToUpdate.size() + " locations for update");
        }
        for (Long id : locationToUpdate.keySet()) {
            dao.updateLocation(id, CvBuilder.create().append(context.getString(R.string.address), locationToUpdate.get(id)).get());
        }
        publishProgress();
        return null;
    }

    /**
     * Runs on the UI thread after {@link #publishProgress} is invoked.
     * The specified values are the values passed to {@link #publishProgress}.
     *
     * @param values The values indicating progress.
     * @see #publishProgress
     * @see #doInBackground
     */
    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        if (context instanceof Activity) {
            SwipeRefreshLayout swipeRefreshLayout = ((SwipeRefreshLayout) ((Activity) context).findViewById(R.id.refresh_layout));
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            if (adapter != null) {
                debug("changing cursor for swipeRefreshLayout");
                adapter.changeCursor(new Dao(context).getAllLocations());
            }
        }
    }
}
