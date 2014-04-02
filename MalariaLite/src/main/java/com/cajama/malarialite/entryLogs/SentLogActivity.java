package com.cajama.malarialite.entryLogs;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cajama.background.FinalSendingService;
import com.cajama.malarialite.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class SentLogActivity extends Activity {
    final String TAG = "SentLogActivity";
    AdapterView.OnItemClickListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentlog);
        //intent = new Intent(this, FinalSendingService.class);
        final Intent intent = new Intent(this, ReportViewerActivity.class);

        listener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                HashMap value = (HashMap) adapterView.getItemAtPosition(i);
                Bundle bundle = new Bundle();
                String[] str = ((String) value.get("date")).split("\n");

                bundle.putString("from", "sent");
                bundle.putString("date", str[0]);
                bundle.putString("time", str[1]);
                bundle.putString("name", (String) value.get("name"));
                intent.putExtras(bundle);
                startActivity(intent);
            }
        };

        updateListView();
    }

    private void updateListView() {
        File sentLog = new File(String.valueOf(getExternalFilesDir(null)) + "/sent_log.txt");
        Log.d(TAG, sentLog.getPath());
        if (!sentLog.exists()) {
            Log.d(TAG, "no sentlog file");
            try {
                sentLog.createNewFile();
            } catch (IOException e) {
                Toast.makeText(this, "error creating sentlog file", Toast.LENGTH_LONG).show();
                Log.d(TAG, "error in creating sentLog");
                e.printStackTrace();
            }
        }

        ArrayList<String> logs;
        ReadTextFile rtf = new ReadTextFile(sentLog);

        try {
            logs = rtf.readText();
            Collections.reverse(logs);

            ArrayList<HashMap> logSet = new ArrayList<HashMap>();
            logSet = getLogSet(logs, logSet);

            ListView lview = (ListView) findViewById(android.R.id.list);
            entryAdapter adapter = new entryAdapter(this, logSet, R.layout.entry_row_sent);
            lview.setOnItemClickListener(listener);
            lview.setAdapter(adapter);

        } catch (FileNotFoundException e) {
            Toast.makeText(this, "error populating listview", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("update").equals("update")) updateListView();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(FinalSendingService.BROADCAST_ACTION_SENT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sent_log, menu);
        return true;
    }

    public void goHome(View view) {
        finish();
    }

    @Override
    protected void onStop(){
        Log.v("stop", "STOP");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v("stop","onDESTROY");
        //finish();
        //Log.v("stop","finish");
    }

    public ArrayList<HashMap> getLogSet(ArrayList<String> logs, ArrayList<HashMap> logSet) {
        for(int i=0;i<logs.size();i=i+3){
            HashMap map = new HashMap();
            String str = logs.get(i) + "_" + logs.get(i+1) + "_" + logs.get(i + 2);
            File file = new File(getExternalFilesDir(null), "Reports/"+str+"/validation.xml");
            map.put("date", formatDate(logs.get(i), "/") + "\n" + formatTime(logs.get(i + 1), ":"));
            map.put("time", file.exists() ? "Available" : "Pending");
            map.put("name", logs.get(i+2));
            logSet.add(map);
        }
        return logSet;
    }

    public String formatDate(String str, String item) { // inserts / and : in date and time
        return str.substring(0, 4) + item + str.substring(4, 6) + item + str.substring(6, str.length());
    }

    public String formatTime(String str, String item) { // inserts / and : in date and time
        return str.substring(0, 2) + item + str.substring(2, 4) + item + str.substring(4, str.length());
    }
}
