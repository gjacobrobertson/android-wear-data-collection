package edu.utexas.cs.jacobr.wearabledatacollector;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;


public class WearableMessageService extends IntentService {
    public WearableMessageService() {
        super("WearableMessageService");
    }

    private static final String TAG = "WearableMessageService";
    private static final String COLLECT_DATA_PATH = "/collect-data";

    private GoogleApiClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        client = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        client.connect();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        for (String node : getNodes()) {
            sendMessage(node, COLLECT_DATA_PATH);
        }
        DataCollectionAlarmReceiver.completeWakefulIntent(intent);
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(client).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    private void sendMessage(String node, final String message) {
        Log.d(TAG, "Sending Message: " + message + " to Node: " + node);
        Wearable.MessageApi.sendMessage(
                client, node, message, new byte[0]).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }
}
