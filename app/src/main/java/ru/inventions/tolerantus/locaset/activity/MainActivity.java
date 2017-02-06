package ru.inventions.tolerantus.locaset.activity;

import android.Manifest;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Dao;
import ru.inventions.tolerantus.locaset.service.MyGPSService;
import ru.inventions.tolerantus.locaset.util.LocationCursorAdapter;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ListView lv;
    private Dao dao;
    private LocationCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dao = new Dao(this);
        lv = (ListView) findViewById(R.id.lvMain);
        adapter = new LocationCursorAdapter(this, dao.getAllLocations(), CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        lv.setAdapter(adapter);
        registerForContextMenu(lv);
        findViewById(R.id.bt_add).setOnClickListener(this);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET},
                1);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.main_location_context_menu, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        if (MyGPSService.isServiceOnline()) {
            menu.getItem(0).setIcon(android.R.drawable.ic_media_pause);
        } else {
            menu.getItem(0).setIcon(android.R.drawable.ic_media_play);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_options_item_stop_service:
                if (!MyGPSService.isServiceOnline()) {
                    Toast.makeText(this, "Starting service", Toast.LENGTH_SHORT).show();
                    startService(new Intent(this, MyGPSService.class));
                    item.setIcon(android.R.drawable.ic_media_pause);
                } else {
                    Toast.makeText(this, "Stopping service", Toast.LENGTH_SHORT).show();
                    stopService(new Intent(this, MyGPSService.class));
                    item.setIcon(android.R.drawable.ic_media_play);
                }
        }
        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.itemDelete:
                dao.deleteLocationById(acmi.id);
                adapter.changeCursor(dao.getAllLocations());
                break;
            case R.id.itemCustomize:
                startCustomizingLocation(acmi.id);
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void startCustomizingLocation(long id) {
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
        adapter.changeCursor(dao.getAllLocations());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_add:
                addNewLocation();
        }
    }

    private void addNewLocation() {
        long id = dao.createLocation("NewLocation" + new SimpleDateFormat("_dd-MM-yyyy_HH:mm:ss", Locale.ENGLISH).format(new Date()), 60, 30, 0);
        if (id != -1) {
            startCustomizingLocation(id);
        }
    }
}
