package com.cajama.background;

/**
 * Created by Jasper on 2/16/14.
 */
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

import com.cajama.malarialite.R;

import java.io.File;

public class NetworkUtil {

    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;


    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    public static String getConnectivityStatusString(Context context) {
        int conn = NetworkUtil.getConnectivityStatus(context);
        String status = null;
        if (conn == NetworkUtil.TYPE_WIFI) {
            status = "Wifi enabled";
        } else if (conn == NetworkUtil.TYPE_MOBILE) {
            status = "Mobile data enabled";
        } else if (conn == NetworkUtil.TYPE_NOT_CONNECTED) {
            status = "Not connected to Internet";
        }
        return status;
    }

    public static boolean dbExists(Context context) {
        return new File(context.getExternalFilesDir(null), "db.db").exists();
    }

    public static AlertDialog createDialog(Context context, String title, String message, boolean cancellable, DialogInterface.OnClickListener cancel, DialogInterface.OnClickListener ok) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setTitle(title)
                .setMessage(message)
                .setCancelable(cancellable);
        if (cancel != null) builder.setNegativeButton("Cancel", cancel);
        if (ok != null) builder.setPositiveButton("OK", ok);
        return builder.create();
    }

    public static String checkWebAddress(String str) {
        if (!str.startsWith("http://")) str = "http://" + str;
        if (str.charAt(str.length()-1)!='/') str += "/";
        return str;
    }
}
