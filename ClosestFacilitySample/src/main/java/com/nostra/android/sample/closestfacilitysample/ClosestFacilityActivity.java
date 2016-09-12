package com.nostra.android.sample.closestfacilitysample;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnLongPressListener;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.CompositeSymbol;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;

import java.io.IOException;

import th.co.nostrasdk.Base.IServiceRequestListener;
import th.co.nostrasdk.Base.NTClosestFacilityService;
import th.co.nostrasdk.Base.NTMapPermissionService;
import th.co.nostrasdk.Base.NTSDKEnvironment;
import th.co.nostrasdk.Parameter.Class.NTLocation;
import th.co.nostrasdk.Parameter.Class.NTSpatialReference;
import th.co.nostrasdk.Parameter.Constant.NTCountry;
import th.co.nostrasdk.Parameter.Constant.NTFacilityDirection;
import th.co.nostrasdk.Parameter.Constant.NTImpedanceMode;
import th.co.nostrasdk.Parameter.Constant.NTLanguage;
import th.co.nostrasdk.Parameter.Constant.NTTravelMode;
import th.co.nostrasdk.Parameter.NTClosestFacilityParameter;
import th.co.nostrasdk.Result.NTClosestFacilityResult;
import th.co.nostrasdk.Result.NTClosestFacilityResultSet;
import th.co.nostrasdk.Result.NTMapPermissionResult;
import th.co.nostrasdk.Result.NTMapPermissionResultSet;

