package com.nostra.android.sample.routesample;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.io.UserCredentials;

import th.co.nostrasdk.Base.IServiceRequestListener;
import th.co.nostrasdk.Base.NTMapPermissionService;
import th.co.nostrasdk.Base.NTSDKEnvironment;
import th.co.nostrasdk.Result.NTMapPermissionResult;
import th.co.nostrasdk.Result.NTMapPermissionResultSet;

public class PinMarkerActivity extends AppCompatActivity implements OnStatusChangedListener {
    private MapView mapView;
    private NTMapPermissionResult[] ntMapResults;
    private LocationDisplayManager ldm;
    private GraphicsLayer pinGraphicLayer = new GraphicsLayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pinmarker);

        // Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.enableWrapAround(true);

        ldm = mapView.getLocationDisplayManager();
        ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);

        //Add map and current location
        NTMapPermissionService.executeAsync(new IServiceRequestListener<NTMapPermissionResultSet>() {
            @Override
            public void onResponse(NTMapPermissionResultSet result, String responseCode) {
                ntMapResults = result.getResults();
                NTMapPermissionResult map = getThailandBasemap();
                if (map != null) {
                    String url = map.getServiceUrl_L();
                    String token = map.getServiceToken_L();
                    String referrer = "Referrer";    // TODO: Insert referrer

                    UserCredentials credentials = new UserCredentials();
                    credentials.setUserToken(token, referrer);
                    credentials.setAuthenticationType(UserCredentials.AuthenticationType.TOKEN);

                    ArcGISTiledMapServiceLayer layer = new ArcGISTiledMapServiceLayer(url, credentials);
                    mapView.addLayer(layer);
                    mapView.addLayer(pinGraphicLayer);
                    mapView.setOnStatusChangedListener(PinMarkerActivity.this);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(PinMarkerActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        // Send parameter to RouteActivity
        Button btnOk = (Button) findViewById(R.id.btnOk);
        btnOk.setOnClickListener(btnOkClickListener);

        final Button btnCancel = (Button) findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pinGraphicLayer.removeAll();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        ldm.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ldm.resume();
    }

    @Override
    public void onStatusChanged(Object source, STATUS status) {
        if (source == mapView && status == STATUS.INITIALIZED) {
            ldm.setLocationListener(new LocationListener() {
                boolean locationChanged = false;

                // Zooms to the current location when first GPS fix arrives.
                @Override
                public void onLocationChanged(Location loc) {
                    if (!locationChanged) {
                        locationChanged = true;

                        double locY = loc.getLatitude();
                        double locX = loc.getLongitude();
                        Point wgsPoint = new Point(locX, locY);
                        Point mapPoint = (Point) GeometryEngine
                                .project(wgsPoint,
                                        SpatialReference.create(4326),
                                        mapView.getSpatialReference());

                        Unit mapUnit = mapView.getSpatialReference().getUnit();
                        double zoomWidth = Unit.convertUnits(5,
                                Unit.create(LinearUnit.Code.MILE_US), mapUnit);
                        Envelope zoomExtent = new Envelope(mapPoint, zoomWidth, zoomWidth);
                        mapView.setExtent(zoomExtent);
                    }
                }

                @Override
                public void onProviderDisabled(String arg0) {
                }

                @Override
                public void onProviderEnabled(String arg0) {
                }

                @Override
                public void onStatusChanged(String arg0, int arg1, Bundle arg2) {

                }
            });
            ldm.start();
        }
    }

    private View.OnClickListener btnOkClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mapView.isLoaded()) {
                Point pointCenter = mapView.getCenter();

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
}