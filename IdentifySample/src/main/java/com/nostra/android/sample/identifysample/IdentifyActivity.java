package com.nostra.android.sample.identifysample;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.common.NTPoint;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;
import th.co.nostrasdk.query.poi.NTIdentifyParameter;
import th.co.nostrasdk.query.poi.NTIdentifyResult;
import th.co.nostrasdk.query.poi.NTIdentifyService;

public class IdentifyActivity extends AppCompatActivity {

    private MapView mapView;
    private NTMapPermissionResult[] ntMapResults;
    private Point point;
    private GraphicsOverlay graphicsOverlay;
    private Callout mapCallout;
    private Point mapPoint;
    private Double lat;
    private Double lon;
    private LocationDisplay locationDisplay;
    private Boolean locationChanged = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify);

        mapView = (MapView) findViewById(R.id.mapView);

        graphicsOverlay = new GraphicsOverlay();
        // Zoom to current location
        ImageView imvCurrentLocation = (ImageView) findViewById(R.id.imvCurrentLocation);
        imvCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.setViewpointCenterAsync(mapPoint, 50000);
            }
        });
        initialMap();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.pause();
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

                    UserCredential credentials = UserCredential.createFromToken(token, referrer);

                    ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer(url);
                    tiledLayer.setCredential(credentials);
                    Basemap basemap = new Basemap(tiledLayer);
                    ArcGISMap mMap = new ArcGISMap(basemap);
                    Envelope env = new Envelope(
                            1.0672849926751213E7,
                            593515.9027621585,
                            1.1905414975501748E7,
                            2375599.5357473083,
                            SpatialReference.create(102100)
                    );
                    Viewpoint vp = new Viewpoint(env);
                    mMap.setInitialViewpoint(vp);
                    mMap.addDoneLoadingListener(doneLoadingListener);
                    mapView.setMap(mMap);

                    NTPoint ntPoint = map.getDefaultLocation();
                    if (ntPoint != null) {
                        lat = ntPoint.getY();
                        lon = ntPoint.getX();
                        point = new Point(lon, lat, SpatialReferences.getWgs84());
                        mapView.setViewpointCenterAsync(point, 50000);
                        mapView.getGraphicsOverlays().add(graphicsOverlay);
                    }
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(IdentifyActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Runnable doneLoadingListener = new Runnable() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void run() {
            locationDisplay = mapView.getLocationDisplay();
            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.OFF);
            locationDisplay.addLocationChangedListener(locationChangedEvent -> {
                if (!locationChanged) {
                    locationChanged = true;
                    Point location = locationDisplay.getLocation().getPosition();
                    double locationY = location.getY();
                    double locationX = location.getX();
                    mapPoint = new Point(locationX, locationY, SpatialReferences.getWgs84());
                    mapView.setViewpointCenterAsync(mapPoint, 50000);
                }
            });
            locationDisplay.startAsync();
            mapView.setOnTouchListener(new DefaultMapViewOnTouchListener(IdentifyActivity.this, mapView) {

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (mapCallout != null && mapCallout.isShowing()) {
                        mapCallout.dismiss();
                        graphicsOverlay.getGraphics().clear();
                    } else {
                        Toast.makeText(IdentifyActivity.this, "Select location", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    graphicsOverlay.getGraphics().clear();
                    if (mapCallout != null && mapCallout.isShowing()) {
                        mapCallout.dismiss();
                    }

                    point = screenToLocation(e.getX(), e.getY());
                    Drawable drawable = ContextCompat.getDrawable(IdentifyActivity.this, R.drawable.pin_markonmap);
                    ListenableFuture<PictureMarkerSymbol> markerSymbol = PictureMarkerSymbol.createAsync((BitmapDrawable) drawable);

                    // Setting parameter
                    NTPoint pointParam = new NTPoint(point.getX(), point.getY());
                    NTIdentifyParameter param = new NTIdentifyParameter(pointParam);
                    // Call NTIdentifyService and show callout
                    NTIdentifyService.executeAsync(param, new ServiceRequestListener<NTIdentifyResult>() {
                        @Override
                        public void onResponse(NTIdentifyResult result) {
                            String nameL = result.getLocalName();
                            String nostraId = result.getNostraId();
                            String adminLevel1L = result.getAdminLevel1().getLocalName();
                            String adminLevel2L = result.getAdminLevel2().getLocalName();
                            String adminLevel3L = result.getAdminLevel3().getLocalName();
                            String adminLevel4L = result.getAdminLevel4().getLocalName();

                            Map<String, Object> attr = new HashMap<>();
                            attr.put("nameL", nameL);
                            attr.put("nostraId", nostraId);
                            attr.put("adminLevel4L", adminLevel4L);
                            attr.put("adminLevel3L", adminLevel3L);
                            attr.put("adminLevel2L", adminLevel2L);
                            attr.put("adminLevel1L", adminLevel1L);

                            try {
                                Graphic graphic = new Graphic(point, attr, markerSymbol.get());
                                graphicsOverlay.getGraphics().add(graphic);
                            } catch (InterruptedException | ExecutionException e1) {
                                e1.printStackTrace();
                            }

                            mapCallout = mapView.getCallout();

                            // Sets custom content view to Callout
                            mapCallout.setContent(getCalloutView(nameL, nostraId,
                                    adminLevel4L, adminLevel3L, adminLevel2L, adminLevel1L));
                            mapCallout.setLocation(point);
                            mapCallout.show();
                        }

                        @Override
                        public void onError(String error, int statusCode) {
                            Toast.makeText(IdentifyActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
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

    //Set content in callout
    private View getCalloutView(String name,
                                final String nostraId,
                                String adminLevel4L,
                                String adminLevel3L,
                                String adminLevel2L,
                                String adminLevel1L) {
        View view = LayoutInflater.from(IdentifyActivity.this).inflate(R.layout.callout, null);
        TextView txvNameL = (TextView) view.findViewById(R.id.txvNameL);
        TextView txvLocation = (TextView) view.findViewById(R.id.txvLocation);
        ImageView imvPinOnMap = (ImageView) view.findViewById(R.id.imvPinOnMap);
        Button btnDetail = (Button) view.findViewById(R.id.btnDetail);

        txvNameL.setText(name);
        txvLocation.setText(adminLevel4L + " " + adminLevel3L + " " + "\n" + adminLevel2L + " " + adminLevel1L);
        imvPinOnMap.setImageDrawable(IdentifyActivity.this.getResources().getDrawable(R.drawable.pin_markonmap));
        btnDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(IdentifyActivity.this, DetailActivity.class);
                intent.putExtra("nostraId", nostraId);
                startActivity(intent);
            }
        });
        if (nostraId == null || nostraId.length() == 0) {
            btnDetail.setVisibility(View.GONE);
        }
        return view;
    }

    private Point screenToLocation(Float x, Float y) {
        try {
            Point mapPoint = mapView.screenToLocation(new android.graphics.Point(Math.round(x), Math.round(y)));
            return (Point) GeometryEngine.project(mapPoint, SpatialReferences.getWgs84());
        } catch (NullPointerException e) {
            return new Point(0, 0);
        }
    }

}