package ru.inventions.tolerantus.locaset.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Aleksandr on 29.01.2017.
 */

public class MyCachedThreadPoolProvider {

    private static Executor instance;

    public static Executor getInstance() {
        if (instance == null) {
            instance = Executors.newCachedThreadPool();
        }
        return instance;
    }
}
