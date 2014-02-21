package com.cajama.malarialite;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.cajama.background.FinalSendingService;
import com.cajama.background.SyncService;

import java.io.File;

/**
 * Created by Jasper on 2/20/14.
 */
public class Initialize extends Activity {
    ProgressDialog pd;
    Context context;
    Button syncButton;
    Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize);
        context = getApplicationContext();
        pd = new ProgressDialog(this);
        pd.setMessage("Please wait...");
        pd.setTitle("Initializing App...");
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    public void startSync(View view) {
        Intent syncIntent = new Intent(this, SyncService.class);
        startService(syncIntent);
        syncButton = (Button) view;
        syncButton.setEnabled(false);
        pd.show();
        thread = new Thread() {
            public void run() {
                try{
                    int timeElapsed=0;
                    while (true) {
                        if (this.isInterrupted()) {
                            Log.d("Init", "interrupted");
                            break;
                        }
                        timeElapsed+=3;
                        sleep(3000);
                        Log.d("Init", String.valueOf(timeElapsed));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(SyncService.BROADCAST_INIT_SYNC));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Init", "onReceive");
            String str =intent.getStringExtra("init");
            if (str != null) {
                Log.d("Init", str);
                pd.dismiss();
                thread.interrupt();
                if (str.equals("init")) {
                    setResult(1, "done", new Bundle());
                    finish();
                }
                else {
                    syncButton.setEnabled(true);
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder
                            .setTitle("Error")
                            .setMessage("Connection to server failed. Please make sure that you are connected to the internet.")
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        }
    };
}
