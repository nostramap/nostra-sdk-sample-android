package com.nostra.android.sample.closestfacilitysample;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
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
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
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
import com.esri.arcgisruntime.symbology.CompositeSymbol;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.Symbol;
import com.esri.arcgisruntime.symbology.TextSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import th.co.nostrasdk.NTSDKEnvironment;
import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.common.NTImpedanceMode;
import th.co.nostrasdk.common.NTLanguage;
import th.co.nostrasdk.common.NTSpatialReference;
import th.co.nostrasdk.common.NTTravelMode;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;
import th.co.nostrasdk.network.NTLocation;
import th.co.nostrasdk.network.facility.NTClosestFacilityParameter;
import th.co.nostrasdk.network.facility.NTClosestFacilityResult;
import th.co.nostrasdk.network.facility.NTClosestFacilityResultSet;
import th.co.nostrasdk.network.facility.NTClosestFacilityService;
import th.co.nostrasdk.network.facility.NTFacilityDirection;

public class ClosestFacilityActivity extends AppCompatActivity {

    private MapView mMapView;
    private RelativeLayout mPinOptionLayout;
    private TextView mTxvPin;
    private TextView mTxvCancel;
    private ImageButton mBtnLocation;
    private NTMapPermissionResult[] mMapPermissions;

    private GraphicsOverlay mGraphicsOverlay;
    private GraphicsOverlay mRouteOverlay;
    private SpatialReference wgs84 = SpatialReferences.getWgs84();
    private int pinId = -1;
    private boolean pinOnMap;

