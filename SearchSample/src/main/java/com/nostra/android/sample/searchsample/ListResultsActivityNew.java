package com.nostra.android.sample.searchsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import th.co.nostrasdk.network.NTPoint;

public class ListResultsActivityNew extends AppCompatActivity {
    private ListView listView;
    private ImageView backActivity;
    private ArrayList<SearchResult> searchResultList;
    private List<NTPoint> points;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_results);
        setBindView();

        searchResultList = getIntent().getParcelableArrayListExtra("results");
        for (SearchResult searchResult : searchResultList) {
            points.add(searchResult.getPoint());
        }
        ResultsAdapter adapter = new ResultsAdapter(searchResultList, this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(setOnClickList());

        backActivity.setOnClickListener(setOnClickImg());
    }

    private View.OnClickListener setOnClickImg() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        };
    }

    private AdapterView.OnItemClickListener setOnClickList() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ListResultsActivityNew.this, PinMarkerActivity.class);
                intent.putExtra("result", searchResultList.get(position));
                intent.putExtra("lat", points.get(position).getY());
                intent.putExtra("lon", points.get(position).getX());
                startActivity(intent);
            }
        };
    }

    private void setBindView() {
        listView = (ListView) findViewById(R.id.lvLocation);
        backActivity = (ImageView) findViewById(R.id.imbBack);
        points = new ArrayList<>();
    }
}
