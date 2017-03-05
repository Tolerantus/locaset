package ru.inventions.tolerantus.locaset.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import es.claucookie.miniequalizerlibrary.EqualizerView;
import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Location;
import ru.inventions.tolerantus.locaset.service.MyAlarmService;
import ru.inventions.tolerantus.locaset.service.MyGPSService;
import ru.inventions.tolerantus.locaset.service.media.MyMediaService;

/**
 * Created by Aleksandr on 25.02.2017.
 */

public class LocationsListAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater inflater;

    private List<Location> locations;

    public LocationsListAdapter(Context context, List<Location> locations) {
        this.context = context;
        this.locations = locations;
        inflater = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
    }

    @Override
    public int getCount() {
        if (locations == null) {
            locations = new ArrayList<>();
        }
        return locations.size();
    }

    @Override
    public Location getItem(int position) {
        if (locations == null || position > locations.size() - 1) {
            locations = new ArrayList<>();
            return null;
        } else {
            return locations.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.activity_main_list_item, parent, false);
        }
        EqualizerView equalizer = ((EqualizerView) view.findViewById(R.id.equalizer_view));

        Location location = getItem(position);
        if (location != null) {
            String locationName = location.getName();
            String address = location.getAddress();
            ((TextView) view.findViewById(R.id.tv_location_name)).setText(locationName);
            if (address != null && !address.isEmpty()) {
                ((TextView) view.findViewById(R.id.address)).setText(location.getAddress());
            }
            if (MyMediaService.currentPreferenceId.get() == location.getId() && MyAlarmService.isAlarmSet) {
                equalizer.animateBars();
                equalizer.setVisibility(View.VISIBLE);
            } else {
                equalizer.stopBars();
                equalizer.setVisibility(View.INVISIBLE);
            }
        }
        return view;
    }

    public List<Location> getLocations() {
        if (locations == null) {
            locations = new ArrayList<>();
        }
        return locations;
    }
}
