package com.nostra.android.sample.addresssearchsample;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
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

import th.co.nostrasdk.NTSDKEnvironment;
import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;
import th.co.nostrasdk.network.NTPoint;

public class MapActivity extends AppCompatActivity implements OnStatusChangedListener {
    private MapView mapView;
    private NTMapPermissionResult[] ntMapResults;
    private Point pointMap;
    private GraphicsLayer graphicsLayer = new GraphicsLayer();
    private String houseNo;
    private String moo;
    private String soiL;
    private String roadL;
    private String adminLevel1L;
    private String adminLevel2L;
    private String adminLevel3L;
    private String postcode;
    private double latSearch;
    private double lonSearch;
    private Point mapPoint;
    private boolean isSearchResult = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("TOKEN_SDK", this);
        // TODO: Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setOnStatusChangedListener(this);
        initializeMap();

        // Get parameter from Class ListResultActivity
        try {
            Bundle data = getIntent().getExtras();
            latSearch = data.getDouble("lat");
            lonSearch = data.getDouble("lon");
            houseNo = data.getString("houseNo");
            moo = data.getString("moo");
            soiL = data.getString("soiL");
            roadL = data.getString("roadL");
            adminLevel1L = data.getString("adminLevel1L");
            adminLevel2L = data.getString("adminLevel2L");
            adminLevel3L = data.getString("adminLevel3L");
            postcode = data.getString("PostCode");
            isSearchResult = true;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        ImageView imvSearch = (ImageView) findViewById(R.id.imvSearch);
        imvSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });

        EditText edtSearch = (EditText) findViewById(R.id.edtSearch);
        edtSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapActivity.this, SearchActivity.class);
                startActivity(intent);
            }

        });
        // Zoom to Current Location
        final ImageView imvCurrentLocation = (ImageView) findViewById(R.id.imvCurrentLocation);
        imvCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lonSearch == 0 && latSearch == 0) {
                    mapView.centerAt(mapPoint, true);
                } else {
                    mapView.centerAt(pointMap, true);
                }
            }
        });
    }

    private void initializeMap() {
        // Add map and current location
        NTMapPermissionService.executeAsync(new ServiceRequestListener<NTMapPermissionResultSet>() {
            @Override
            public void onResponse(NTMapPermissionResultSet resultSet) {
                ntMapResults = resultSet.getResults();
                NTMapPermissionResult mapPermission = getThailandBasemap();
                if (mapPermission != null) {
                    NTMapServiceInfo localInfo = mapPermission.getLocalService();
                    String url = localInfo.getServiceUrl();
                    String token = localInfo.getServiceToken();
                    // TODO: Insert referrer
                    String referrer = "REFERRER";

                    UserCredentials credentials = new UserCredentials();
                    credentials.setUserToken(token, referrer);
                    credentials.setAuthenticationType(UserCredentials.AuthenticationType.TOKEN);

                    ArcGISTiledMapServiceLayer layer = new ArcGISTiledMapServiceLayer(url, credentials);
                    mapView.addLayer(layer);

                    NTPoint ntPoint = mapPermission.getDefaultLocation();
                    if (ntPoint != null) {
                        pointMap = new Point(ntPoint.getX(), ntPoint.getY());
                        String decimalDegrees = CoordinateConversion.pointToDecimalDegrees(
                                pointMap, SpatialReference.create(SpatialReference.WKID_WGS84), 7);
                        pointMap = CoordinateConversion.decimalDegreesToPoint(
                                decimalDegrees, SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR));

                        mapView.addLayer(graphicsLayer);
                        mapView.centerAt(pointMap, true);
                    }
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(MapActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
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
                        mapPoint = (Point) GeometryEngine.project(wgsPoint,
                                SpatialReference.create(4326), mapView.getSpatialReference());

                        Unit mapUnit = mapView.getSpatialReference().getUnit();
                        double zoomWidth = Unit.convertUnits(5, Unit.create(LinearUnit.Code.MILE_US), mapUnit);
                        Envelope zoomExtent = new Envelope(mapPoint, zoomWidth, zoomWidth);
                        mapView.setExtent(zoomExtent);
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
            locationManager.start();
        } else if (status == STATUS.LAYER_LOADED && isSearchResult) {
            graphicsLayer.removeAll();
            pointMap = new Point(lonSearch, latSearch);
            String strPoint = CoordinateConversion.pointToDecimalDegrees(pointMap,
                    SpatialReference.create(SpatialReference.WKID_WGS84), 7);
            pointMap = CoordinateConversion.decimalDegreesToPoint(strPoint,
                    SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE));
            PictureMarkerSymbol symbol = new PictureMarkerSymbol(MapActivity.this,
                    ContextCompat.getDrawable(this,R.drawable.pin_markonmap));

            Map<String, Object> attr = new HashMap<>();
            attr.put("houseNo", houseNo);
            attr.put("moo", moo);
            attr.put("soiL", soiL);
            attr.put("roadL", roadL);
            attr.put("adminLevel1L", adminLevel1L);
            attr.put("adminLevel2L", adminLevel2L);
            attr.put("adminLevel3L", adminLevel3L);
            attr.put("postcode", postcode);

            Graphic graphic = new Graphic(pointMap, symbol, attr);
            graphicsLayer.addGraphic(graphic);
            mapView.centerAt(pointMap, true);
            mapView.zoomToScale(pointMap, 10000);

            // Display callout.
            View calloutView = loadView(houseNo, moo, soiL, roadL,
                    adminLevel1L, adminLevel2L, adminLevel3L, postcode);
            Callout mapCallOut = mapView.getCallout();
            mapCallOut.setOffsetDp(0, 25);
            mapCallOut.animatedShow(pointMap, calloutView);
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

    // Set content in callout
    private View loadView(String houseNo,
                          String moo,
                          String soiL,
                          String roadL,
                          String adminLevel1L,
                          String adminLevel2L,
                          String adminLevel3L,
                          String postcode) {

        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(houseNo)) {
            sb.append(houseNo);
        }
        if (!TextUtils.isEmpty(moo)) {
            sb.append("หมู่ ").append(moo).append(" ");
        }
        if (!TextUtils.isEmpty(soiL)) {
            sb.append("ซอย ").append(soiL).append(" ");
        }
        if (!TextUtils.isEmpty(roadL)) {
            sb.append("ถนน ").append(roadL).append(" ");
        }
        if (!TextUtils.isEmpty(adminLevel3L)) {
            sb.append("ตำบล ").append(adminLevel3L).append(" ");
        }
        if (!TextUtils.isEmpty(adminLevel2L)) {
            sb.append("อำเภอ ").append(adminLevel2L).append(" ");
        }
        if (!TextUtils.isEmpty(adminLevel1L)) {
            sb.append("จังหวัด ").append(adminLevel1L).append(" ");
        }
        if (!TextUtils.isEmpty(postcode)) {
            sb.append("รหัสไปรษณี ").append(postcode);
        }
        Drawable pin = ResourcesCompat.getDrawable(getResources(),
                R.drawable.pin_markonmap, getTheme());

        View view = LayoutInflater.from(MapActivity.this).inflate(R.layout.callout, null);
        final TextView number = (TextView) view.findViewById(R.id.location);
        number.setText(sb.toString());

        final ImageView imvPin = (ImageView) view.findViewById(R.id.pinMarkOnMap);
        imvPin.setImageDrawable(pin);
        return view;
    }
}
