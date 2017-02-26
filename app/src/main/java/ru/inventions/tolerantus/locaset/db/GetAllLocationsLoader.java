package ru.inventions.tolerantus.locaset.db;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ru.inventions.tolerantus.locaset.async.AddressRefreshTask;

import static ru.inventions.tolerantus.locaset.util.LogUtils.error;

/**
 * Created by Aleksandr on 15.02.2017.
 */

public class GetAllLocationsLoader extends Loader<List<Location>> {


    public GetAllLocationsLoader(Context context) {
        super(context);
    }

    public List<Location> loadInBackground() {
        try {
            OrmDbOpenHelper ormDbOpenHelper = OpenHelperManager.getHelper(getContext(), OrmDbOpenHelper.class);
            return ormDbOpenHelper.getDao().queryForAll();
        } catch (SQLException e) {
            error("error during loading locations by OrmDbOpenHelper:" + e.getMessage());
        }
        return new ArrayList<>();
    }

//    @Override
//    public void forceLoad() {
//        AddressRefreshTask task = new AddressRefreshTask(getContext(), )
//    }
}
