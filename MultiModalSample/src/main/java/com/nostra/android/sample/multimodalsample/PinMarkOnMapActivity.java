package com.nostra.android.sample.multimodalsample;

import android.os.StrictMode;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.loadable.LoadStatusChangedEvent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedListener;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.UserCredential;

import th.co.nostrasdk.NTSDKEnvironment;
import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;
import th.co.nostrasdk.common.NTPoint;

public class PinMarkOnMapActivity extends AppCompatActivity implements LoadStatusChangedListener {
    private MapView mapView;
    private LocationDisplay locationDisplay;
    private NTMapPermissionResult[] ntMapResults;
    private boolean isLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_mark_on_map);

        Button btnOk = findViewById(R.id.btnOk);
        Button btnCancel = findViewById(R.id.btnCancel);
        btnOk.setOnClickListener(btnOkClick);
        btnCancel.setOnClickListener(btnCancelClick);

        initialMap();
    }

    //Initialize map and current location
    private void initialMap() {
        mapView = findViewById(R.id.mapView);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy); // to run network operation on main thread
        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
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

                    UserCredential credentials = UserCredential.createFromToken(token, referrer);
                    ArcGISTiledLayer layer = new ArcGISTiledLayer(url);
                    layer.setCredential(credentials);
                    Basemap basemap = new Basemap(layer);
                    ArcGISMap arcGISMap = new ArcGISMap(basemap);
                    mapView.setMap(arcGISMap);
                    mapView.setAttributionTextVisible(false);
                    // Set listener
                    arcGISMap.addLoadStatusChangedListener(PinMarkOnMapActivity.this);
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(PinMarkOnMapActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View.OnClickListener btnCancelClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

    //Get parameter from MainActivity
    private View.OnClickListener btnOkClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int toLocationCode = 0;
            int fromLocationCode = 1;
            Point pointCenter;
            if (!isLoaded) {
                Toast.makeText(PinMarkOnMapActivity.this, "Loading Map", Toast.LENGTH_SHORT).show();
            } else if (getIntent().getExtras().getString("Location").equals("toLocation")) {
                pointCenter = mapView.getVisibleArea().getExtent().getCenter();
                getIntent().putExtra("CenterX", pointCenter.getX());
                getIntent().putExtra("CenterY", pointCenter.getY());
                getIntent().putExtra("ToLocation", toLocationCode);
            } else if (getIntent().getExtras().getString("Location").equals("fromLocation")) {
                pointCenter = mapView.getVisibleArea().getExtent().getCenter();
                getIntent().putExtra("CenterX", pointCenter.getX());
                getIntent().putExtra("CenterY", pointCenter.getY());
                getIntent().putExtra("FromLocation", fromLocationCode);
            }
            setResult(RESULT_OK, getIntent());
            finish();
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
        if ("LOADED".equals(mapLoadStatus)) {
            isLoaded = true;
            Envelope env = new Envelope(1.0672849926751213E7, 593515.9027621585,
                    1.1905414975501748E7, 2375599.5357473083, SpatialReference.create(102100));
            Viewpoint viewpoint = new Viewpoint(env);
            mapView.setViewpoint(viewpoint);
            locationDisplay = mapView.getLocationDisplay();
            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
            locationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
                boolean locationChanged = false;

                @Override
                public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
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