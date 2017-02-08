package ru.inventions.tolerantus.locaset.async;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.Marker;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Dao;
import ru.inventions.tolerantus.locaset.util.AddressUtils;
import ru.inventions.tolerantus.locaset.util.CvBuilder;
import ru.inventions.tolerantus.locaset.util.ParsingUtils;

/**
 * Created by Aleksandr on 13.01.2017.
 */

public class LocationPreferencesSavingTask extends AsyncTask<Void, Double, Void> {

    private Dao dao;
    private long locationId;
    private double latitude;
    private double longitude;
    private boolean markerChanged;
    private Activity activityInvoker;
    private Intent afterSavingIntent;

    public LocationPreferencesSavingTask(Activity activityInvoker, Dao dao, long locationId, Marker m, boolean mChanged, Intent afterSavingIntent) {
        this.activityInvoker = activityInvoker;
        this.dao = dao;
        this.locationId = locationId;
        latitude = m.getPosition().latitude;
        longitude = m.getPosition().longitude;
        markerChanged = mChanged;
        this.afterSavingIntent = afterSavingIntent;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... doubles) {
        if (markerChanged) {
            RequestQueue queue = Volley.newRequestQueue(activityInvoker);
            String url = "http://maps.googleapis.com/maps/api/elevation/" + "xml?locations="
                    + latitude + "," + longitude + "&sensor=true";
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(this.getClass().getSimpleName(), "Response was received, starting parsing");
                            publishProgress(ParsingUtils.parseXmlResponseElevation(response, activityInvoker));
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(this.getClass().getSimpleName(), "Error occurred during elevation calculation! ",error);
                    publishProgress(0d);
                }
            });
            queue.add(stringRequest);
        } else {
            publishProgress();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Double... values) {
        super.onProgressUpdate(values);
        if (values.length != 0) {
            String stringAddress = AddressUtils.getStringAddress(latitude, longitude, activityInvoker);
            ContentValues cv = CvBuilder.create()
                    .append(activityInvoker.getString(R.string.latitude_column), latitude)
                    .append(activityInvoker.getString(R.string.longitude_column), longitude)
                    .append(activityInvoker.getString(R.string.altitude_column), values[0])
                    .append(activityInvoker.getString(R.string.address), stringAddress).get();
            dao.updateLocation(locationId, cv);
        }
        if (afterSavingIntent != null) {
            activityInvoker.startActivity(afterSavingIntent);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Log.d(this.getClass().getSimpleName(), "LocationPreferencesSavingTask finished her work");
        super.onPostExecute(aVoid);
    }
}
