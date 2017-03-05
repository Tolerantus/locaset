package ru.inventions.tolerantus.locaset.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import ru.inventions.tolerantus.locaset.util.Loggable;

import static ru.inventions.tolerantus.locaset.service.ReceiverActionEnum.CANCEL;
import static ru.inventions.tolerantus.locaset.service.ReceiverActionEnum.PLAN;

/**
 * Created by Aleksandr on 04.03.2017.
 */

public class MyAlarmService  extends Loggable{

    private String tag = this.getClass().getSimpleName();

    public static int REQUEST_CODE = 555;

    public static boolean isAlarmSet;

    private AlarmManager alarmManager;

    private Context _context;

    public MyAlarmService(Context context) {
        alarmManager = ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
        _context = context;
    }

    private Intent createIntent(ReceiverActionEnum actionEnum) {
        Intent intent = new Intent(_context, Receiver.class);
        intent.setAction(actionEnum.toString());
        return intent;
    }

    public void replanAlarms() {
        cancel();
        SharedPreferences preferences = _context.getSharedPreferences("global", Context.MODE_PRIVATE);
        int minutes = preferences.getInt("gps_lookup_cycle", 10);
        debug("replanning alarms with " + minutes + " minutes cycle");
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis(), getPI(PLAN));
    }

    public void cancel() {
        debug("cancelling alarms");
        alarmManager.cancel(getPI(PLAN));
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis(), getPI(CANCEL));
        MyNotificationManager.cancelAllNotifications(_context);
    }

    private PendingIntent getPI(ReceiverActionEnum action) {
        Intent intent;
        switch (action) {
            case CANCEL:
                intent = createIntent(CANCEL);
                return PendingIntent.getBroadcast(_context, REQUEST_CODE, intent, 0);
            case PLAN:
                intent = createIntent(PLAN);
                return PendingIntent.getBroadcast(_context, REQUEST_CODE, intent, 0);
            default:
                throw new IllegalArgumentException("can;t create pending intent for " + action);
        }
    }




}
