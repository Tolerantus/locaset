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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.misc.TransactionManager;

import org.json.JSONObject;

import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Location;
import ru.inventions.tolerantus.locaset.db.OrmDbOpenHelper;
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

    private Location location;
    private boolean markerChanged;
    private Activity activityInvoker;
    private Intent afterSavingIntent;

    public CoordinatesSavingTask(Activity activityInvoker, Intent afterSavingIntent, Location location, boolean changed) {
        this.activityInvoker = activityInvoker;
        this.afterSavingIntent = afterSavingIntent;
        this.location = location;
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
            location.setAltitude(0);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        debug("CoordinatesSavingTask finished her work");
        activityInvoker.findViewById(R.id.pb_loading).setVisibility(View.INVISIBLE);
        final OrmDbOpenHelper ormDbOpenHelper = OpenHelperManager.getHelper(activityInvoker, OrmDbOpenHelper.class);
        try {
            TransactionManager.callInTransaction(ormDbOpenHelper.getConnectionSource(), new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    debug("coordinates saving transaction started");
                    ormDbOpenHelper.getDao().update(location);
                    debug("coordinates saving transaction finished");
                    return null;
                }
            });
            debug("location with id=" + location.getId() + " has been saved");
        } catch (SQLException e) {
            error("error during updating location: " + e.getMessage());
        }
        if (MyMediaService.currentPreferenceId.get() == location.getId()) {
            MyMediaService.currentPreferenceId.set(-1);
        }
        if (afterSavingIntent != null) {
            activityInvoker.startActivity(afterSavingIntent);
            debug("starting activity " + afterSavingIntent.getAction());
        }
        super.onPostExecute(aVoid);
    }

    private void makeElevationRequest() {
        RequestQueue queue = Volley.newRequestQueue(activityInvoker);
        RequestFuture requestFuture = RequestFuture.newFuture();
        Double latitude = location.getLatitude();
        Double longitude = location.getLongitude();
        if (latitude != null && longitude != null) {
            String url = String.format(REQUEST, latitude, longitude);
            debug("sending elevation request:\n" + url);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    requestFuture, requestFuture);
            queue.add(stringRequest);
            try {
                String rawResponse = requestFuture.get(5, TimeUnit.SECONDS).toString();
                debug("Response has been received, starting parsing");
                double alt = ParsingUtils.parseXmlResponseElevation(rawResponse, activityInvoker);
                debug("computed altitude=" + alt);
                location.setAltitude(alt);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                error("requesting elevation failed: " + e.getMessage());
                error("Error occurred during elevation calculation! ");
                location.setAltitude(0);
            }
        }
    }
}
