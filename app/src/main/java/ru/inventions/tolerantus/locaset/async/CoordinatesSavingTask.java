package ru.inventions.tolerantus.locaset.async;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Dao;
import ru.inventions.tolerantus.locaset.service.media.MyMediaService;
import ru.inventions.tolerantus.locaset.util.NetworkUtils;
import ru.inventions.tolerantus.locaset.util.ParsingUtils;

import static ru.inventions.tolerantus.locaset.util.LogUtils.debug;
import static ru.inventions.tolerantus.locaset.util.LogUtils.error;

/**
 * Created by Aleksandr on 13.01.2017.
 */

public class CoordinatesSavingTask extends AsyncTask<Void, Double, Void> {

    private final static String REQUEST = "http://maps.googleapis.com/maps/api/elevation/xml?locations=%1$s,%2$s&sensor=true";

    private long locationId;
    private boolean markerChanged;
    private Activity activityInvoker;
    private Intent afterSavingIntent;
    private ContentValues contentForSaving;

    public CoordinatesSavingTask(Activity activityInvoker, Intent afterSavingIntent, long locationId, ContentValues contentForSaving, boolean changed) {
        this.activityInvoker = activityInvoker;
        this.afterSavingIntent = afterSavingIntent;
        this.locationId = locationId;
        this.contentForSaving = contentForSaving;
        this.markerChanged = changed;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        activityInvoker.findViewById(R.id.pb_loading).setVisibility(View.VISIBLE);
    }

    @Override
    protected Void doInBackground(Void... doubles) {
        debug("starting elevation search task");
        if (NetworkUtils.isAnyNetwork(activityInvoker)) {
            debug("network is available, marker has been changed, sending elevation request");
            makeElevationRequest();
        } else {
            error("network hasn't been found, skipping elevation request");
            putReceivedDataInCv(0);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Double... values) {
        super.onProgressUpdate(values);
        new Dao(activityInvoker).updateLocation(locationId, contentForSaving);
        if (MyMediaService.currentPreferenceId.get() == locationId) {
            MyMediaService.currentPreferenceId.set(-1);
        }
        if (afterSavingIntent != null) {
            activityInvoker.startActivity(afterSavingIntent);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        debug("CoordinatesSavingTask finished her work");
        activityInvoker.findViewById(R.id.pb_loading).setVisibility(View.INVISIBLE);
        super.onPostExecute(aVoid);
    }

    private void makeElevationRequest() {
        RequestQueue queue = Volley.newRequestQueue(activityInvoker);
        Double latitude = contentForSaving.getAsDouble(activityInvoker.getString(R.string.latitude_column));
        Double longitude = contentForSaving.getAsDouble(activityInvoker.getString(R.string.longitude_column));
        if (latitude != null && longitude != null) {
            String url = String.format(REQUEST, latitude, longitude);
            debug("sending elevation request:\n" + url);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            debug("Response was received, starting parsing");
                            double alt = ParsingUtils.parseXmlResponseElevation(response, activityInvoker);
                            debug("computed altitude=" + alt);
                            putReceivedDataInCv(alt);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error("Error occurred during elevation calculation! ");
                    putReceivedDataInCv(0);
                }
            });
            queue.add(stringRequest);
        }
    }

    private void putReceivedDataInCv(double alt) {
        contentForSaving.put(activityInvoker.getString(R.string.altitude_column), alt);
        contentForSaving.put(activityInvoker.getString(R.string.address), "");
        publishProgress();
    }
}
