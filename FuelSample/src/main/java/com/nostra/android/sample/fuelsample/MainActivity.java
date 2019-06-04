package com.nostra.android.sample.fuelsample;

import android.os.Bundle;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.AppCompatActivity;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;

import th.co.nostrasdk.NTSDKEnvironment;

public class MainActivity extends AppCompatActivity {

    //Create TabHost
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fuel);

        FragmentTabHost tabHost = (FragmentTabHost) findViewById(R.id.tabHost_Search);
        tabHost.setup(this, getSupportFragmentManager(), android.R.id.tabcontent);

        tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("Administrative"),
                AdminPolyFragment.class, null);
        tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("Mark on map"),
                MarkOnMapFragment.class, null);
    }
}