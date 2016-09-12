package com.nostra.android.sample.searchsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

public class ListResultsActivity extends AppCompatActivity {
    private ListView lvLocation;

    private String[] addressResult;
    private double lat;
    private double lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_results);

        lvLocation = (ListView) findViewById(R.id.lvLocation);
        lon = getIntent().getExtras().getDouble("lon");
        lat = getIntent().getExtras().getDouble("lat");

        // Get parameter from CategoriesActivity
        if (getIntent().getStringArrayExtra("addressSearchResults") != null) {
            addressResult = getIntent().getStringArrayExtra("addressSearchResults");
            bindAddressSearchResult();
        } else if (getIntent().getStringArrayExtra("autoCompleteSearchResults") != null) {
            addressResult = getIntent().getStringArrayExtra("autoCompleteSearchResults");
            bindAutoCompleteSearchResult();
        }
        ImageButton imbBack = (ImageButton) findViewById(R.id.imbBack);
        imbBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void bindAddressSearchResult() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ListResultsActivity.this,
                R.layout.row_results, R.id.txvLocation, addressResult);
        lvLocation.setAdapter(adapter);
        lvLocation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ListResultsActivity.this, PinMarkerActivity.class);
                final String[] results = getIntent().getStringArrayExtra("addressSearchResults");
                String answer = results[position];
                intent.putExtra("listResults", answer);
                intent.putExtra("lat", lat);
                intent.putExtra("lon", lon);
                startActivity(intent);
            }
        });
    }

    private void bindAutoCompleteSearchResult() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ListResultsActivity.this,
                R.layout.row_results, R.id.txvLocation, addressResult);
        lvLocation.setAdapter(adapter);
        lvLocation.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ListResultsActivity.this, PinMarkerActivity.class);
                String answer = addressResult[position];
                intent.putExtra("listResults", answer);
                intent.putExtra("lat", lat);
                intent.putExtra("lon", lon);
                startActivity(intent);
            }
        });
    }
}