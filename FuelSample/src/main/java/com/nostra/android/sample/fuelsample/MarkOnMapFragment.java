package com.nostra.android.sample.fuelsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.UserCredential;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;

public class MarkOnMapFragment extends Fragment {
    private MapView mMapView;

    private Point point = null;
    private Point mapPoint;
    private NTMapPermissionResult[] ntMapResults;
    private LocationDisplay locationDisplay;
    private boolean locationChanged = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mark_map, container, false);
        mMapView = (MapView) view.findViewById(R.id.mapView);
        Button btnOk = (Button) view.findViewById(R.id.btnOk);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Polygon visibleArea = mMapView.getVisibleArea();
                Envelope extent = visibleArea.getExtent();
                point = extent.getCenter();

                if (visibleArea != null && extent != null && point != null) {
                    Point wgsPoint = (Point) GeometryEngine.project(point, SpatialReferences.getWgs84());
                    Intent intent = new Intent(getActivity(), ListResultsActivity.class);
                    intent.putExtra("x", wgsPoint.getX());
                    intent.putExtra("y", wgsPoint.getY());
                    startActivity(intent);
                } else {
                    Toast.makeText(requireContext(), "Map is not ready", Toast.LENGTH_SHORT).show();
                }

            }
        });
        // Initialize map
        initialMap();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.resume();
        if (locationDisplay!=null && !locationDisplay.isStarted()) {
            locationDisplay.startAsync();
        }
    }

    @Override
    public void onPause() {
        mMapView.pause();
        if (locationDisplay != null) {
            locationDisplay.stop();
        }
        super.onPause();
    }

    // Add map
    private void initialMap() {
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

                    UserCredential credentials =  UserCredential.createFromToken(token,referrer);

                    ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer(url);
                    tiledLayer.setCredential(credentials);
                    Basemap baseMap = new Basemap(tiledLayer);

                    Envelope env = new Envelope(
                            1.0672849926751213E7,
                            593515.9027621585,
                            1.1905414975501748E7,
                            2375599.5357473083,
                            SpatialReference.create(102100)
                    );
                    Viewpoint vp = new Viewpoint(env);
                    ArcGISMap mMap = new ArcGISMap(baseMap);
                    mMap.setInitialViewpoint(vp);
                    mMap.addDoneLoadingListener(doneLoadingListener);
                    mMapView.setMap(mMap);
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Runnable doneLoadingListener = new Runnable() {
        @Override
        public void run() {
            locationDisplay = mMapView.getLocationDisplay();
            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.OFF);
            locationDisplay.addLocationChangedListener(locationChangedEvent -> {
                // Zooms to the current location when first GPS fix arrives.
                if (!locationChanged) {
                    locationChanged = true;
                    Point loc = locationChangedEvent.getLocation().getPosition();
                    double locY = loc.getY();
                    double locX = loc.getX();
                    Point wgsPoint = new Point(locX, locY);
                    mapPoint = (Point) GeometryEngine.project(
                            wgsPoint, SpatialReferences.getWgs84());

                    mMapView.setViewpointCenterAsync(mapPoint, 5000);
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
