package com.nostra.android.sample.serviceareasample;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.Symbol;

import java.util.concurrent.ExecutionException;

import th.co.nostrasdk.NTSDKEnvironment;
import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.common.NTImpedanceMode;
import th.co.nostrasdk.common.NTLanguage;
import th.co.nostrasdk.common.NTTravelMode;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.network.NTLocation;
import th.co.nostrasdk.network.service.NTServiceAreaParameter;
import th.co.nostrasdk.network.service.NTServiceAreaResult;
import th.co.nostrasdk.network.service.NTServiceAreaResultSet;
import th.co.nostrasdk.network.service.NTServiceAreaService;

public class ServiceAreaActivity extends AppCompatActivity  {
    private MapView mapView;
    private ImageButton imbLocation;
    private RelativeLayout rllPinOption;
    private TextView txvPin, txvCancel;

    private GraphicsOverlay mGraphicsLayer, saLayer;
    private SpatialReference wgs84 = SpatialReferences.getWgs84();
    private SpatialReference webMercator = SpatialReference.create(102100);
    private int pinId = -1;
    private boolean pinOnMap;
    private double lat, lon;

    private SimpleFillSymbol smallSymbol;
    private SimpleFillSymbol mediumSymbol;
    private SimpleFillSymbol largeSymbol;

