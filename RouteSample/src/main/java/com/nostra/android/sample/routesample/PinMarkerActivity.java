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

import th.co.nostrasdk.NTSDKEnvironment;
import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;

public class PinMarkerActivity extends AppCompatActivity implements OnStatusChangedListener {
    private MapView mapView;
    private NTMapPermissionResult[] ntMapResults;
    private LocationDisplayManager ldm;
    private GraphicsLayer pinGraphicLayer = new GraphicsLayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pinmarker);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.enableWrapAround(true);

        ldm = mapView.getLocationDisplayManager();
        ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);

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
            public void onError(String errorMessage,int statusCode) {
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