package com.nostra.android.sample.addresssearchsample;

import android.app.Application;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;

import th.co.nostrasdk.NTSDKEnvironment;

public class SearchAddressApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);

        // TODO: Setting Licence_ID
         ArcGISRuntimeEnvironment.setLicense("Licence_ID");
    }
}
