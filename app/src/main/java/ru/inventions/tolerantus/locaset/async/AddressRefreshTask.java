package ru.inventions.tolerantus.locaset.async;

import android.app.Activity;
import android.content.Context;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;

import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.Callable;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.adapter.LocationsListAdapter;
import ru.inventions.tolerantus.locaset.db.Location;
import ru.inventions.tolerantus.locaset.db.OrmDbOpenHelper;
import ru.inventions.tolerantus.locaset.util.AddressUtils;
import ru.inventions.tolerantus.locaset.util.NetworkUtils;

import static ru.inventions.tolerantus.locaset.util.LogUtils.debug;
import static ru.inventions.tolerantus.locaset.util.LogUtils.error;

/**
 * Created by Aleksandr on 09.02.2017.
 */

public class AddressRefreshTask extends AsyncTask<Void, Void, Void> {

    private Context context;

    private LocationsListAdapter adapter;

    private OrmDbOpenHelper ormDbOpenHelper;

    public AddressRefreshTask(Context context, LocationsListAdapter adapter) {
        this.context = context;
        this.adapter = adapter;
        ormDbOpenHelper = OpenHelperManager.getHelper(context, OrmDbOpenHelper.class);
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
        try {
            TransactionManager.callInTransaction(ormDbOpenHelper.getConnectionSource(), new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    debug("address refreshing transaction started");
                    Dao<Location, Long> dao = ormDbOpenHelper.getDao();
                    for (Location location : dao.queryForAll()) {
                        String refreshedAddress = AddressUtils.getStringAddress(location.getLatitude(), location.getLongitude(), new Geocoder(context, Locale.getDefault()));
                        location.setAddress(refreshedAddress);
                        dao.update(location);
                    }
                    debug("address refreshing transaction finished");
                    return null;
                }
            });
        } catch (SQLException e) {
            error("error during updating address information:" + e.getMessage());
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
        if (context instanceof FragmentActivity) {
            SwipeRefreshLayout swipeRefreshLayout = ((SwipeRefreshLayout) ((Activity) context).findViewById(R.id.refresh_layout));
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
        }
        if (ormDbOpenHelper != null) {
            OpenHelperManager.releaseHelper();
            ormDbOpenHelper = null;
        }
    }
}
