package com.cajama.malarialite.newreport;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import com.cajama.malarialite.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class test extends ListActivity {
    private ArrayList<HashMap> list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queuelog);

        ArrayList<String> logs;

        String[] from = {"label","value"};
        int [] to = {R.id.label, R.id.value};
        //ListView lview = (ListView) findViewById(R.id.summary_l);
    }



    private ArrayList<Map<String,String>> buildSummary(ArrayList<List<String>> data){
        ArrayList<Map<String,String>> list = new ArrayList<Map<String, String>>();
        for(int i=0; i < data.size();i++){
            list.add(putSummary(data.get(i).get(0),data.get(i).get(1)));
        }

        return list;
    }
    private HashMap<String,String> putSummary(String label,String value){
        HashMap<String,String> line = new HashMap<String, String>();
        line.put("label",label);
        line.put("value",value);
        return line;
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