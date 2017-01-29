package ru.inventions.tolerantus.locaset.activity;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import ru.inventions.tolerantus.locaset.R;

/**
 * Created by Aleksandr on 29.01.2017.
 */

public class MyPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.location_preferences);
    }
}
