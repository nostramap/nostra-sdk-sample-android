package com.nostra.android.sample.routesample;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import static com.nostra.android.sample.routesample.R.drawable.ic_directions_bike_48px;
import static com.nostra.android.sample.routesample.R.drawable.ic_directions_car_48px;
import static com.nostra.android.sample.routesample.R.drawable.ic_directions_walk_48px;

class VehicleAdapter extends BaseAdapter {
    private Context mContext;
    private String[] mVehicle;

    VehicleAdapter(Context context, String[] Vehicle) {
        this.mContext= context;
        this.mVehicle = Vehicle;
    }

    public int getCount() {
        return mVehicle.length;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (view == null) {
            view = mInflater.inflate(R.layout.row_direction, parent, false);
        }
        TextView txvVehicle = (TextView)view.findViewById(R.id.txvVehicle);
        ImageView icon = (ImageView)view.findViewById(R.id.icon);
        txvVehicle.setText(mVehicle[position]);

        Drawable drawableVehicle[] = new Drawable[4];
        drawableVehicle[0] = ContextCompat.getDrawable(parent.getContext(),ic_directions_car_48px);
        drawableVehicle[1] = ContextCompat.getDrawable(parent.getContext(),ic_directions_bike_48px);
        drawableVehicle[2] = ContextCompat.getDrawable(parent.getContext(),ic_directions_bike_48px);
        drawableVehicle[3] = ContextCompat.getDrawable(parent.getContext(),ic_directions_walk_48px);

        if (position == 0) {
            icon.setImageDrawable(drawableVehicle[0]);
        }
        if (position == 1){
            icon.setImageDrawable(drawableVehicle[1]);
        }
        if (position == 2){
            icon.setImageDrawable(drawableVehicle[2]);
        }
        if (position == 3){
            icon.setImageDrawable(drawableVehicle[3]);
        }

        return view;
    }
}