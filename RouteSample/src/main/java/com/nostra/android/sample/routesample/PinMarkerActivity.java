package com.nostra.android.sample.routesample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.loadable.LoadStatusChangedEvent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedListener;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;
import com.esri.arcgisruntime.security.UserCredential;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;

public class PinMarkerActivity extends AppCompatActivity implements LoadStatusChangedListener {
    private MapView mapView;
    private NTMapPermissionResult[] ntMapResults;
    private LocationDisplay locationDisplay;
    private boolean isMapLoad = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pinmarker);

        mapView = findViewById(R.id.mapView);
        mapView.setWrapAroundMode(WrapAroundMode.ENABLE_WHEN_SUPPORTED);

        locationDisplay = mapView.getLocationDisplay();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);

        //Add map and current location
        NTMapPermissionService.executeAsync(new ServiceRequestListener<NTMapPermissionResultSet>() {
            @Override
            public void onResponse(NTMapPermissionResultSet result) {
                ntMapResults = result.getResults();
                NTMapPermissionResult map = getThailandBasemap();
                if (map != null) {
                    NTMapServiceInfo info = map.getLocalService();
                    String url = info.getServiceUrl();
                    String token = info.getServiceToken();
                    // TODO: Insert referrer
                    String referrer = "REFERRER";

                    UserCredential credential = UserCredential.createFromToken(token,referrer);

                    ArcGISTiledLayer layer = new ArcGISTiledLayer(url);
                    layer.setCredential(credential);
                    Basemap basemap = new Basemap(layer);
                    ArcGISMap arcGISMap = new ArcGISMap(basemap);
                    arcGISMap.addLoadStatusChangedListener(PinMarkerActivity.this);

                    mapView.setMap(arcGISMap);
                    mapView.setAttributionTextVisible(false);
                }
            }

            @Override
            public void onError(String errorMessage,int statusCode) {
                Toast.makeText(PinMarkerActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        // Send parameter to RouteActivity
        Button btnOk = findViewById(R.id.btnOk);
        btnOk.setOnClickListener(btnOkClickListener);

        final Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationDisplay.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationDisplay.startAsync();
    }

    private View.OnClickListener btnOkClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isMapLoad) {
                Point pointCenter = mapView.getVisibleArea().getExtent().getCenter();

                Intent intent = new Intent();
                intent.putExtra("locationX", pointCenter.getX());
                intent.putExtra("locationY", pointCenter.getY());
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(PinMarkerActivity.this, "Map Loading", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private NTMapPermissionResult getThailandBasemap() {
        for (NTMapPermissionResult result : ntMapResults) {
            // Thailand basemap service id is 2
            if (result.getServiceId() == 2) {
                return result;
            }
        }
        return null;
    }

    @Override
    public void loadStatusChanged(LoadStatusChangedEvent loadStatusChangedEvent) {
        String mapLoadStatus = loadStatusChangedEvent.getNewLoadStatus().name();
        switch(mapLoadStatus){
            case "LOADED":
                locationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
                    boolean locationChanged = false;
                    @Override
                    public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
                        isMapLoad = true;
                        if (!locationChanged) {
                            locationChanged = true;
                            LinearUnit unit = new LinearUnit(LinearUnitId.MILES);
                            double zoomWidth = unit.toMeters(50);
                            mapView.setViewpointCenterAsync(locationDisplay.getLocation().getPosition(), zoomWidth);
                        }
                    }
                });
            locationDisplay.startAsync();
        }
    }
}