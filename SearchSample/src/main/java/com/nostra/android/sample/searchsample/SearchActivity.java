package com.nostra.android.sample.searchsample;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.UserCredential;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.common.NTPoint;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;

public class SearchActivity extends AppCompatActivity {
    private MapView mapView;
    private NTMapPermissionResult[] ntMapResults;
    private Point point;
    private double lat;
    private double lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mapView = (MapView) findViewById(R.id.mapView);
        initialMap();

        final ImageView imvCurrentlocation = (ImageView) findViewById(R.id.imvCurrentLocation);
        imvCurrentlocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.setViewpointCenterAsync(point);
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
        NTMapPermissionService.executeAsync(new ServiceRequestListener<NTMapPermissionResultSet>() {
            @Override
            public void onResponse(NTMapPermissionResultSet result) {
                ntMapResults = result.getResults();
                NTMapPermissionResult mapPermission = getThailandBasemap();
                if (mapPermission != null) {
                    NTMapServiceInfo info = mapPermission.getLocalService();
                    String url = info.getServiceUrl();
                    String token = info.getServiceToken();
                    String referrer = "REFERRER";    // TODO: Insert referrer

                    UserCredential credentials = UserCredential.createFromToken(token, referrer);

                    ArcGISTiledLayer layer = new ArcGISTiledLayer(url);
                    layer.setCredential(credentials);
                    ArcGISMap arcMap = new ArcGISMap(new Basemap(layer));

                    mapView.setMap(arcMap);
                    arcMap.addDoneLoadingListener(doneLoadingListener);
                    NTPoint ntPoint = mapPermission.getDefaultLocation();
                    if (ntPoint != null) {
                        lon = mapPermission.getDefaultLocation().getX();
                        lat = mapPermission.getDefaultLocation().getY();
                    }
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(SearchActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.resume();
    }

    @Override
    protected void onPause() {
        mapView.pause();
        super.onPause();
    }

    private Runnable doneLoadingListener = new Runnable() {
        @Override
        public void run() {
            LocationDisplay locationDisplay = mapView.getLocationDisplay();
            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
            locationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
                boolean locationChanged = false;

                @Override
                public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
                    Point loc = locationChangedEvent.getLocation().getPosition();
                    if (!locationChanged) {
                        locationChanged = true;

                        double locY = loc.getY();
                        double locX = loc.getX();
                        Point wgsPoint = new Point(locX, locY, SpatialReference.create(4326));
                        mapView.setViewpointCenterAsync(wgsPoint, 50000);

                    }
                    lat = loc.getY();
                    lon = loc.getX();
                }
            });
            locationDisplay.startAsync();

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