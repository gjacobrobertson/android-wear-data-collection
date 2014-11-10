package edu.utexas.cs.jacobr.wearabledatacollector;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

/**
 * Listens to DataItems and Messages from the local node.
 */
public class DataCollectionListenerService extends WearableListenerService implements SensorEventListener {
    private static final String TAG = "DataCollectionListenerService";
    private static final String COLLECT_DATA_PATH = "/collect-data";
    private static final String SENSOR_DATA_PATH = "/sensor-data";
    private static final long SENSOR_WINDOW = 30000;

    private GoogleApiClient mGoogleApiClient;
    private SensorManager sensorManager;
    private PutDataMapRequest sensorData;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                TAG);

    }

    @Override // SensorEventListener
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        return;
    }

    @Override // SensorEventListener
    public final void onSensorChanged(SensorEvent event) {
        String key = event.sensor.getName();
        float[] values = event.values;
        int currentAccuracy = sensorData.getDataMap().getInt(key + " Accuracy");
        if(event.accuracy > currentAccuracy) {
            Log.d(TAG, "New reading for sensor: " + key);
            sensorData.getDataMap().putFloatArray(key, values);
            sensorData.getDataMap().putInt(key + " Accuracy", event.accuracy);
        }
        if (event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
            Log.d(TAG, "Unregistering sensor: " + key);
            sensorManager.unregisterListener(this, event.sensor);
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // Check to see if the message is to start an activity
        String path = messageEvent.getPath();
        Log.d(TAG, "onMessageReceived: " + path);
        if (path.equals(COLLECT_DATA_PATH)) {
            try {
                acquireWakeLock();
                startSensorListeners();
                Thread.sleep(SENSOR_WINDOW);
                stopSensorListeners();
                sendSensorData();
                releaseWakeLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPeerConnected(Node peer) {
        Log.d(TAG, "onPeerConnected: " + peer);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        Log.d(TAG, "onPeerDisconnected: " + peer);
    }

    private void startSensorListeners() {
        Log.d(TAG, "startSensorListeners");
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        sensorData = PutDataMapRequest.create(SENSOR_DATA_PATH);
        sensorData.getDataMap().putLong("Timestamp", System.currentTimeMillis());
        float[] empty = new float[0];
        for (Sensor sensor : sensors) {
            sensorData.getDataMap().putFloatArray(sensor.getName(), empty);
            sensorData.getDataMap().putInt(sensor.getName() + " Accuracy", 0);
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void stopSensorListeners() {
        Log.d(TAG, "stopSensorListeners");
        sensorManager.unregisterListener(DataCollectionListenerService.this);
    }

    private void sendSensorData() {
        Log.d(TAG, "sendSensorData");
        PutDataRequest request = sensorData.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request);
    }

    private void acquireWakeLock() {
        Log.d(TAG, "acquireWakeLock");
        wakeLock.acquire();
    }

    private void releaseWakeLock() {
        Log.d(TAG, "releaseWakeLock");
        wakeLock.release();
    }
}