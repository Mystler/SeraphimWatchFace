package eu.mystler.seraphimwatchface;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.support.v4.app.ActivityCompat;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SeraphimService extends WearableListenerService {
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (!messageEvent.getPath().equals("/seraphim-update-request"))
            return;

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .build();
        ConnectionResult connectionResult = googleApiClient.blockingConnect(10, TimeUnit.SECONDS);
        if (!connectionResult.isSuccess())
            return;

        // Update phone battery percentage
        Wearable.MessageApi.sendMessage(googleApiClient, messageEvent.getSourceNodeId(), "/seraphim-update-phonebattery", getBatteryPercentage().getBytes());

        // Update Temperature
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location lastLoc = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            String temp = getTemperature(lastLoc);
            if (temp != null)
                Wearable.MessageApi.sendMessage(googleApiClient, messageEvent.getSourceNodeId(), "/seraphim-update-temperature", temp.getBytes());
        }

        googleApiClient.disconnect();
    }

    private String getBatteryPercentage() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = registerReceiver(null, ifilter);
        return String.valueOf(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) + "%");
    }

    private String getTemperature(Location loc) {
        if (loc == null)
            return "";
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://api.openweathermap.org/data/2.5/weather?lat=" + loc.getLatitude()
                + "&lon=" + loc.getLongitude()
                + "&units=metric&appid=ee28eedf3a25bd3b820a06a65e453b73";
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(url, null, future, future);
        queue.add(request);
        try {
            JSONObject response = future.get(10, TimeUnit.SECONDS);
            return String.valueOf(Math.round(response.getJSONObject("main").getDouble("temp"))) + "°";
        } catch (InterruptedException | TimeoutException | ExecutionException | JSONException e) {
            return "";
        }
    }
}
