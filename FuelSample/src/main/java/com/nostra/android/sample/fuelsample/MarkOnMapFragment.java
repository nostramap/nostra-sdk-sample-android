package com.nostra.android.sample.fuelsample;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.io.UserCredentials;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;

public class MarkOnMapFragment extends Fragment implements OnStatusChangedListener {
    private MapView mMapView;

    private Point point;
    private Point mapPoint;
    private NTMapPermissionResult[] ntMapResults;
    private LocationDisplayManager locationDisplayManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mark_map, container, false);
        mMapView = (MapView) view.findViewById(R.id.mapView);
        Button btnOk = (Button) view.findViewById(R.id.btnOk);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                point = mMapView.getCenter();
                Point wgsPoint = (Point) GeometryEngine.project(point,
                        SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE),
                        SpatialReference.create(SpatialReference.WKID_WGS84));

                Intent intent = new Intent(getActivity(), ListResultsActivity.class);
                intent.putExtra("x", wgsPoint.getX());
                intent.putExtra("y", wgsPoint.getY());
                startActivity(intent);
            }
        });
        // Current Location
        mMapView.setOnStatusChangedListener(this);
        // Initialize map
        initialMap();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.unpause();
        if (locationDisplayManager != null) {
            locationDisplayManager.resume();
        }
    }

    @Override
    public void onPause() {
        mMapView.pause();
        if (locationDisplayManager != null) {
            locationDisplayManager.pause();
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

                    UserCredentials credentials = new UserCredentials();
                    credentials.setUserToken(token, referrer);
                    credentials.setAuthenticationType(UserCredentials.AuthenticationType.TOKEN);

                    ArcGISTiledMapServiceLayer layer = new ArcGISTiledMapServiceLayer(url, credentials);
                    mMapView.addLayer(layer);
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
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
        if (source == mMapView && status == OnStatusChangedListener.STATUS.INITIALIZED) {
            locationDisplayManager = mMapView.getLocationDisplayManager();
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
                        mapPoint = (Point) GeometryEngine.project(
                                wgsPoint, SpatialReference.create(4326),
                                mMapView.getSpatialReference());

                        Unit mapUnit = mMapView.getSpatialReference().getUnit();
                        double zoomWidth = Unit.convertUnits(5,
                                Unit.create(LinearUnit.Code.MILE_US),
                                mapUnit);
                        Envelope zoomExtent = new Envelope(mapPoint, zoomWidth, zoomWidth);
                        mMapView.setExtent(zoomExtent);
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
}