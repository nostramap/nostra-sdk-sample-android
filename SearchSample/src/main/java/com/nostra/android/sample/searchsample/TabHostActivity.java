package com.nostra.android.sample.searchsample;

import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TabHost;

public class TabHostActivity extends AppCompatActivity {
    private LocalActivityManager localActivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabhost);

        TabHost tabHost;
        tabHost = (TabHost) findViewById(R.id.thSearch);

        localActivityManager = new LocalActivityManager(this, false);
        double lat;
        double lon;
        lat = getIntent().getExtras().getDouble("lat");
        lon = getIntent().getExtras().getDouble("lon");
        tabHost.setup(localActivityManager);
        localActivityManager.dispatchCreate(savedInstanceState);

        //Create TabHost
        TabHost.TabSpec spec;
        Intent activity_tab1 = new Intent().setClass(this, KeywordActivity.class);
        activity_tab1.putExtra("lon", lon);
        activity_tab1.putExtra("lat", lat);
        spec = tabHost.newTabSpec("Tab1").setIndicator("Keyword").setContent(activity_tab1);
        tabHost.addTab(spec);

        Intent activity_tab2 = new Intent().setClass(this, CategoriesActivity.class);
        activity_tab2.putExtra("lon", lon);
        activity_tab2.putExtra("lat", lat);
        spec = tabHost.newTabSpec("Tab2").setIndicator("Categories").setContent(activity_tab2);
        tabHost.addTab(spec);

        Intent activity_tab3 = new Intent().setClass(this, LocalCategoriesActivity.class);
        activity_tab3.putExtra("lon", lon);
        activity_tab3.putExtra("lat", lat);
        spec = tabHost.newTabSpec("Tab3").setIndicator("LocalCategories").setContent(activity_tab3);
        tabHost.addTab(spec);
    }

    @Override
    protected void onPause() {
        super.onPause();
        localActivityManager.dispatchPause(isFinishing());
    }

    @Override
    protected void onResume() {
        super.onResume();
        localActivityManager.dispatchResume();
    }
}