package com.nostra.android.sample.searchsample;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

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
import th.co.nostrasdk.Parameter.Class.NTPoint;
import th.co.nostrasdk.Result.NTMapPermissionResult;
import th.co.nostrasdk.Result.NTMapPermissionResultSet;

public class SearchActivity extends AppCompatActivity implements OnStatusChangedListener {
    private MapView mapView;
    private NTMapPermissionResult[] ntMapResults;
    private Point point;
    private double lat;
    private double lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setOnStatusChangedListener(this);
        initialMap();

        final ImageView imvCurrentlocation = (ImageView) findViewById(R.id.imvCurrentLocation);
        imvCurrentlocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.centerAt(point, true);
            }
        });

        EditText edtSearch = (EditText) findViewById(R.id.edtSearch);
        edtSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SearchActivity.this, TabHostActivity.class);
                intent.putExtra("lon", lon);
                intent.putExtra("lat", lat);
                startActivity(intent);
            }
        });

        ImageView imvSearch = (ImageView) findViewById(R.id.imvSearch);
        imvSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SearchActivity.this, TabHostActivity.class);
                intent.putExtra("lon", lon);
                intent.putExtra("lat", lat);
                startActivity(intent);
            }
        });
    }

    // Add map and current location
    private void initialMap() {
        NTMapPermissionService.executeAsync(new IServiceRequestListener<NTMapPermissionResultSet>() {
            @Override
            public void onResponse(NTMapPermissionResultSet result, String responseCode) {
                ntMapResults = result.getResults();
                NTMapPermissionResult mapPermission = getThailandBasemap();
                if (mapPermission != null) {
                    String url = mapPermission.getServiceUrl_L();
                    String token = mapPermission.getServiceToken_L();
                    String referrer = "Referrer";    // TODO: Insert referrer

                    UserCredentials credentials = new UserCredentials();
                    credentials.setUserToken(token, referrer);
                    credentials.setAuthenticationType(UserCredentials.AuthenticationType.TOKEN);

                    ArcGISTiledMapServiceLayer layer = new ArcGISTiledMapServiceLayer(url, credentials);
                    mapView.addLayer(layer);

                    NTPoint ntPoint = mapPermission.getDefaultZoom();
                    if (ntPoint != null) {
                        lon = mapPermission.getDefaultZoom().getX();
                        lat = mapPermission.getDefaultZoom().getY();
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(SearchActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
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
            LocationDisplayManager locationManager = mapView.getLocationDisplayManager();
            locationManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
            locationManager.setLocationListener(new LocationListener() {
                boolean locationChanged = false;

                // Zooms to the current location when first GPS fix arrives.
                @Override
                public void onLocationChanged(Location loc) {
                    if (!locationChanged) {
                        locationChanged = true;
                        double locY = loc.getLatitude();
                        double locX = loc.getLongitude();
                        Point wgsPoint = new Point(locX, locY);
                        point = (Point) GeometryEngine.project(wgsPoint,
                                SpatialReference.create(4326),
                                mapView.getSpatialReference());

                        Unit mapUnit = mapView.getSpatialReference().getUnit();
                        double zoomWidth = Unit.convertUnits(5, Unit.create(LinearUnit.Code.MILE_US),
                                mapUnit);
                        Envelope zoomExtent = new Envelope(point, zoomWidth, zoomWidth);
                        mapView.setExtent(zoomExtent);
                    }
                    lat = loc.getLatitude();
                    lon = loc.getLongitude();
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
            locationManager.start();
        }
    }
}