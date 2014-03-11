package com.cajama.malarialite.entryLogs;

import android.app.ListActivity;
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

import com.cajama.background.FinalSendingService;
import com.cajama.malarialite.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class QueueLogActivity extends ListActivity {
    private ArrayList<HashMap> list;
    private Intent intent, intentViewer;
    AdapterView.OnItemClickListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queuelog);
        intent = new Intent(this, FinalSendingService.class);
        intentViewer = new Intent(this, ReportViewerActivity.class);

        File dir = new File(String.valueOf(getExternalFilesDir("ZipFiles")));
        if (!dir.exists()) dir.mkdirs();

        listener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                HashMap value = (HashMap) adapterView.getItemAtPosition(i);
                Bundle bundle = new Bundle();
                bundle.putString("from", "queue");
                bundle.putString("date", (String) value.get("date"));
                bundle.putString("time", (String) value.get("time"));
                bundle.putString("name", (String) value.get("name"));
                intentViewer.putExtras(bundle);
                startActivity(intentViewer);
            }
        };

        updateListView();
    }

    public void updateListView() {
        File[] filesQueued = new File (String.valueOf(getExternalFilesDir("ZipFiles"))).listFiles();

        if (filesQueued != null) {
            Arrays.sort(filesQueued, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                }
            });
        }

        try {
            ArrayList<HashMap> logSet = new ArrayList<HashMap>();
            ArrayList<String[]> split = new ArrayList<String[]>();

            for (File file : filesQueued) {
                split.add(file.getName().split("_"));
            }

            logSet = getLogSet(split, logSet);

            ListView lview = (ListView) findViewById(android.R.id.list);
            entryAdapter adapter = new entryAdapter(this, logSet);
            lview.setOnItemClickListener(listener);
            lview.setAdapter(adapter);
        }
        catch (Exception e){
            e.printStackTrace();
            Log.d("Error", "blah");
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("update").equals("update")) updateListView();
        }
    };

    private ArrayList<HashMap> getLogSet(ArrayList<String[]> split, ArrayList<HashMap> logSet){
        for (int i=0; i<split.size(); i++) {
            HashMap map = new HashMap();
            map.put("date", formatDate(split.get(i)[0], "/"));
            map.put("time", formatTime(split.get(i)[1], ":"));
            map.put("name", split.get(i)[2].replace(".zip", ""));
            logSet.add(map);
        }
        return logSet;
    }

    @Override
    public void onResume() {
        super.onResume();
        startService(intent);
        registerReceiver(broadcastReceiver, new IntentFilter(FinalSendingService.BROADCAST_ACTION_QUEUE));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        stopService(intent);
    }

    public String formatDate(String str, String item) { // inserts / and : in date and time
        return str.substring(0, 4) + item + str.substring(4, 6) + item + str.substring(6, str.length());
    }

    public String formatTime(String str, String item) { // inserts / and : in date and time
        return str.substring(0, 2) + item + str.substring(2, 4) + item + str.substring(4, str.length());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.queue_log, menu);
        return true;
    }

    public void goHome(View view) {
        finish();
    }
}