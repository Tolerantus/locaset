package ru.inventions.tolerantus.locaset.util;

import android.util.Log;

/**
 * Created by Aleksandr on 04.03.2017.
 */

public abstract class Loggable {

    private String tag = this.getClass().getSimpleName();

    public void debug(String msg) {
        Log.d(tag, msg);
    }

    public void error(String msg) {
        Log.d(tag, msg);
    }

    public void warn(String msg) {
        Log.w(tag, msg);
    }
}