    private LocationDisplay mLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closest_facility);

        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);

        // TODO: Setting Licence ID
        ArcGISRuntimeEnvironment.setLicense("Licence_ID");

        mMapView = (MapView) findViewById(R.id.mapView);
        mPinOptionLayout = (RelativeLayout) findViewById(R.id.rllPinOption);

        mTxvPin = (TextView) findViewById(R.id.txvPin);
        mTxvPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPinOptionLayout.setVisibility(View.GONE);
                callService();
            }
        });

        mTxvCancel = (TextView) findViewById(R.id.txvCancel);
        mTxvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pinOnMap) {
                    mGraphicsOverlay.getGraphics().remove(pinId);
                    pinOnMap = false;
                }
                mPinOptionLayout.setVisibility(View.GONE);
            }
        });

        mBtnLocation = (ImageButton) findViewById(R.id.imbLocation);
        mBtnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pan to current location
                Point locationPoint = mLocationManager.getMapLocation();
                if (locationPoint != null) {
                    mMapView.setViewpointCenterAsync(locationPoint);
                }
            }
        });

        // Setup location manager.
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mLocationManager = mMapView.getLocationDisplay();
            mLocationManager.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
            mLocationManager.startAsync();
        }
        initializeMap();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initializeMap() {
        NTMapPermissionService.executeAsync(new ServiceRequestListener<NTMapPermissionResultSet>() {
            @Override
            public void onResponse(NTMapPermissionResultSet result) {
                mMapPermissions = result.getResults();
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
                    Basemap baseMap = new Basemap(tiledLayer);
                    ArcGISMap mMap = new ArcGISMap(baseMap);
                    mMapView.setMap(mMap);
                    mMap.addDoneLoadingListener(doneLoadingListener);
                }
                // Add graphic layer for add pin and route
                mRouteOverlay = new GraphicsOverlay();
                mGraphicsOverlay = new GraphicsOverlay();

                mMapView.getGraphicsOverlays().add(mRouteOverlay);
                mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(ClosestFacilityActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (pinOnMap) {
                    mGraphicsOverlay.getGraphics().remove(pinId);
                    pinOnMap = false;
                }
                // clear all routes
                mRouteOverlay.getGraphics().clear();

                if (mPinOptionLayout.getVisibility() == View.VISIBLE) {
                    mPinOptionLayout.setVisibility(View.GONE);
                }

                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (pinOnMap) {
                    mGraphicsOverlay.getGraphics().remove(pinId);
                    pinOnMap = false;
                }
                // Clear all routes
                mRouteOverlay.getGraphics().clear();
                // Draw pin on map
                Point screenPoint = screenToLocation(e.getX(), e.getY());
                Point p = new Point(screenPoint.getX(), screenPoint.getY(), wgs84);
                Drawable pinDrawable = ResourcesCompat.getDrawable(
                        getResources(), R.drawable.pin_markonmap, getTheme());


                ListenableFuture<PictureMarkerSymbol> symbol =
                        PictureMarkerSymbol.createAsync((BitmapDrawable) pinDrawable);

                try {
                    mGraphicsOverlay.getGraphics().add(new Graphic(p, symbol.get()));
                    pinId = mGraphicsOverlay.getGraphics().size() - 1;
                    pinOnMap = true;
                    mPinOptionLayout.setVisibility(View.VISIBLE);
                } catch (ExecutionException e1) {
                    e1.printStackTrace();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

            }
        });
    }

    private Point screenToLocation(Float x, Float y) {
        try {
            Point mapPoint = mMapView.screenToLocation(new android.graphics.Point(Math.round(x), Math.round(y)));
            return (Point) GeometryEngine.project(mapPoint, SpatialReferences.getWgs84());
        } catch (NullPointerException e) {
            return new Point(0, 0);
        }
    }

    private void callService() {
        // Get pin point location
        Point pinPoint = (Point) mGraphicsOverlay.getGraphics().get(pinId).getGeometry();
        pinPoint = (Point) GeometryEngine.project(pinPoint, wgs84); // Convert to WGS84

        // Setting parameter
        NTLocation[] facilities = new NTLocation[]{
                new NTLocation("MBK", 13.744651781616076, 100.52989481845307),
                new NTLocation("Siam Discovery", 13.746598089591219, 100.53145034771327),
                new NTLocation("Siam Center", 13.746321783330115, 100.53279034433699),
                new NTLocation("Siam Paragon", 13.746155248206387, 100.53481456769379)
        };
        NTLocation incident = new NTLocation("incident", pinPoint.getY(), pinPoint.getX());
        NTClosestFacilityParameter parameter = new NTClosestFacilityParameter(facilities, incident);
        parameter.setTravelMode(NTTravelMode.CAR);
        parameter.setImpedance(NTImpedanceMode.TIME);
        parameter.setTravelDirection(NTFacilityDirection.TO_FACILITY);
        parameter.setTargetFacilityCount(4);
        parameter.setOutSpatialReference(NTSpatialReference.WEB_MERCATOR);
        parameter.setLanguage(NTLanguage.LOCAL);

        // Call service
        NTClosestFacilityService.executeAsync(parameter, new ServiceRequestListener<NTClosestFacilityResultSet>() {
            @Override
            public void onResponse(NTClosestFacilityResultSet result) {
                // If successful
                NTClosestFacilityResult[] results = result.getResults();
                if (results != null) {
                    SimpleLineSymbol lineSymbol;
                    for (NTClosestFacilityResult cfr : results) {
                        String name = TextUtils.isEmpty(cfr.getFacilityName()) ? "" : cfr.getFacilityName();
                        if (name.endsWith("Siam Discovery")) {
                            lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 5);
                        } else if (name.endsWith("Siam Center")) {
                            lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 5);
                        } else if (name.endsWith("Siam Paragon")) {
                            lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.MAGENTA, 5);
                        } else {
                            lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.GREEN, 5);
                        }
                        if (cfr.getShape() != null) {
                            Geometry geometry = Geometry.fromJson(cfr.getShape());

                            if (geometry != null && !geometry.isEmpty()) {
                                geometry = GeometryEngine.project(geometry, wgs84);
                                Graphic routeGraphic = new Graphic(geometry, lineSymbol);
                                mRouteOverlay.getGraphics().add(routeGraphic);
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                // If error
                Toast.makeText(ClosestFacilityActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private NTMapPermissionResult getThailandBasemap() {
        for (NTMapPermissionResult result : mMapPermissions) {
            // Thailand basemap service id is 2
            if (result.getServiceId() == 2) {
                return result;
            }
        }
        return null;
    }

    Runnable doneLoadingListener = new Runnable() {
        @Override
        public void run() {
            // Zoom to these 4 places
            Point mbk = new Point(100.52989481845307, 13.744651781616076, wgs84);

            Point siamDis = new Point(100.53145034771327, 13.746598089591219, wgs84);
            Point siamCenter = new Point(100.53279034433699, 13.746321783330115, wgs84);
            Point siamParagon = new Point(100.53481456769379, 13.746155248206387, wgs84);

            // Create circle with border symbol
            SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.WHITE, 50);
            symbol.setOutline(new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 5));

            // Create number symbol
            TextSymbol textSymbol = new TextSymbol(25, "1", Color.BLACK,
                    TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.MIDDLE);
            TextSymbol textSymbol2 = new TextSymbol(25, "2", Color.BLACK,
                    TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.MIDDLE);
            TextSymbol textSymbol3 = new TextSymbol(25, "3", Color.BLACK,
                    TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.MIDDLE);
            TextSymbol textSymbol4 = new TextSymbol(25, "4", Color.BLACK,
                    TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.MIDDLE);
            // Combine 2 symbol and add to map view
            // 1
            List<Symbol> symbolList = new ArrayList<>();
            symbolList.add(symbol);
            symbolList.add(textSymbol);
            CompositeSymbol compositeSymbol = new CompositeSymbol(symbolList);

            mGraphicsOverlay.getGraphics().add(new Graphic(mbk, compositeSymbol));

            // 2
            symbolList.clear();
            symbolList.add(symbol);
            symbolList.add(textSymbol2);
            compositeSymbol = new CompositeSymbol(symbolList);
            mGraphicsOverlay.getGraphics().add(new Graphic(siamDis, compositeSymbol));

            // 3
            symbolList.clear();
            symbolList.add(symbol);
            symbolList.add(textSymbol3);
            compositeSymbol = new CompositeSymbol(symbolList);
            mGraphicsOverlay.getGraphics().add(new Graphic(siamCenter, compositeSymbol));

            // 4
            symbolList.clear();
            symbolList.add(symbol);
            symbolList.add(textSymbol4);
            compositeSymbol = new CompositeSymbol(symbolList);
            mGraphicsOverlay.getGraphics().add(new Graphic(siamParagon, compositeSymbol));

            // merge all point to envelope
           PointCollection collection = new PointCollection(wgs84);
            collection.add(mbk);
            collection.add(siamCenter);
            collection.add(siamDis);
            collection.add(siamParagon);
            Polyline polyline = new Polyline(collection);
            Point center =  polyline.getExtent().getCenter();
            Double width =  polyline.getExtent().getWidth();
            Double height =  polyline.getExtent().getHeight();
            Envelope envelope = new Envelope(center, width, height);
            Viewpoint viewpoint = new Viewpoint(envelope);
            // zoom to the envelope

            mMapView.setViewpointAsync(viewpoint, 0.2f);
        }
    };

    @Override
    protected void onPause() {
        mLocationManager.stop();
        mMapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.resume();
        if (!mLocationManager.isStarted()) {
            mLocationManager.startAsync();
        }
    }

    @Override
    protected void onDestroy() {
        mLocationManager.stop();
        super.onDestroy();
    }
}