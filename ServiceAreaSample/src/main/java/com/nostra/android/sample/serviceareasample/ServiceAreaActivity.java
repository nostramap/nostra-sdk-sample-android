package com.nostra.android.sample.serviceareasample;

import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.Symbol;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;

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

public class ServiceAreaActivity extends AppCompatActivity implements OnSingleTapListener, OnLongPressListener {
    private MapView mapView;
    private ImageButton imbLocation;
    private RelativeLayout rllPinOption;
    private TextView txvPin, txvCancel;

    private GraphicsLayer mGraphicsLayer, saLayer;
    private SpatialReference inSR = SpatialReference.create(SpatialReference.WKID_WGS84);
    private SpatialReference outSR = SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE);
    private int pinId = -1;
    private boolean pinOnMap;
    private double lat, lon;

    private SimpleFillSymbol smallSymbol;
    private SimpleFillSymbol mediumSymbol;
    private SimpleFillSymbol largeSymbol;

    private NTMapPermissionResult[] ntMapResults;
    private LocationManager manager;
    private LocationDisplayManager ldm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_area);

        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("TOKEN_SDK", this);
        // TODO: Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

        mapView = (MapView) findViewById(R.id.mapView);
        ldm = mapView.getLocationDisplayManager();
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
                Point currentPoint = ldm.getPoint();
                if (currentPoint != null) {
                    mapView.centerAt(currentPoint, true);
                }
            }
        });

        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            LocationDisplayManager ldm = mapView.getLocationDisplayManager();
            ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
            ldm.setLocationListener(new LocationListener() {
                boolean firstRun = true;

                @Override
                public void onLocationChanged(Location location) {
                    lat = location.getLatitude();
                    lon = location.getLongitude();
                    if (firstRun) {
                        Point p = GeometryEngine.project(lon, lat, outSR);
                        mapView.zoomToResolution(p, 1);
                        firstRun = false;
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            });
            ldm.start();
        }
        // Symbol for the smallest service area polygon
        smallSymbol = new SimpleFillSymbol(Color.RED);
        smallSymbol.setAlpha(128);
        smallSymbol.setOutline(new SimpleLineSymbol(Color.BLACK, 1));

        // Symbol for the medium service area polygon
        mediumSymbol = new SimpleFillSymbol(Color.YELLOW);
        mediumSymbol.setAlpha(128);
        mediumSymbol.setOutline(new SimpleLineSymbol(Color.BLACK, 1));

        // Symbol for the largest service area polygon
        largeSymbol = new SimpleFillSymbol(Color.GREEN);
        largeSymbol.setAlpha(128);
        largeSymbol.setOutline(new SimpleLineSymbol(Color.BLACK, 1));
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

                    UserCredentials credentials = new UserCredentials();
                    credentials.setUserToken(token, referrer);
                    credentials.setAuthenticationType(UserCredentials.AuthenticationType.TOKEN);

                    ArcGISTiledMapServiceLayer layer = new ArcGISTiledMapServiceLayer(url, credentials);
                    mapView.addLayer(layer);

                    // Add graphic layer for add pin and service area
                    mGraphicsLayer = new GraphicsLayer();
                    saLayer = new GraphicsLayer();
                    mapView.addLayer(saLayer);
                    mapView.addLayer(mGraphicsLayer);
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(ServiceAreaActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        mapView.setOnSingleTapListener(this);
        mapView.setOnLongPressListener(this);
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
        Point pin = (Point) mGraphicsLayer.getGraphic(pinId).getGeometry();
        pin = (Point) GeometryEngine.project(pin, outSR, inSR); // Convert to WGS84

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
                saLayer.removeAll();

                if (result != null) {
                    try {
                        Envelope allEnv = new Envelope();
                        Envelope lineEnv = new Envelope();

                        NTServiceAreaResult[] results = result.getResults();
                        if (results != null && results.length > 0) {
                            for (int i = 0; i < results.length; i++) {
                                JsonParser parser = new JsonFactory().createJsonParser(results[i].getShape());
                                MapGeometry mapGeometry = GeometryEngine.jsonToGeometry(parser);
                                Geometry geometry = mapGeometry.getGeometry();

                                if (geometry != null && !geometry.isEmpty()) {
                                    geometry = GeometryEngine.project(geometry,
                                            SpatialReference.create(4326),
                                            SpatialReference.create(102100));

                                    Symbol symbol = i % 3 == 0 ? largeSymbol : i % 2 == 0 ? mediumSymbol : smallSymbol;
                                    Graphic routeGraphic = new Graphic(geometry, symbol);
                                    saLayer.addGraphic(routeGraphic);

                                    geometry.queryEnvelope(lineEnv);
                                    allEnv.merge(lineEnv);
                                }
                            }
                        }
                        // Zoom to the extent of the service area polygon with a padding
                        mapView.setExtent(allEnv, 50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(ServiceAreaActivity.this, "No service area", Toast.LENGTH_SHORT).show();
                    if (pinOnMap) {
                        mGraphicsLayer.removeGraphic(pinId);
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

    @Override
    public boolean onLongPress(float x, float y) {
        // If there is a pin on map view, remove it
        if (pinOnMap) {
            mGraphicsLayer.removeGraphic(pinId);
            pinOnMap = false;
        }
        // clear the service area
        saLayer.removeAll();

        // Draw pin on map
        Point point = mapView.toMapPoint(x, y);
        PictureMarkerSymbol pin = new PictureMarkerSymbol(ContextCompat.getDrawable(this,R.drawable.pin_markonmap));
        pinId = mGraphicsLayer.addGraphic(new Graphic(point, pin));
        pinOnMap = true;

        rllPinOption.setVisibility(View.VISIBLE);
        return false;
    }

    @Override
    public void onSingleTap(float x, float y) {
        // If there is a pin on map view, remove it
        if (pinOnMap) {
            mGraphicsLayer.removeGraphic(pinId);
            pinOnMap = false;
        }
        // clear the service area
        saLayer.removeAll();

        if (rllPinOption.getVisibility() == View.VISIBLE) {
            rllPinOption.setVisibility(View.GONE);
        }
    }
}