package com.nostra.android.sample.multimodalsample;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

class TravelAdapter extends BaseAdapter {
    private String[] mTravel;
    private boolean[] itemChecked;

    private Drawable drawableVehicle[];
    private LayoutInflater mInflater;

    TravelAdapter(Context context, String[] strTravel) {
        this.mTravel = strTravel;
        itemChecked = new boolean[strTravel.length];
        mInflater = LayoutInflater.from(context);
        drawableVehicle = new Drawable[strTravel.length];
        drawableVehicle[0] = ContextCompat.getDrawable(context,R.drawable.ic_local_airport_black_24dp);
        drawableVehicle[1] = ContextCompat.getDrawable(context,R.drawable.ic_directions_bus_black_24dp);
        drawableVehicle[2] = ContextCompat.getDrawable(context,R.drawable.ic_directions_subway_black_24dp);
        drawableVehicle[3] = ContextCompat.getDrawable(context,R.drawable.ic_directions_subway_black_24dp);
        drawableVehicle[4] = ContextCompat.getDrawable(context,R.drawable.ic_directions_bus_black_24dp);
        drawableVehicle[5] = ContextCompat.getDrawable(context,R.drawable.ic_directions_subway_black_24dp);
        drawableVehicle[6] = ContextCompat.getDrawable(context,R.drawable.ic_directions_railway_black_24dp);
        drawableVehicle[7] = ContextCompat.getDrawable(context,R.drawable.ic_directions_boat_black_24dp);
        drawableVehicle[8] = ContextCompat.getDrawable(context,R.drawable.ic_directions_bus_black_24dp);
    }

    ArrayList<String> getCheck(){
        ArrayList<String> checkList = new ArrayList<>();
        for(int i = 0; i< itemChecked.length; i++){
            if(itemChecked[i]){
                checkList.add(mTravel[i]);
            }
        }
        return checkList;
    }

    private class ViewHolder {
        ImageView imvTravel;
        TextView txvTravel;
        CheckBox cbTravel;
    }

    public int getCount() {
        return mTravel.length;
    }

    public Object getItem(int position) {
        return mTravel[position];
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View view, ViewGroup parent) {
        final ViewHolder holder;
        if( view == null) {
            view = mInflater.inflate(R.layout.row_travel_by, parent, false);
            holder = new ViewHolder();
            holder.imvTravel = (ImageView)view.findViewById(R.id.imvTravel);
            holder.txvTravel = (TextView) view.findViewById(R.id.txvTravel);
            holder.cbTravel = (CheckBox) view.findViewById(R.id.cbTravelBy);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        holder.txvTravel.setText(mTravel[position]);
        holder.cbTravel.setChecked(itemChecked[position]);
        holder.imvTravel.setImageDrawable(drawableVehicle[position]);

        holder.cbTravel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemChecked[position] = holder.cbTravel.isChecked();
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.cbTravelBy);
                checkBox.performClick();
            }
        });

        return view;
    }
}