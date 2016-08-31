package com.nostra.android.sample.fuelsample;

import android.app.LocalActivityManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TabHost;

import com.esri.android.runtime.ArcGISRuntime;

import th.co.gissoft.nostrasdk.Base.NTSDKEnvironment;

public class MainActivity extends AppCompatActivity {

    //Create TabHost
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fuel);

        // Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

        TabHost tabHost = (TabHost) findViewById(R.id.tabHost_Search);
        LocalActivityManager localActivityManager = new LocalActivityManager(this, true);
        tabHost.setup(localActivityManager);
        localActivityManager.dispatchCreate(savedInstanceState);

        TabHost.TabSpec spec;
        Intent activity_tab1 = new Intent(this, AdminPolyActivity.class);
        spec = tabHost.newTabSpec("Tab1").setIndicator("ADMINPOLY").setContent(activity_tab1);
        tabHost.addTab(spec);

        Intent activity_tab2 = new Intent(this, MarkOnMapActivity.class);
        spec = tabHost.newTabSpec("Tab2").setIndicator("MARK ON MAP").setContent(activity_tab2);
        tabHost.addTab(spec);
    }
}