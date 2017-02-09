package ru.inventions.tolerantus.locaset.async;

import android.content.Context;
import android.database.Cursor;
import android.location.Geocoder;
import android.os.AsyncTask;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Dao;
import ru.inventions.tolerantus.locaset.util.AddressUtils;
import ru.inventions.tolerantus.locaset.util.CvBuilder;

/**
 * Created by Aleksandr on 09.02.2017.
 */

public class AddressRefreshTask extends AsyncTask<Void, Void, Void> {

    private Context context;

    public AddressRefreshTask(Context context) {
        this.context = context;
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
        Dao dao = new Dao(context);
        Map<Long, String> locationToUpdate = new HashMap<>();
        Cursor c = dao.getAllLocations();
        if (c.moveToFirst()) {
            do {
                String address = c.getString(c.getColumnIndex(context.getString(R.string.address)));
                if (address == null || address.isEmpty()) {
                    double latitude = c.getDouble(c.getColumnIndex(context.getString(R.string.latitude_column)));
                    double longitude = c.getDouble(c.getColumnIndex(context.getString(R.string.longitude_column)));
                    String refreshedAddress = AddressUtils.getStringAddress(latitude, longitude, new Geocoder(context, Locale.getDefault()));
                    if (refreshedAddress != null && !refreshedAddress.isEmpty()) {
                        locationToUpdate.put(c.getLong(c.getColumnIndex("_id")), refreshedAddress);
                    }
                }
            } while (c.moveToNext());
        }
        for (Long id : locationToUpdate.keySet()) {
            dao.updateLocation(id, CvBuilder.create().append(context.getString(R.string.address), locationToUpdate.get(id)).get());
        }
        return null;
    }
}
