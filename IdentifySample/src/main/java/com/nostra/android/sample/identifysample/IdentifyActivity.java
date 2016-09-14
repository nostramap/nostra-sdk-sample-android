package com.nostra.android.sample.identifysample;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnLongPressListener;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.CoordinateConversion;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;

import java.util.HashMap;
import java.util.Map;

import th.co.nostrasdk.Base.IServiceRequestListener;
import th.co.nostrasdk.Base.NTIdentifyService;
import th.co.nostrasdk.Base.NTMapPermissionService;
import th.co.nostrasdk.Base.NTSDKEnvironment;
import th.co.nostrasdk.Parameter.Class.NTPoint;
import th.co.nostrasdk.Parameter.NTIdentifyParameter;
import th.co.nostrasdk.Result.NTIdentifyResult;
import th.co.nostrasdk.Result.NTMapPermissionResult;
import th.co.nostrasdk.Result.NTMapPermissionResultSet;

public class IdentifyActivity extends AppCompatActivity
        implements OnStatusChangedListener, OnSingleTapListener, OnLongPressListener {
    private MapView mapView;
    private NTMapPermissionResult[] ntMapResults;
    private Point point;
    private GraphicsLayer graphicsLayerPin = new GraphicsLayer();
    private Callout mapCallout;
    private Point mapPoint;
    private Double lat;
    private Double lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify);

        // Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setOnStatusChangedListener(this);
        mapView.setOnLongPressListener(this);
        mapView.setOnSingleTapListener(this);

        // Zoom to current location
        ImageView imvCurrentLocation = (ImageView) findViewById(R.id.imvCurrentLocation);
        imvCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.centerAt(mapPoint,true);
            }
        });
        initialMap();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.unpause();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.pause();
    }

    @Override
    public void onStatusChanged(Object source, STATUS status) {
        if (source == mapView && status == OnStatusChangedListener.STATUS.INITIALIZED) {
            LocationDisplayManager locationManager = mapView.getLocationDisplayManager();
            locationManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
            locationManager.setLocationListener(locationListener);
            locationManager.start();
        }
    }

    private LocationListener locationListener = new LocationListener() {
        boolean locationChanged = false;

        @Override
        public void onLocationChanged(Location location) {
            if (!locationChanged) {
                locationChanged = true;

                double locationY = location.getLatitude();
                double locationX = location.getLongitude();
                Point wgsPoint = new Point(locationX, locationY);
                mapPoint = (Point) GeometryEngine.project(wgsPoint,SpatialReference.create(4326),
                        mapView.getSpatialReference());

                Unit mapUnit = mapView.getSpatialReference().getUnit();
                double zoomWidth = Unit.convertUnits(5, Unit.create(LinearUnit.Code.MILE_US), mapUnit);
                Envelope zoomExtent = new Envelope(mapPoint,zoomWidth, zoomWidth);
                mapView.setExtent(zoomExtent);
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
    };

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
                    String referrer = "referrer";    // TODO: Insert referrer

                    UserCredentials credentials = new UserCredentials();
                    credentials.setUserToken(token, referrer);
                    credentials.setAuthenticationType(UserCredentials.AuthenticationType.TOKEN);

                    ArcGISTiledMapServiceLayer layer = new ArcGISTiledMapServiceLayer(url, credentials);
                    mapView.addLayer(layer);

                    NTPoint ntPoint = map.getDefaultZoom();
                    if (ntPoint != null) {
                        lat = ntPoint.getY();
                        lon = ntPoint.getX();
                        point = new Point(lat, lon);
                        String decimalDegrees = CoordinateConversion.pointToDecimalDegrees(point,
                                SpatialReference.create(SpatialReference.WKID_WGS84), 7);
                        point = CoordinateConversion.decimalDegreesToPoint(decimalDegrees,
                                SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE));
                        mapView.addLayer(layer);
                        mapView.centerAt(point, true);
                        mapView.addLayer(graphicsLayerPin);
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(IdentifyActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
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

    //Set content in callout
    private View loadView(String name,
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
        txvLocation.setText(adminLevel4L + " " + adminLevel3L + " "+ "\n" + adminLevel2L + " " + adminLevel1L);
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

    @Override
    public boolean onLongPress(float x, float y) {
        if (!mapView.isLoaded()) {
            Toast.makeText(IdentifyActivity.this, "Map is loading", Toast.LENGTH_SHORT);
        } else if (mapView.isLoaded()) {
            graphicsLayerPin.removeAll();
            if (mapCallout != null && mapCallout.isShowing()) {
                mapCallout.hide();
            }
            if (x != 0 && y != 0) {
                point = mapView.toMapPoint(x, y);
                String decimalDegrees = CoordinateConversion.pointToDecimalDegrees(point,
                        mapView.getSpatialReference(), 7);
                final Point wgsPoint = CoordinateConversion.decimalDegreesToPoint(decimalDegrees,
                        SpatialReference.create(SpatialReference.WKID_WGS84));
                PictureMarkerSymbol markerSymbol = new PictureMarkerSymbol(this,
                        getResources().getDrawable(R.drawable.pin_markonmap));
                Graphic graphic = new Graphic(point, markerSymbol);
                final int id = graphicsLayerPin.addGraphic(graphic);
                // Setting parameter
                NTIdentifyParameter param = new NTIdentifyParameter(wgsPoint.getY(), wgsPoint.getX());
                // Call NTIdentifyService and show callout
                NTIdentifyService.executeAsync(param, new IServiceRequestListener<NTIdentifyResult>() {
                    @Override
                    public void onResponse(NTIdentifyResult result, String s) {
                        String nameL = result.getName_L();
                        String nostraId = result.getNostraId();
                        String adminLevel1L = result.getAdminLevel1_L();
                        String adminLevel2L = result.getAdminLevel2_L();
                        String adminLevel3L = result.getAdminLevel3_L();
                        String adminLevel4L = result.getAdminLevel4_L();

                        Map<String, Object> attr = new HashMap<>();
                        attr.put("nameL", nameL);
                        attr.put("nostraId", nostraId);
                        attr.put("adminLevel4L", adminLevel4L);
                        attr.put("adminLevel3L", adminLevel3L);
                        attr.put("adminLevel2L", adminLevel2L);
                        attr.put("adminLevel1L", adminLevel1L);
                        graphicsLayerPin.updateGraphic(id, attr);
                        mapCallout = mapView.getCallout();

                        // Sets custom content view to Callout
                        mapCallout.setContent(loadView(nameL, nostraId,
                                adminLevel4L, adminLevel3L, adminLevel2L, adminLevel1L));
                        mapCallout.setOffsetDp(0, 25);
                        mapCallout.show(point);
                    }

                    @Override
                    public void onError(String s) {
                        Toast.makeText(IdentifyActivity.this, s, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(IdentifyActivity.this, "No Data", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    @Override
    public void onSingleTap(float x, float y) {
        if(mapCallout != null && mapCallout.isShowing()) {
            mapCallout.hide();
            graphicsLayerPin.removeAll();
        }else {
            Toast.makeText(IdentifyActivity.this, "Select location", Toast.LENGTH_SHORT).show();
        }
    }
}