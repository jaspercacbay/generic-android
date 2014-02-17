package com.cajama.malarialite.newreport;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.cajama.android.customviews.SquareImageView;

import java.util.Vector;

/**
 * Created by GMGA on 8/5/13.
 */
public class ImageAdapter extends BaseAdapter{
    private Context mContext;
    private Vector<myBitmap> images = new Vector<myBitmap>();

    public ImageAdapter(Context c) {
        mContext = c;
    }

    public void AddImage(myBitmap b) {
        images.add(b);
    }

    public void remove(int pos) {
        images.remove(pos);
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public myBitmap getItem(int arg0) {
        return images.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SquareImageView img;
        if(convertView ==null) {
            img = new SquareImageView(mContext);
        }
        else {
            img = (SquareImageView)convertView;
        }

        img.setImageBitmap(images.get(position).image);
        img.setScaleType(SquareImageView.ScaleType.CENTER_CROP);
        return img;
    }
}
