package com.nostra.android.sample.multimodalsample;

import android.location.Location;
import android.location.LocationListener;
import android.os.StrictMode;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
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

import th.co.gissoft.nostrasdk.Base.IServiceRequestListener;
import th.co.gissoft.nostrasdk.Base.NTMapPermissionService;
import th.co.gissoft.nostrasdk.Base.NTSDKEnvironment;
import th.co.gissoft.nostrasdk.Parameter.Class.NTPoint;
import th.co.gissoft.nostrasdk.Result.NTMapPermissionResult;
import th.co.gissoft.nostrasdk.Result.NTMapPermissionResultSet;

public class PinMarkOnMapActivity extends AppCompatActivity implements OnStatusChangedListener {
    private MapView mapView;
    private LocationDisplayManager locationDisplayManager;
    private NTMapPermissionResult[] ntMapResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_mark_on_map);

        Button btnOk = (Button) findViewById(R.id.btnOk);
        Button btnCancel = (Button) findViewById(R.id.btnCancel);
        btnOk.setOnClickListener(btnOkClick);
        btnCancel.setOnClickListener(btnCancelClick);

        initialMap();
    }

    @Override
    public void onStatusChanged(Object source, STATUS status) {
        if (source == mapView && status == OnStatusChangedListener.STATUS.INITIALIZED) {
            locationDisplayManager = mapView.getLocationDisplayManager();
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
                        Point mapPoint = (Point) GeometryEngine
                                .project(wgsPoint,
                                        SpatialReference.create(4326),
                                        mapView.getSpatialReference());

                        Unit mapUnit = mapView.getSpatialReference().getUnit();
                        double zoomWidth = Unit.convertUnits(5, Unit.create(LinearUnit.Code.MILE_US),mapUnit);
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
            locationDisplayManager.start();
        }
    }

    //Initialize map and current location
    private void initialMap() {
        mapView = (MapView) findViewById(R.id.mapView);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy); // to run network operation on main thread

        NTSDKEnvironment.setEnvironment("API_KEY", this);
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

                    NTPoint ntPoint = map.getDefaultZoom();
                    if (ntPoint != null) {
                        double lon = ntPoint.getX();
                        double lat = ntPoint.getY();
                        mapView.centerAt(lat, lon, true);
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getApplicationContext(),errorMessage,Toast.LENGTH_SHORT).show();
            }
        });

        mapView.setOnStatusChangedListener(this);
    }

    private View.OnClickListener btnCancelClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

    //Get parameter from MainActivity
    private View.OnClickListener btnOkClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int toLocationCode = 0;
            int fromLocationCode = 1;
            Point pointCenter;
            if(!mapView.isLoaded()){
                Toast.makeText(getApplicationContext(),"Loading Map",Toast.LENGTH_SHORT).show();
            }else  if (getIntent().getExtras().getString("Location").equals("toLocation")) {
                pointCenter = mapView.getCenter();
                getIntent().putExtra("CenterX", pointCenter.getX());
                getIntent().putExtra("CenterY", pointCenter.getY());
                getIntent().putExtra("ToLocation", toLocationCode);
            } else if (getIntent().getExtras().getString("Location").equals("fromLocation")) {
                pointCenter = mapView.getCenter();
                getIntent().putExtra("CenterX", pointCenter.getX());
                getIntent().putExtra("CenterY", pointCenter.getY());
                getIntent().putExtra("FromLocation", fromLocationCode);
            }
            setResult(RESULT_OK, getIntent());
            finish();
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