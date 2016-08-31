package com.nostra.android.sample.multimodalsample;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

class ResultDirectionAdapter extends BaseAdapter {
    private ArrayList<String> mDirections;
    private LayoutInflater mInflater;
    private String[] type;
    private Drawable drawable[];
    private String[] arrDistanceAndTime;

    ResultDirectionAdapter(Context context, ArrayList<String> arrDirection, String[] type, String[] arrDistanceAndTime) {
        this.mDirections = arrDirection;
        this.type = type;
        this.arrDistanceAndTime = arrDistanceAndTime;
        this.mInflater = LayoutInflater.from(context);
        drawable = new Drawable[2];
        drawable[0] = context.getResources().getDrawable(R.drawable.flag);
        drawable[1] = context.getResources().getDrawable(R.drawable.flag_des);
    }

    @Override
    public int getCount() {
        return mDirections.size();
    }

    @Override
    public Object getItem(int position) {
        return mDirections.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.row_result_direction, parent, false);
        }

        TextView txvDirection = (TextView)convertView.findViewById(R.id.txvDirection);
        ImageView imvFlag = (ImageView) convertView.findViewById(R.id.imvFlag);
        TextView txvDirectionAndTime = (TextView)convertView.findViewById(R.id.txvDirectionAndTime);
        TextView txvType = (TextView)convertView.findViewById(R.id.txvType);

        txvDirection.setText(mDirections.get(position));
        int size = mDirections.size();
        if (position == 0) {
            imvFlag.setImageDrawable(drawable[0]);
            txvDirectionAndTime.setText("");
            txvType.setText(" ");
        } else if(position == size-1) {
            imvFlag.setImageDrawable(drawable[1]);
            txvDirectionAndTime.setText("");
            txvType.setText(" ");
        }
        if (position >= 1 && position <= size - 2 ) {
            txvType.setText(type[position-1]);
            txvDirectionAndTime.setText(arrDistanceAndTime[position-1]);
            imvFlag.setImageDrawable(null);
        }

        return convertView;
    }
}