package ru.inventions.tolerantus.locaset.util;

import android.util.Log;

/**
 * Created by Aleksandr on 11.02.2017.
 */

public class LogUtils {

    private final static String tag = "Locaset logs";

    public static void error(String errorMessage) {
        Log.e(tag, errorMessage);
    }

    public static void debug(String debugMessage) {
        Log.d(tag, debugMessage);
    }
}
