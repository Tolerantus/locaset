package ru.inventions.tolerantus.locaset.activity;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.async.AddressRefreshTask;
import ru.inventions.tolerantus.locaset.async.MyCachedThreadPoolProvider;
import ru.inventions.tolerantus.locaset.db.Dao;
import ru.inventions.tolerantus.locaset.db.GetAllLocationsCursorLoader;
import ru.inventions.tolerantus.locaset.service.MyGPSService;
import ru.inventions.tolerantus.locaset.db.LocationCursorAdapter;

import static ru.inventions.tolerantus.locaset.util.LogUtils.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    public final static int GET_ALL_LOCATIONS_LOADER_ID = 1;

    private ListView lv;
    private Dao dao;
    private LocationCursorAdapter adapter;
    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        debug("creating main activity");

        setContentView(R.layout.activity_main);
        dao = new Dao(this);
        lv = (ListView) findViewById(R.id.lvMain);
        adapter = new LocationCursorAdapter(this, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        lv.setAdapter(adapter);
        registerForContextMenu(lv);
        findViewById(R.id.bt_add).setOnClickListener(this);
        refreshLayout = ((SwipeRefreshLayout) findViewById(R.id.refresh_layout));
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET},
                1);
        getSupportLoaderManager().initLoader(GET_ALL_LOCATIONS_LOADER_ID, null, this);
    }

    private void refresh() {
        AddressRefreshTask refreshTask = new AddressRefreshTask(MainActivity.this, adapter);
        refreshTask.executeOnExecutor(MyCachedThreadPoolProvider.getInstance());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.main_location_context_menu, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        if (MyGPSService.isServiceOnline()) {
            menu.findItem(R.id.main_opt_service).setIcon(android.R.drawable.ic_media_pause);
        } else {
            menu.findItem(R.id.main_opt_service).setIcon(android.R.drawable.ic_media_play);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_opt_service:
                if (isNotificationPolicyAccessGranted()) {
                    if (!MyGPSService.isServiceOnline()) {
                        Toast.makeText(this, "Starting service", Toast.LENGTH_SHORT).show();
                        startService(new Intent(this, MyGPSService.class));
                        item.setIcon(android.R.drawable.ic_media_pause);
                    } else {
                        Toast.makeText(this, "Stopping service", Toast.LENGTH_SHORT).show();
                        stopService(new Intent(this, MyGPSService.class));
                        item.setIcon(android.R.drawable.ic_media_play);
                    }
                } else {
                    debug("app doesn't have permission for managing notification policies, starting activity to fix this.");
                    Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivity(intent);
                }
                break;
            case R.id.refresh:
                refresh();
                break;
        }
        return true;
    }

    private boolean isNotificationPolicyAccessGranted() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return mNotificationManager.isNotificationPolicyAccessGranted();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.itemDelete:
                dao.deleteLocationById(acmi.id);
                getSupportLoaderManager().getLoader(GET_ALL_LOCATIONS_LOADER_ID).forceLoad();
                break;
            case R.id.itemCustomize:
                startCustomizingLocation(acmi.id);
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void startCustomizingLocation(long id) {
        debug("preparing location customizing activity for location id=" + id);
        Intent settingsIntent = new Intent(this, MapActivity.class);
        settingsIntent.putExtra(getString(R.string.location_id), id);
        startActivity(settingsIntent);
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        getSupportLoaderManager().getLoader(GET_ALL_LOCATIONS_LOADER_ID).forceLoad();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_add:
                addNewLocation();
        }
    }

    private void addNewLocation() {
        long id = dao.createLocation("New location", 60, 30, 0);
        debug("added location with id=" + id);
        if (id != -1) {
            startCustomizingLocation(id);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finishAffinity();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        debug("creating loader");
        return new GetAllLocationsCursorLoader(this, dao);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        debug("loader finished his work");
        adapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}
}
