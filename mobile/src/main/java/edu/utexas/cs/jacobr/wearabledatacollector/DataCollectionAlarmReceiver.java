package edu.utexas.cs.jacobr.wearabledatacollector;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class DataCollectionAlarmReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = "DataCollectionAlarmReceiver";
    private static final long INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        Intent service = new Intent(context, WearableMessageService.class);
        startWakefulService(context, service);
    }

    public void setAlarm(Context context) {
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DataCollectionAlarmReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        Log.d(TAG, "Setting Alarm");
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                INTERVAL,
                INTERVAL, alarmIntent);

    }

    public void cancelAlarm(Context context) {
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DataCollectionAlarmReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        Log.d(TAG, "Cancelling Alarm");
        alarmManager.cancel(alarmIntent);
    }
}
