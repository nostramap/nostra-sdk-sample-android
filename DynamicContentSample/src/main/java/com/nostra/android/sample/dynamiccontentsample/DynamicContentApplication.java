package com.nostra.android.sample.dynamiccontentsample;

import android.app.Application;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;

import th.co.nostrasdk.NTSDKEnvironment;

public class DynamicContentApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // TODO: Setting Licence ID
        ArcGISRuntimeEnvironment.setLicense("Licence_ID");
    }
}
