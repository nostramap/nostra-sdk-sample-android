package com.nostra.android.sample.addresssearchsample;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.loadable.LoadStatusChangedEvent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedListener;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
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

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private NTMapPermissionResult[] ntMapResults;
    private Point pointMap = null;
    private GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
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

        mapView = (MapView) findViewById(R.id.mapView);
        // mapView.setOnStatusChangedListener(this);
        Boolean hasIntentData = (getIntent().getExtras() == null);
        initializeMap(hasIntentData);

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
                    //  mapView.centerAt(mapPoint, true);
                    mapView.setViewpointCenterAsync(mapPoint,50000);
                } else {
                    //  mapView.centerAt(pointMap, true);
                    mapView.setViewpointCenterAsync(pointMap,50000);
                }
            }
        });
    }

    private void initializeMap(Boolean hasIntentData) {
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
                    UserCredential credentials = UserCredential.createFromToken(token,referrer);
                    ArcGISTiledLayer layer = new ArcGISTiledLayer(url);
                    layer.setCredential(credentials);
                    Basemap basemap = new Basemap(layer);
                    Envelope env = new Envelope(
                            1.0672849926751213E7,
                            593515.9027621585,
                            1.1905414975501748E7,
                            2375599.5357473083,
                            SpatialReference.create(102100)
                    );

                    ArcGISMap map = new ArcGISMap(basemap);
                    Viewpoint vp = new Viewpoint(env);
                    map.setInitialViewpoint(vp);
                    mapView.setMap(map);
                    map.addLoadStatusChangedListener(loadStatusChangedListener);


                    NTPoint ntPoint = mapPermission.getDefaultLocation();
                    if (ntPoint != null) {
                        Point wgsPoint = new Point(ntPoint.getX(), ntPoint.getY());
                        // Convert WGS84 point (lat, lon) to WebMercator point (y, x)
                        pointMap = (Point) GeometryEngine.project(wgsPoint,
                                SpatialReference.create(102100));
                        mapView.getGraphicsOverlays().add(graphicsOverlay);
                        mapView.setViewpointCenterAsync(pointMap,50000);
                        if (isSearchResult) {
                            onAddGraphic();
                        }
                    }
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(MapActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    LoadStatusChangedListener loadStatusChangedListener = new LoadStatusChangedListener() {
        @Override
        public void loadStatusChanged(LoadStatusChangedEvent loadStatusChangedEvent) {
            if (loadStatusChangedEvent.getNewLoadStatus().equals(LoadStatus.LOADED)) {
                LocationDisplay locationDisplay = mapView.getLocationDisplay();
                locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.OFF);
                locationDisplay.startAsync();
                locationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
                    boolean locationChanged = false;

                    @Override
                    public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
                        if (!locationChanged) {
                            locationChanged = true;
                            Point loc = locationChangedEvent.getLocation().getPosition();
                            double locY = loc.getY();
                            double locX = loc.getX();
                            Point wgsPoint = new Point(locX, locY, SpatialReferences.getWgs84());
                            //
                            mapPoint = (Point) GeometryEngine.project(wgsPoint, mapView.getSpatialReference());
                            if (!isSearchResult) {
                                mapView.setViewpointCenterAsync(mapPoint, 50000);
                            }

                        }
                    }
                });

            }
        }
    };

    private void onAddGraphic() {
        graphicsOverlay.getGraphics().clear();
        Point wgsPoint = new Point(lonSearch, latSearch, SpatialReferences.getWgs84());
        // Convert WGS84 point (lat, lon) to WebMercator point (x, y)
        pointMap = (Point) GeometryEngine.project(wgsPoint,
                SpatialReference.create(102100));
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.pin_markonmap);

        ListenableFuture<PictureMarkerSymbol> symbol =
                PictureMarkerSymbol.createAsync((BitmapDrawable) drawable);
        Map<String, Object> attr = new HashMap<>();
        attr.put("houseNo", houseNo);
        attr.put("moo", moo);
        attr.put("soiL", soiL);
        attr.put("roadL", roadL);
        attr.put("adminLevel1L", adminLevel1L);
        attr.put("adminLevel2L", adminLevel2L);
        attr.put("adminLevel3L", adminLevel3L);
        attr.put("postcode", postcode);

        try {

            Graphic graphic = new Graphic(pointMap, symbol.get());
            graphicsOverlay.getGraphics().add(graphic);

            // Display callout.
            View calloutView = loadView(houseNo, moo, soiL, roadL,
                    adminLevel1L, adminLevel2L, adminLevel3L, postcode);
            Callout mapCallOut = mapView.getCallout();
            mapCallOut.setContent(calloutView);
            mapCallOut.setLocation(pointMap);
            mapCallOut.show();
            mapView.setViewpointCenterAsync(mapCallOut.getLocation(),50000);

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
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

