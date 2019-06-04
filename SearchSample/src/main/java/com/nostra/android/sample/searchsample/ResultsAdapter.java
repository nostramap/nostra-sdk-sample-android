package com.nostra.android.sample.searchsample;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by GEOTALENT114 on 10/16/2017.
 */

public class ResultsAdapter extends BaseAdapter {

    private ArrayList<SearchResult> results;
    private Context context;
    private Holder holder;

    public ResultsAdapter(ArrayList<SearchResult> results, Context context) {
        this.results = results;
        this.context = context;
    }

    @Override
    public int getCount() {
        return results.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = LayoutInflater.from(context).inflate(R.layout.row_results, null);
        holder = new Holder();
        holder.title = (TextView) convertView.findViewById(R.id.txvLocation);
        convertView.setTag(holder);
        holder.title.setText(results.get(position).getLocalName() + "   ชุมชน" +
                results.get(position).getAdmin4() + "   แขวง" +
                results.get(position).getAdmin3() + "   เขต" +
                results.get(position).getAdmin2() + "   จังหวัด" +
                results.get(position).getAdmin1() + "   \n" +
                String.format(Locale.ENGLISH, "%.6f", results.get(position).getLatitude()) + " " +
                String.format(Locale.ENGLISH, "%.6f", results.get(position).getLongitude()));
        return convertView;
    }

    private class Holder {
        TextView title;
    }
}
