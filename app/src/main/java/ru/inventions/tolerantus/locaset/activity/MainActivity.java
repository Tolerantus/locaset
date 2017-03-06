package ru.inventions.tolerantus.locaset.activity;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.sql.SQLException;
import java.util.ArrayList;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.adapter.LocationsListAdapter;
import ru.inventions.tolerantus.locaset.async.LocationUpdateTask;
import ru.inventions.tolerantus.locaset.async.ThreadPoolProvider;
import ru.inventions.tolerantus.locaset.db.Location;
import ru.inventions.tolerantus.locaset.db.LocationFabric;
import ru.inventions.tolerantus.locaset.db.OrmDbOpenHelper;
import ru.inventions.tolerantus.locaset.service.MyAlarmService;
import ru.inventions.tolerantus.locaset.service.MyGPSService;
import ru.inventions.tolerantus.locaset.util.LocationActionEnum;

import static ru.inventions.tolerantus.locaset.util.LogUtils.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private LocationsListAdapter adapter;
    private DrawerLayout drawer;
    private ListView drawerList;
    private OrmDbOpenHelper ormDbOpenHelper;
    private MyAlarmService _alarmService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        debug("creating main activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        {
            drawer = ((DrawerLayout) findViewById(R.id.drawer_layout));
            drawerList = ((ListView) findViewById(R.id.left_drawer));
            drawerList.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1,
                    getResources().getStringArray(R.array.drawer_items))
            );
            drawerList.setOnItemClickListener(this);
        }

        {
            ormDbOpenHelper = OpenHelperManager.getHelper(this, OrmDbOpenHelper.class);
            ListView lv = (ListView) findViewById(R.id.lvMain);
            adapter = new LocationsListAdapter(this, new ArrayList<Location>());
            lv.setAdapter(adapter);
            registerForContextMenu(lv);
        }

        _alarmService = new MyAlarmService(this);

        findViewById(R.id.bt_add).setOnClickListener(this);

        {
            SwipeRefreshLayout refreshLayout = ((SwipeRefreshLayout) findViewById(R.id.refresh_layout));
            refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refreshList();
                }
            });
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
    }

    private void refreshList() {
        SwipeRefreshLayout refreshLayout = ((SwipeRefreshLayout) findViewById(R.id.refresh_layout));
        refreshLayout.setRefreshing(true);
        LocationUpdateTask task = new LocationUpdateTask(null, LocationActionEnum.UPDATE, this, null, adapter);
        task.executeOnExecutor(ThreadPoolProvider.getCachedInstance());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.main_location_context_menu, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        renderActionPanel(menu.findItem(R.id.main_opt_service));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_opt_service:
                if (isNotificationPolicyAccessGranted()) {
                    if (!MyAlarmService.isAlarmSet) {
                        _alarmService.replanAlarms();
                    } else {
                        _alarmService.cancel();
                    }
                    renderActionPanel(item);
                    adapter.notifyDataSetInvalidated();
                } else {
                    debug("app doesn't have permission for managing notification policies, starting activity to fix this.");
                    Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivity(intent);
                }
                break;
        }
        return true;
    }

    private void renderActionPanel(MenuItem item) {
        if (_alarmService.isAnyAlarmPlanned()) {
            item.setIcon(android.R.drawable.ic_media_pause);
        } else {
            item.setIcon(android.R.drawable.ic_media_play);
        }
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
                LocationUpdateTask task = new LocationUpdateTask(adapter.getItem(acmi.position), LocationActionEnum.DELETE, this, null, adapter);
                task.executeOnExecutor(ThreadPoolProvider.getCachedInstance());
                break;
            case R.id.itemCustomize:
                checkMap(acmi.id);
                break;
            case R.id.itemDetails:
                checkDetails(acmi.id);
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void checkMap(long id) {
        debug("preparing location customizing activity for location id=" + id);
        Intent settingsIntent = new Intent(this, MapActivity.class);
        settingsIntent.putExtra(getString(R.string.location_id), id);
        startActivity(settingsIntent);
    }

    private void checkDetails(long id) {
        debug("preparing location customizing activity for location id=" + id);
        Intent settingsIntent = new Intent(this, DetailedSettingsActivity.class);
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
        refreshList();
        super.onResume();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_add:
                addNewLocation();
        }
    }

    private void addNewLocation() {
        try {
            Location newLocation = LocationFabric.createLocation();
            ormDbOpenHelper.getDao().create(newLocation);
            long id = newLocation.getId();
            debug("added location with id=" + id);
            if (id != -1) {
                checkMap(id);
            }
        } catch (SQLException e) {
            error("error during location creation: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ormDbOpenHelper != null) {
            OpenHelperManager.releaseHelper();
            ormDbOpenHelper = null;
        }
        finishAffinity();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        drawerList.setItemChecked(position, true);
        drawer.closeDrawer(drawerList);
        switch (position) {
            case 0:
                Intent globalPreferences = new Intent(this, GlobalPreferencesActivity.class);
                startActivity(globalPreferences);
                break;
        }
    }
}
