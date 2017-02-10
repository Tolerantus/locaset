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
import com.google.android.gms.maps.model.Marker;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Dao;
import ru.inventions.tolerantus.locaset.service.media.MyMediaService;
import ru.inventions.tolerantus.locaset.util.CvBuilder;
import ru.inventions.tolerantus.locaset.util.NetworkUtils;
import ru.inventions.tolerantus.locaset.util.ParsingUtils;

/**
 * Created by Aleksandr on 13.01.2017.
 */

public class CoordinatesSavingTask extends AsyncTask<Void, Double, Void> {

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
        if (markerChanged && NetworkUtils.isAnyNetwork(activityInvoker)) {
            contentForSaving.put(activityInvoker.getString(R.string.address), "");
            makeElevationRequest();
        } else {
            publishProgress();
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
        Log.d(this.getClass().getSimpleName(), "CoordinatesSavingTask finished her work");
        activityInvoker.findViewById(R.id.pb_loading).setVisibility(View.INVISIBLE);
        super.onPostExecute(aVoid);
    }

    private void makeElevationRequest() {
        RequestQueue queue = Volley.newRequestQueue(activityInvoker);
        Double latitude = contentForSaving.getAsDouble(activityInvoker.getString(R.string.latitude_column));
        Double longitude = contentForSaving.getAsDouble(activityInvoker.getString(R.string.longitude_column));
        Double altitude = contentForSaving.getAsDouble(activityInvoker.getString(R.string.altitude_column));
        if (latitude != null && longitude != null && altitude == null) {
            String url = "http://maps.googleapis.com/maps/api/elevation/" + "xml?locations="
                    + latitude + "," + longitude + "&sensor=true";
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(this.getClass().getSimpleName(), "Response was received, starting parsing");
                            contentForSaving.put(activityInvoker.getString(R.string.altitude_column), ParsingUtils.parseXmlResponseElevation(response, activityInvoker));
                            contentForSaving.put(activityInvoker.getString(R.string.address), "");
                            publishProgress();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(this.getClass().getSimpleName(), "Error occurred during elevation calculation! ", error);
                    contentForSaving.put(activityInvoker.getString(R.string.altitude_column), 0d);
                    contentForSaving.put(activityInvoker.getString(R.string.address), "");
                    publishProgress();
                }
            });
            queue.add(stringRequest);
        }
    }
}
