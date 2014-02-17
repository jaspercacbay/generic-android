package com.cajama.malarialite.newreport;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.cajama.malarialite.R;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by GMGA on 8/7/13.
 */
public class summaryAdapter extends BaseAdapter{

    public ArrayList<Map<String, String>> list;
    Activity activity;

    public summaryAdapter(Activity activity, ArrayList<Map<String, String>> list) {
        super();
        this.activity = activity;
        this.list = list;
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
        TextView txtLabel;
        TextView txtValue;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub

        // TODO Auto-generated method stub
        ViewHolder holder;
        LayoutInflater inflater =  activity.getLayoutInflater();

        if (convertView == null)
        {
            convertView = inflater.inflate(R.layout.summary_row, null);
            holder = new ViewHolder();
            holder.txtLabel = (TextView) convertView.findViewById(R.id.label);
            holder.txtValue = (TextView) convertView.findViewById(R.id.value);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder) convertView.getTag();
        }

        Map<String, String> map = list.get(position);
        holder.txtLabel.setText(map.get("label").toString());
        holder.txtValue.setText(map.get("value").toString());

        return convertView;
    }

}