    private NTMapPermissionResult[] ntMapResults;
    private LocationManager manager;
    private LocationDisplay ldm;
    private boolean firstRun = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_area);

        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // TODO: Setting Licence ID
        ArcGISRuntimeEnvironment.setLicense("Licence_ID");

        mapView = (MapView) findViewById(R.id.mapView);
        ldm = mapView.getLocationDisplay();
        initializeMap();

        rllPinOption = (RelativeLayout) findViewById(R.id.rllPinOption);

        txvPin = (TextView) findViewById(R.id.txvPin);
        txvPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rllPinOption.setVisibility(View.GONE);
                requestServiceArea();
            }
        });

        txvCancel = (TextView) findViewById(R.id.txvCancel);
        txvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pinOnMap) {
                    mGraphicsLayer.getGraphics().remove(pinId);
                    pinOnMap = false;
                }
                rllPinOption.setVisibility(View.GONE);
            }
        });

        imbLocation = (ImageButton) findViewById(R.id.imbLocation);
        imbLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pan to current location
                Point currentPoint = ldm.getLocation().getPosition();
                if (currentPoint != null) {
                    mapView.setViewpointCenterAsync(currentPoint, 50000);
                }
            }
        });

        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            ldm = mapView.getLocationDisplay();
            ldm.setAutoPanMode(LocationDisplay.AutoPanMode.OFF);

            ldm.addLocationChangedListener(locationChangedEvent -> {
                Point location = locationChangedEvent.getLocation().getPosition();
                lat = location.getY();
                lon = location.getX();
                if (firstRun) {
                    Point p = new Point(lon, lat, wgs84);
                    mapView.setViewpointCenterAsync(p, 50000);
                    firstRun = false;
                }
            });
            ldm.startAsync();
        }
        // Symbol for the smallest service area polygon
        SimpleLineSymbol line = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 1);
        smallSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.RED, line);


        // Symbol for the medium service area polygon
        mediumSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.YELLOW, line);

        // Symbol for the largest service area polygon
        largeSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.GREEN, line);

    }

    private void initializeMap() {
        // Call map service to add map layer
        NTMapPermissionService.executeAsync(new ServiceRequestListener<NTMapPermissionResultSet>() {
            @Override
            public void onResponse(NTMapPermissionResultSet result) {
                ntMapResults = result.getResults();
                NTMapPermissionResult map = getThailandBasemap();
                if (map != null) {
                    String url = map.getLocalService().getServiceUrl();
                    String token = map.getLocalService().getServiceToken();
                    String referrer = "REFERRER";    // TODO: Insert referrer

                    UserCredential credentials = UserCredential.createFromToken(token, referrer);

                    ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer(url);
                    tiledLayer.setCredential(credentials);
                    Basemap basemap = new Basemap(tiledLayer);
                    ArcGISMap mMap = new ArcGISMap(basemap);
                    mapView.setMap(mMap);
                    mMap.addDoneLoadingListener(doneLoadingListener);

                    // Add graphic layer for add pin and service area
                    mGraphicsLayer = new GraphicsOverlay();
                    saLayer = new GraphicsOverlay();
                    mapView.getGraphicsOverlays().add(saLayer);
                    mapView.getGraphicsOverlays().add(mGraphicsLayer);
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(ServiceAreaActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });


    }

    Runnable doneLoadingListener = new Runnable() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void run() {
            mapView.setOnTouchListener(new DefaultMapViewOnTouchListener
                    (ServiceAreaActivity.this, mapView) {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (pinOnMap) {
                        mGraphicsLayer.getGraphics().remove(pinId);
                        pinOnMap = false;
                    }
                    // clear the service area
                    saLayer.getGraphics().clear();

                    if (rllPinOption.getVisibility() == View.VISIBLE) {
                        rllPinOption.setVisibility(View.GONE);
                    }

                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    if (pinOnMap) {
                        mGraphicsLayer.getGraphics().remove(pinId);
                        pinOnMap = false;
                    }
                    // clear the service area
                    saLayer.getGraphics().clear();

                    // Draw pin on map
                    Point point = screenToLocation(e.getX(), e.getY());
                    Drawable drawable = ContextCompat.getDrawable(ServiceAreaActivity.this, R.drawable.pin_markonmap);
                    ListenableFuture<PictureMarkerSymbol> pin = PictureMarkerSymbol.createAsync((BitmapDrawable) drawable);
                    try {
                        mGraphicsLayer.getGraphics().add(new Graphic(point, pin.get()));
                        pinId = mGraphicsLayer.getGraphics().size() - 1;
                        pinOnMap = true;
                        rllPinOption.setVisibility(View.VISIBLE);
                    } catch (InterruptedException | ExecutionException e1) {
                        e1.printStackTrace();
                    }


                }
            });
        }
    };

    private Point screenToLocation(Float x, Float y) {
        try {
            Point mapPoint = mapView.screenToLocation(new android.graphics.Point(Math.round(x), Math.round(y)));
            return (Point) GeometryEngine.project(mapPoint, SpatialReferences.getWgs84());
        } catch (NullPointerException e) {
            return new Point(0, 0);
        }
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

    private void requestServiceArea() {
        Point pin = (Point) mGraphicsLayer.getGraphics().get(pinId).getGeometry();
        pin = (Point) GeometryEngine.project(pin, wgs84); // Convert to WGS84

        // Setting parameter
        NTLocation[] facilities = new NTLocation[]{
                new NTLocation("facility", pin.getY(), pin.getX())
        };
        int[] breaks = new int[]{1, 3, 5};
        NTServiceAreaParameter param = new NTServiceAreaParameter(facilities, breaks);
        param.setTravelMode(NTTravelMode.CAR);
        param.setImpedance(NTImpedanceMode.TIME);
        param.setUseTollRoad(true);
        param.setLanguage(NTLanguage.LOCAL);

        // Call service
        NTServiceAreaService.executeAsync(param, new ServiceRequestListener<NTServiceAreaResultSet>() {
            @Override
            public void onResponse(NTServiceAreaResultSet result) {
                // Remove all previous result.
                saLayer.getGraphics().clear();

                if (result != null) {
                    try {
                        Envelope allEnv = null;
                        Envelope lineEnv = null;

                        NTServiceAreaResult[] results = result.getResults();
                        if (results != null && results.length > 0) {
                            for (int i = 0; i < results.length; i++) {

                                Geometry geometry = Geometry.fromJson(results[i].getShape());

                                if (geometry != null && !geometry.isEmpty()) {
                                    geometry = GeometryEngine.project(geometry,
                                            SpatialReference.create(4326));

                                    Symbol symbol = i % 3 == 0 ? largeSymbol : i % 2 == 0 ? mediumSymbol : smallSymbol;
                                    Graphic routeGraphic = new Graphic(geometry, symbol);
                                    saLayer.getGraphics().add(routeGraphic);
                                    lineEnv = geometry.getExtent();
                                        allEnv = GeometryEngine.combineExtents(lineEnv,allEnv);
                                }
                            }
                        }
                        // Zoom to the extent of the service area polygon with a padding
                        Viewpoint vp = new Viewpoint(allEnv);
                        mapView.setViewpoint(vp);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(ServiceAreaActivity.this, "No service area", Toast.LENGTH_SHORT).show();
                    if (pinOnMap) {
                        mGraphicsLayer.getGraphics().remove(pinId);
                        pinOnMap = false;
                    }
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(ServiceAreaActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onPause() {
        ldm.stop();
        mapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.resume();
        if (!ldm.isStarted())
            ldm.startAsync();
    }

    @Override
    protected void onDestroy() {
        ldm.stop();
        super.onDestroy();
    }

}