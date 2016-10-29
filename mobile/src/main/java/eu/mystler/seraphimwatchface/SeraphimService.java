package eu.mystler.seraphimwatchface;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

public class SeraphimService extends WearableListenerService {
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        ConnectionResult connectionResult = googleApiClient.blockingConnect(30, TimeUnit.SECONDS);
        if (!connectionResult.isSuccess())
            return;
        if (messageEvent.getPath().equals("/seraphim-update-request"))
            Wearable.MessageApi.sendMessage(googleApiClient, messageEvent.getSourceNodeId(), "/seraphim-update-response", getBatteryPercentage().getBytes());
        googleApiClient.disconnect();
    }

    private String getBatteryPercentage() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = registerReceiver(null, ifilter);
        return String.valueOf(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) + "%");
    }
}
