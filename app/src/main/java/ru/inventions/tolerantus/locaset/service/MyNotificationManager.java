package ru.inventions.tolerantus.locaset.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

/**
 * Created by Aleksandr on 05.03.2017.
 */

public class MyNotificationManager {

    public static int NOTIFICATION_ID = 777;

    public static void notify(Context context, Notification notification) {
        NotificationManager manager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
        manager.notify(NOTIFICATION_ID, notification);
    }

    public static void cancelAllNotifications(Context context) {
        NotificationManager manager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
        manager.cancel(NOTIFICATION_ID);
    }
}
