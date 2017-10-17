package com.nostra.android.sample.routesample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class DirectionAdapter extends BaseAdapter {
    private String[] strName;
    private int[] Length;
    private LayoutInflater mInflater;

    DirectionAdapter(Context context, String[] strName, int[] Length) {
        this.strName = strName;
        this.Length = Length;
        this.mInflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return strName.length;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View view, ViewGroup parent) {

        if (view == null) {
            view = mInflater.inflate(R.layout.direction_listview, parent, false);
        }

        TextView txvMeter = (TextView) view.findViewById(R.id.meter);
        txvMeter.setText(String.valueOf(Length[position]) + " " + "ม.");
        TextView txvVehicle = (TextView) view.findViewById(R.id.txvVehicle);
        txvVehicle.setText(strName[position]);
        String text = (String) txvVehicle.getText();
        ImageView start = (ImageView) view.findViewById(R.id.start);
        ImageView stop = (ImageView) view.findViewById(R.id.stop);

        if (text.contains("เริ่มต้น")) {
            start.setVisibility(View.VISIBLE);
        } else if (text.contains("สิ้นสุด")) {
            stop.setVisibility(View.VISIBLE);
        } else {
            start.setVisibility(View.GONE);
            stop.setVisibility(View.GONE);
        }

        return view;
    }
}