package com.nostra.android.sample.fuelsample;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.CoordinateConversion;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.io.UserCredentials;

import th.co.nostrasdk.Base.IServiceRequestListener;
import th.co.nostrasdk.Base.NTMapPermissionService;
import th.co.nostrasdk.Result.NTMapPermissionResult;
import th.co.nostrasdk.Result.NTMapPermissionResultSet;

public class MarkOnMapActivity extends AppCompatActivity implements OnStatusChangedListener {
    private MapView mapView;
    private Button btnOk;

    private Point point;
    private Point mapPoint;
    private NTMapPermissionResult[] ntMapResults;
    private LocationDisplayManager locationDisplayManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_on_map);
        mapView = (MapView) findViewById(R.id.mapView);
        btnOk = (Button) findViewById(R.id.btnOk);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                point = mapView.getCenter();
                String decimalDegrees = CoordinateConversion.pointToDecimalDegrees(
                        point, mapView.getSpatialReference(), 7);
                final Point newPoint = CoordinateConversion.decimalDegreesToPoint(decimalDegrees,
                        SpatialReference.create(SpatialReference.WKID_WGS84));

                Intent intent = new Intent(MarkOnMapActivity.this, ListResultsActivity.class);
                intent.putExtra("x", newPoint.getX());
                intent.putExtra("y", newPoint.getY());
                startActivity(intent);
            }
        });
        // Current Location
        mapView.setOnStatusChangedListener(this);
        // Initialize map
        initialMap();
    }

    // Add map
    private void initialMap() {
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
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MarkOnMapActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

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
    public void onStatusChanged(Object source, STATUS status) {
        if (source == mapView && status == OnStatusChangedListener.STATUS.INITIALIZED) {
            locationDisplayManager = mapView.getLocationDisplayManager();
            locationDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
            locationDisplayManager.setLocationListener(new LocationListener() {
                boolean locationChanged = false;

                // Zooms to the current location when first GPS fix arrives.
                @Override
                public void onLocationChanged(Location loc) {
                    if (!locationChanged) {
                        locationChanged = true;
                        double locY = loc.getLatitude();
                        double locX = loc.getLongitude();
                        Point wgsPoint = new Point(locX, locY);
                        mapPoint = (Point) GeometryEngine.project(wgsPoint,SpatialReference.create(4326),
                                mapView.getSpatialReference());

                        Unit mapUnit = mapView.getSpatialReference().getUnit();
                        double zoomWidth = Unit.convertUnits(5,
                                Unit.create(LinearUnit.Code.MILE_US),
                                mapUnit);
                        Envelope zoomExtent = new Envelope(mapPoint,zoomWidth, zoomWidth);
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
            locationDisplayManager.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.unpause();
        locationDisplayManager.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.pause();
        locationDisplayManager.pause();
    }
}