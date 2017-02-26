package ru.inventions.tolerantus.locaset.async;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Aleksandr on 29.01.2017.
 */

public class ThreadPoolProvider {

    private static Executor cachedInstance;
    private static Executor singleInstance;

    public static Executor getCachedInstance() {
        if (cachedInstance == null) {
            cachedInstance = Executors.newCachedThreadPool();
        }
        return cachedInstance;
    }

    public static Executor getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = Executors.newSingleThreadExecutor();
        }
        return singleInstance;
    }
}
