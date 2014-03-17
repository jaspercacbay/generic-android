package com.cajama.malarialite.entryLogs;

import java.util.ArrayList;
import java.util.HashMap;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.cajama.malarialite.R;

/**
 *
 * @author Paresh N. Mayani
 */
public class entryAdapter extends BaseAdapter
{
    public ArrayList<HashMap> list;
    Activity activity;
    int layoutId;

    public entryAdapter(Activity activity, ArrayList<HashMap> list, int layoutId) {
        super();
        this.activity = activity;
        this.list = list;
        this.layoutId = layoutId;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    private class ViewHolder {
        TextView txtDate;
        TextView txtTime;
        TextView txtName;
        TextView txtParts;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub

        // TODO Auto-generated method stub
        ViewHolder holder;
        LayoutInflater inflater =  activity.getLayoutInflater();

        if (convertView == null)
        {
            //convertView = inflater.inflate(R.layout.entry_row_queue, null);
            convertView = inflater.inflate(layoutId, null);
            holder = new ViewHolder();
            holder.txtDate = (TextView) convertView.findViewById(R.id.date);
            holder.txtTime = (TextView) convertView.findViewById(R.id.time);
            holder.txtName = (TextView) convertView.findViewById(R.id.name);
            convertView.setTag(holder);

            HashMap map = list.get(position);
            holder.txtDate.setText(map.get("date").toString());
            holder.txtTime.setText(map.get("time").toString());
            holder.txtName.setText(map.get("name").toString());

            if (map.containsKey("filesize")) {
                int fileSize = Integer.parseInt(map.get("filesize").toString());
                float partSize = Float.parseFloat(map.get("partsize").toString());
                int total = new Double(Math.ceil(fileSize/(partSize*1024))).intValue() + 1;
                int current = Integer.parseInt(map.get("current").toString());
                int percent = ((total-current)*100) / total;

                holder.txtParts = (TextView) convertView.findViewById(R.id.parts);
                holder.txtParts.setText(String.valueOf(percent)+"%");
            }

        }
        else
        {
            holder = (ViewHolder) convertView.getTag();
        }



        return convertView;
    }

}