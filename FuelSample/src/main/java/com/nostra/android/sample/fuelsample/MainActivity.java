package com.nostra.android.sample.fuelsample;

import android.os.Bundle;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.AppCompatActivity;

import com.esri.android.runtime.ArcGISRuntime;

import th.co.nostrasdk.NTSDKEnvironment;

public class MainActivity extends AppCompatActivity {

    //Create TabHost
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fuel);

        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("GpaFVfndCwAsINg8V7ruX9DNKvwyOOg(OtcKjh7dfAyIppXlmS9I)Q1mT8X0W685UxrXVI6V7XuNSRz7IyuXWSm=====2", this);
        // TODO: Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

        FragmentTabHost tabHost = (FragmentTabHost) findViewById(R.id.tabHost_Search);
        tabHost.setup(this, getSupportFragmentManager(), android.R.id.tabcontent);

        tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("Administrative"),
                AdminPolyFragment.class, null);
        tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("Mark on map"),
                MarkOnMapFragment.class, null);

        /*
        TabHost tabHost = (TabHost) findViewById(R.id.tabHost_Search);
        LocalActivityManager localActivityManager = new LocalActivityManager(this, true);
        tabHost.setup(localActivityManager);
        localActivityManager.dispatchCreate(savedInstanceState);

        TabHost.TabSpec spec;
        Intent activity_tab1 = new Intent(this, AdminPolyFragment.class);
        spec = tabHost.newTabSpec("Tab1").setIndicator("ADMINPOLY").setContent(activity_tab1);
        tabHost.addTab(spec);

        Intent activity_tab2 = new Intent(this, MarkOnMapFragment.class);
        spec = tabHost.newTabSpec("Tab2").setIndicator("MARK ON MAP").setContent(activity_tab2);
        tabHost.addTab(spec);
        */
    }
}