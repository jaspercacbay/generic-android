package com.cajama.malarialite;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.cajama.background.FinalSendingService;
import com.cajama.background.SyncService;
import com.cajama.malarialite.entryLogs.QueueLogActivity;
import com.cajama.malarialite.entryLogs.SentLogActivity;
import com.cajama.malarialite.newreport.NewReportActivity;

import android.os.Handler;

import java.io.File;

public class MainActivity extends Activity {

    public static final int UPDATE_SETTINGS = 1001;
    public static final int INIT_SETTINGS = 1000;
    final Activity ctx = this;
    private Handler messageHandler = new Handler();
    boolean isCancelDialogOpen = false;
    SharedPreferences firstTimePref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File f = new File(String.valueOf(getExternalFilesDir(null)));
        Log.d("main", String.valueOf(getExternalFilesDir(null)));
        if (!f.exists()) {
            Log.d("main", "not exists");
            f.mkdirs();
        }
        /*Intent startUpdate = new Intent(this, UpdateService.class);
        startService(startUpdate);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivityForResult(settings, UPDATE_SETTINGS);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void submitNewReport(View view) {
        //turnGPSOn();
        File db = new File(getExternalFilesDir(null), "db.db");
        if (checkGps()) {
            if (db != null && db.exists()) {
                Log.d("main", "db exists");
                Intent intent = new Intent(this, NewReportActivity.class);
                startActivity(intent);
            }
            else {
                Log.d("main", "db is null");
                Intent intent = new Intent(this, SyncService.class);
                startService(intent);
            }
        }
    }

    private void turnGPSOn(){

        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if(!provider.contains("gps")){
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings","com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            sendBroadcast(poke);
        }
        System.out.println("gps turned on!");
    }

    public void viewQueueLog(View view) {
    	Intent startUpload = new Intent(this, FinalSendingService.class);
        startService(startUpload);
        Intent intent = new Intent(this, QueueLogActivity.class);
        startActivity(intent);
    }

    public void viewSentLog(View view) {
        Intent intent = new Intent(this, SentLogActivity.class);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        /*if (request == UPDATE_SETTINGS)*/ messageHandler.postDelayed(recreate, 0);
        /*else if (request == INIT_SETTINGS) {
            if (result == 1) {
                SharedPreferences.Editor editor = firstTimePref.edit();
                editor.putBoolean("firstTime", false);
                editor.commit();
            }
            else {
                Intent intent = new Intent(this, Initialize.class);
                startActivityForResult(intent, INIT_SETTINGS);
            }
        }*/
    }

    private Runnable recreate = new Runnable() {
        @Override
        public void run() {
            if (Build.VERSION.SDK_INT >= 11)
                ctx.recreate();
            else {
                onResume();
            }
            Log.w("Handler...", "Recreate requested.");
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        /*firstTimePref = getSharedPreferences("firstTime", Context.MODE_PRIVATE);
        boolean firstTime = firstTimePref.getBoolean("firstTime", true);
        if (firstTime) {
            System.out.println("first time!");
            Intent init = new Intent(this, Initialize.class);
            startActivityForResult(init, INIT_SETTINGS);
        }
        else {
            System.out.println("not!");
            Intent startSyncDB = new Intent(this, SyncService.class);
            startService(startSyncDB);
            Intent startUpload = new Intent(this, FinalSendingService.class);
            startService(startUpload);
        }*/
        Intent startSyncDB = new Intent(this, SyncService.class);
        startService(startSyncDB);
        Intent startUpload = new Intent(this, FinalSendingService.class);
        startService(startUpload);
    }

    private boolean checkGps() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!enabled) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder
                    .setTitle(R.string.warning)
                    .setMessage("This action needs the GPS Settings to be enabled. Do you want to enable GPS settings?")
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            isCancelDialogOpen = false;
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                            System.out.println("activate gps settings");
                        }
                    });

            if (!isCancelDialogOpen) {
                isCancelDialogOpen = true;
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}