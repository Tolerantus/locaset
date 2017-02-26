package ru.inventions.tolerantus.locaset.async;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;

import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.adapter.LocationsListAdapter;
import ru.inventions.tolerantus.locaset.db.Location;
import ru.inventions.tolerantus.locaset.db.OrmDbOpenHelper;
import ru.inventions.tolerantus.locaset.service.media.MyMediaService;
import ru.inventions.tolerantus.locaset.util.AddressUtils;
import ru.inventions.tolerantus.locaset.util.LocationActionEnum;
import ru.inventions.tolerantus.locaset.util.NetworkUtils;
import ru.inventions.tolerantus.locaset.util.ParsingUtils;

import static ru.inventions.tolerantus.locaset.util.LogUtils.debug;
import static ru.inventions.tolerantus.locaset.util.LogUtils.error;
import static ru.inventions.tolerantus.locaset.util.LogUtils.warn;

/**
 * Created by Aleksandr on 25.02.2017.
 */

public class LocationUpdateTask extends AsyncTask<Void, Void, Void> {

    private final static String REQUEST = "http://maps.googleapis.com/maps/api/elevation/xml?locations=%1$s,%2$s&sensor=true";

    private Location updatedLocation;
    private LocationActionEnum action;
    private Activity activityInvoker;
    private Intent afterSavingIntent;
    private LocationsListAdapter adapter;
    private OrmDbOpenHelper ormDbOpenHelper;

    public LocationUpdateTask(Location updatedLocation,
                              LocationActionEnum action,
                              Activity activityInvoker,
                              Intent afterSavingIntent,
                              LocationsListAdapter adapter) {
        this.updatedLocation = updatedLocation;
        this.action = action;
        this.activityInvoker = activityInvoker;
        this.afterSavingIntent = afterSavingIntent;
        this.adapter = adapter;
        ormDbOpenHelper = OpenHelperManager.getHelper(activityInvoker, OrmDbOpenHelper.class);
    }

    @Override
    protected Void doInBackground(Void... params) {
        debug("starting location update task");
        try {
            final Dao<Location, Long> dao = ormDbOpenHelper.getDao();
            if (updatedLocation != null) {
                debug("found location for update");
                if (NetworkUtils.isAnyNetwork(activityInvoker)) {
                    debug("network is available, start location updating");
                    makeElevationRequest();
                }
                TransactionManager.callInTransaction(ormDbOpenHelper.getConnectionSource(), new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        debug("======================================");
                        debug("location update transaction started");
                        int affectedRows = 0;
                        switch (action) {
                            case UPDATE:
                                affectedRows = dao.update(updatedLocation);
                                break;
                            case DELETE:
                                affectedRows = dao.delete(updatedLocation);
                                break;
                        }
                        if (affectedRows > 0) {
                            debug("location has been successfully updated");
                        } else {
                            warn("no one location has been updated");
                        }
                        debug("location update transaction finished");
                        debug("======================================");
                        return null;
                    }
                });
                debug("location with id=" + updatedLocation.getId() + " has been updated");
                debug("resetting audio preference");
                if (MyMediaService.currentPreferenceId.get() == updatedLocation.getId()) {
                    MyMediaService.isCurrentPreferenceValid.set(false);
                }
            } else {
                warn("no location has been given for update");
            }
            if (NetworkUtils.isAnyNetwork(activityInvoker)) {
                debug("network is available, start address updating");
                TransactionManager.callInTransaction(
                        ormDbOpenHelper.getConnectionSource(),
                        new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                debug("======================================");
                                debug("address refreshing transaction started");
                                for (Location location : dao.queryForAll()) {
                                    String refreshedAddress = AddressUtils
                                            .getStringAddress(
                                                    location.getLatitude(),
                                                    location.getLongitude(),
                                                    new Geocoder(activityInvoker, Locale.getDefault()));
                                    location.setAddress(refreshedAddress);
                                    dao.update(location);
                                }
                                debug("address refreshing transaction finished");
                                debug("======================================");
                                return null;
                            }
                        });
            } else {
                warn("no network were found for addresses update, skipping");
            }
        } catch (SQLException e) {
            error("error during updating location:" + e.getMessage());
        }
        return null;
    }

    private void makeElevationRequest() {
        RequestQueue queue = Volley.newRequestQueue(activityInvoker);
        RequestFuture requestFuture = RequestFuture.newFuture();
        Double latitude = updatedLocation.getLatitude();
        Double longitude = updatedLocation.getLongitude();
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
            updatedLocation.setAltitude(alt);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            error("requesting elevation failed: " + e.getMessage());
            updatedLocation.setAltitude(0);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        SwipeRefreshLayout swipeRefreshLayout = ((SwipeRefreshLayout) activityInvoker.findViewById(R.id.refresh_layout));
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (adapter != null) {
            adapter.getLocations().clear();
            try {
                adapter.getLocations().addAll(ormDbOpenHelper.getDao().queryForAll());
            } catch (SQLException e) {
                error("reading locations failed: " + e.getMessage());
            }
            adapter.notifyDataSetChanged();
        }

        if (afterSavingIntent != null) {
            activityInvoker.startActivity(afterSavingIntent);
            debug("starting activity " + afterSavingIntent.getAction());
        }
        if (ormDbOpenHelper != null) {
            OpenHelperManager.releaseHelper();
            ormDbOpenHelper = null;
        }

        super.onPostExecute(aVoid);
    }
}