public class ClosestFacilityActivity extends AppCompatActivity
        implements OnStatusChangedListener, OnSingleTapListener, OnLongPressListener {
    private MapView mapView;
    private RelativeLayout rllPinOption;
    private TextView txvPin, txvCancel;
    private ImageButton imbLocation;
    private NTMapPermissionResult[] ntMapResults;

    private GraphicsLayer mGraphicsLayer, routeLayer;
    private SpatialReference inSR = SpatialReference.create(SpatialReference.WKID_WGS84);
    private SpatialReference outSR = SpatialReference.create(
            SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE);
    private int pinId = -1;
    private boolean pinOnMap;

    private LocationDisplayManager ldm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closest_facility);

        // Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

        mapView = (MapView) findViewById(R.id.mapView);
        rllPinOption = (RelativeLayout) findViewById(R.id.rllPinOption);

        txvPin = (TextView) findViewById(R.id.txvPin);
        txvPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rllPinOption.setVisibility(View.GONE);
                callService();
            }
        });

        txvCancel = (TextView) findViewById(R.id.txvCancel);
        txvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pinOnMap) {
                    mGraphicsLayer.removeGraphic(pinId);
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
                Point locationPoint = ldm.getPoint();
                if (locationPoint != null) {
                    mapView.centerAt(locationPoint, true);
                }
            }
        });

        // Setup location manager.
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            ldm = mapView.getLocationDisplayManager();
            ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
            ldm.start();
        }
        initializeMap();
    }

    private void initializeMap() {
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
                // Add graphic layer for add pin and route
                routeLayer = new GraphicsLayer();
                mapView.addLayer(routeLayer);

                mGraphicsLayer = new GraphicsLayer();
                mapView.addLayer(mGraphicsLayer);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ClosestFacilityActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        mapView.setOnStatusChangedListener(this);
        mapView.setOnSingleTapListener(this);
        mapView.setOnLongPressListener(this);
    }

    private void callService() {
        // Get pin point location
        Point pinPoint = (Point) mGraphicsLayer.getGraphic(pinId).getGeometry();
        pinPoint = (Point) GeometryEngine.project(pinPoint, outSR, inSR); // Convert to WGS84

        // Setting parameter
        NTLocation[] facilities = new NTLocation[]{
                new NTLocation("MBK", 13.744651781616076, 100.52989481845307),
                new NTLocation("Siam Discovery", 13.746598089591219, 100.53145034771327),
                new NTLocation("Siam Center", 13.746321783330115, 100.53279034433699),
                new NTLocation("Siam Paragon", 13.746155248206387, 100.53481456769379)
        };
        NTLocation[] incident = new NTLocation[]{
                new NTLocation("incident", pinPoint.getY(), pinPoint.getX())
        };
        NTClosestFacilityParameter parameter = new NTClosestFacilityParameter(facilities, incident);
        parameter.setTravelMode(NTTravelMode.CAR);
        parameter.setImpedance(NTImpedanceMode.TIME);
        parameter.setTravelDirection(NTFacilityDirection.TO_FACILITY);
        parameter.setTargetFacilityCount(4);
        parameter.setOutSR(NTSpatialReference.WEB_MERCATOR);
        parameter.setLanguage(NTLanguage.LOCAL);
        parameter.setCountry(NTCountry.THAILAND);

        // Call service
        NTClosestFacilityService.executeAsync(parameter, new IServiceRequestListener<NTClosestFacilityResultSet>() {
            @Override
            public void onResponse(NTClosestFacilityResultSet result, String responseCode) {
                // If successful
                NTClosestFacilityResult[] results = result.getResults();

                SimpleLineSymbol lineSymbol;
                for (NTClosestFacilityResult cfr : results) {
                    String name = TextUtils.isEmpty(cfr.getFacilityName()) ? "" : cfr.getFacilityName();
                    if (name.endsWith("Siam Discovery")) {
                        lineSymbol = new SimpleLineSymbol(Color.BLUE, 5);
                    } else if (name.endsWith("Siam Center")) {
                        lineSymbol = new SimpleLineSymbol(Color.RED, 5);
                    } else if (name.endsWith("Siam Paragon")) {
                        lineSymbol = new SimpleLineSymbol(Color.MAGENTA, 5);
                    } else {
                        lineSymbol = new SimpleLineSymbol(Color.GREEN, 5);
                    }
                    try {
                        JsonParser parser = new JsonFactory().createJsonParser(cfr.getShape());
                        MapGeometry mapGeometry = GeometryEngine.jsonToGeometry(parser);
                        Geometry geometry = mapGeometry.getGeometry();

                        if (geometry != null && !geometry.isEmpty()) {
                            geometry = GeometryEngine.project(geometry,
                                    SpatialReference.create(4326),
                                    SpatialReference.create(102100));
                            Graphic routeGraphic = new Graphic(geometry, lineSymbol);
                            routeLayer.addGraphic(routeGraphic);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                // If error
                Toast.makeText(ClosestFacilityActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
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
        if (status == STATUS.LAYER_LOADED) {
            // Zoom to these 4 places
            Point mbk = GeometryEngine.project(100.52989481845307, 13.744651781616076, outSR);
            Point siamDis = GeometryEngine.project(100.53145034771327, 13.746598089591219, outSR);
            Point siamCenter = GeometryEngine.project(100.53279034433699, 13.746321783330115, outSR);
            Point siamParagon = GeometryEngine.project(100.53481456769379, 13.746155248206387, outSR);

            // Create circle with border symbol
            SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(Color.WHITE, 50, SimpleMarkerSymbol.STYLE.CIRCLE);
            symbol.setOutline(new SimpleLineSymbol(Color.BLACK, 5, SimpleLineSymbol.STYLE.SOLID));

            // Create number symbol
            TextSymbol textSymbol = new TextSymbol(25, "1", Color.BLACK,
                    TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.MIDDLE);

            // Combine 2 symbol and add to map view
            // 1
            CompositeSymbol compositeSymbol = new CompositeSymbol();
            compositeSymbol.add(symbol);
            compositeSymbol.add(textSymbol);
            mGraphicsLayer.addGraphic(new Graphic(mbk, compositeSymbol));

            // 2
            textSymbol.setText("2");
            compositeSymbol.removeAll();
            compositeSymbol.add(symbol);
            compositeSymbol.add(textSymbol);
            mGraphicsLayer.addGraphic(new Graphic(siamDis, compositeSymbol));

            // 3
            textSymbol.setText("3");
            compositeSymbol.removeAll();
            compositeSymbol.add(symbol);
            compositeSymbol.add(textSymbol);
            mGraphicsLayer.addGraphic(new Graphic(siamCenter, compositeSymbol));

            // 4
            textSymbol.setText("4");
            compositeSymbol.removeAll();
            compositeSymbol.add(symbol);
            compositeSymbol.add(textSymbol);
            mGraphicsLayer.addGraphic(new Graphic(siamParagon, compositeSymbol));

            // merge all point to envelope
            Envelope envelope = new Envelope();
            envelope.merge(mbk);
            envelope.merge(siamDis);
            envelope.merge(siamCenter);
            envelope.merge(siamParagon);

            // zoom to the envelope
            mapView.setExtent(envelope, 200);
        }
    }

    @Override
    public void onSingleTap(float x, float y) {
        // If there is a pin on map view, remove it
        if (pinOnMap) {
            mGraphicsLayer.removeGraphic(pinId);
            pinOnMap = false;
        }
        // clear all routes
        routeLayer.removeAll();

        if (rllPinOption.getVisibility() == View.VISIBLE) {
            rllPinOption.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onLongPress(float x, float y) {
        // If there is a pin on map view, remove it
        if (pinOnMap) {
            mGraphicsLayer.removeGraphic(pinId);
            pinOnMap = false;
        }
        // Clear all routes
        routeLayer.removeAll();
        // Draw pin on map
        Point p = mapView.toMapPoint(x, y);

        Drawable pinDrawable = ResourcesCompat.getDrawable(
                getResources(), R.drawable.pin_markonmap, getTheme());
        PictureMarkerSymbol pin = new PictureMarkerSymbol(pinDrawable);
        pinId = mGraphicsLayer.addGraphic(new Graphic(p, pin));
        pinOnMap = true;

        rllPinOption.setVisibility(View.VISIBLE);
        return false;
    }

    @Override
    protected void onPause() {
        ldm.pause();
        mapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.unpause();
        ldm.resume();
    }

    @Override
    protected void onDestroy() {
        ldm.stop();
        super.onDestroy();
    }
}