package ru.inventions.tolerantus.locaset.db;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;

/**
 * Created by Aleksandr on 15.02.2017.
 */

public class GetAllLocationsCursorLoader extends CursorLoader {

    private Dao dao;

    public GetAllLocationsCursorLoader(Context context, Dao dao) {
        super(context);
        this.dao = dao;
    }

    @Override
    public Cursor loadInBackground() {
        return dao.getAllLocations();
    }
}
