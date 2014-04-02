package com.cajama.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by Jasper on 7/26/13.
 */
public class ConnectionBroadcastReceiver extends BroadcastReceiver {
    String TAG = "ConnectionBroadcastReceiver";
    static HashSet<String> hashSet = new HashSet<String>();

    /*@Override
    public void onReceive(final Context context, Intent intent) {

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE );
        NetworkInfo activeNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo activeWifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean wifiConnected = activeWifiInfo != null && activeWifiInfo.isConnected();
        boolean netConnected = activeNetInfo != null && activeNetInfo.isConnected();
        boolean isConnected = wifiConnected || netConnected;
        if (isConnected) {
            Log.d(TAG, "connected " +isConnected);
            Intent startServiceIntent = new Intent(context, FinalSendingService.class);
            context.startService(startServiceIntent);
        }
        else Log.d(TAG, "not connected " +isConnected);

    }*/

    @Override
    public void onReceive(Context context, Intent intent) {
        String key = intent.getStringExtra("extraInfo");
        if (!hashSet.contains(key)) {
            hashSet.add(key);
            if (NetworkUtil.getConnectivityStatus(context) != 0) {
                Intent validationService = new Intent(context, ValidationService.class);
                Intent sendService = new Intent(context, FinalSendingService.class);
                Intent syncService = new Intent(context, SyncService.class);
                context.startService(validationService);
                context.startService(sendService);
                context.startService(syncService);
            }
        }
        else {
            Log.d(TAG, "broadcast already received!");
            hashSet.remove(key);
        }
    }
}
