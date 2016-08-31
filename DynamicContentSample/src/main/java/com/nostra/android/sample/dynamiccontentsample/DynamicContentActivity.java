package com.nostra.android.sample.dynamiccontentsample;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;

import th.co.gissoft.nostrasdk.Base.IServiceRequestListener;
import th.co.gissoft.nostrasdk.Base.NTDynamicContentListService;
import th.co.gissoft.nostrasdk.Base.NTMapPermissionService;
import th.co.gissoft.nostrasdk.Base.NTSDKEnvironment;
import th.co.gissoft.nostrasdk.Base.NTShortLinkService;
import th.co.gissoft.nostrasdk.Parameter.Constant.NTLanguage;
import th.co.gissoft.nostrasdk.Parameter.Constant.NTMapType;
import th.co.gissoft.nostrasdk.Parameter.Constant.NTShortLinkType;
import th.co.gissoft.nostrasdk.Parameter.NTShortLinkParameter;
import th.co.gissoft.nostrasdk.Result.NTDynamicContentListResult;
import th.co.gissoft.nostrasdk.Result.NTDynamicContentListResultSet;
import th.co.gissoft.nostrasdk.Result.NTDynamicContentResult;
import th.co.gissoft.nostrasdk.Result.NTMapPermissionResult;
import th.co.gissoft.nostrasdk.Result.NTMapPermissionResultSet;
import th.co.gissoft.nostrasdk.Result.NTShortLinkResult;

public class DynamicContentActivity extends AppCompatActivity implements OnStatusChangedListener {
    private MapView mapView;
    private DrawerLayout drawerLayout;
    private ImageView imvLayer, imvBack;
    private TextView txvHeader;
    private RecyclerView rcvLayer;
    private FrameLayout frlContainer;
    private ImageButton imbLocation;

    private View curtainView;
    private RelativeLayout rllShare;
    private TextView txvShareUrl, txvCopy, txvCancel;

    private GraphicsLayer mGraphicsLayer;
    private SpatialReference outSR = SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE);
    private NTMapPermissionResult[] ntMapResults;
    private NTDynamicContentListResult[] dynamicLayers;
    private double lat, lon;
    private DMCResultFragment resultFragment;
    private LocationManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dynamic_map_content);

        // Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

        manager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mapView = (MapView) findViewById(R.id.mapView);
        rcvLayer = (RecyclerView) findViewById(R.id.rcvLayer);

        initializeMap();
        generateLayerList();

        imvLayer = (ImageView) findViewById(R.id.imvLayer);
        imvLayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open drawer
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        txvHeader = (TextView) findViewById(R.id.txvHeader);
        imvBack = (ImageView) findViewById(R.id.imvBack);
        imvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        frlContainer = (FrameLayout) findViewById(R.id.frlContainer);

        curtainView = findViewById(R.id.curtainView);
        curtainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                curtainView.setVisibility(View.GONE);
                rllShare.setVisibility(View.GONE);
                txvShareUrl.setText(null);
            }
        });

        rllShare = (RelativeLayout) findViewById(R.id.rllShare);
        rllShare.setOnClickListener(null);
        txvShareUrl = (TextView) findViewById(R.id.txvShareUrl);

        txvCopy = (TextView) findViewById(R.id.txvCopy);
        txvCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Copy share url to clipboard
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText(ClipDescription.MIMETYPE_TEXT_PLAIN, txvShareUrl.getText().toString());
                clipboardManager.setPrimaryClip(clipData);
                curtainView.setVisibility(View.GONE);
                rllShare.setVisibility(View.GONE);
                txvShareUrl.setText(null);
            }
        });

        txvCancel = (TextView) findViewById(R.id.txvCancel);
        txvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                curtainView.setVisibility(View.GONE);
                rllShare.setVisibility(View.GONE);
                txvShareUrl.setText(null);
            }
        });

        imbLocation = (ImageButton) findViewById(R.id.imbLocation);
        imbLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pan to current location
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    LocationDisplayManager ldm = mapView.getLocationDisplayManager();
                    ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                    ldm.start();
                }
            }
        });
    }

    private void initializeMap() {
        // Call map service to add map layer
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
                // Add graphic layer for add pin
                mGraphicsLayer = new GraphicsLayer();
                mapView.addLayer(mGraphicsLayer);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        mapView.setOnStatusChangedListener(this);
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

    private void generateLayerList() {
        // Call service and show in drawer list
        NTDynamicContentListService.executeAsync(new IServiceRequestListener<NTDynamicContentListResultSet>() {
            @Override
            public void onResponse(NTDynamicContentListResultSet result, String responseCode) {
                dynamicLayers = result.getResults();
                DMCLayerAdapter layerAdapter = new DMCLayerAdapter(dynamicLayers, new DMCLayerAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(int position) {
                        // Show result in fragment
                        resultFragment = DMCResultFragment.newInstance(
                                dynamicLayers[position].getLayerID(), lat, lon);
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.frlContainer, resultFragment);
                        transaction.addToBackStack("result");
                        transaction.commit();

                        frlContainer.setVisibility(View.VISIBLE);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        imvLayer.setClickable(false);
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    }
                });
                rcvLayer.setAdapter(layerAdapter);
                rcvLayer.setLayoutManager(new LinearLayoutManager(DynamicContentActivity.this));
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(DynamicContentActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    void showDetail(NTDynamicContentResult dmcResult) {
        // Show detail
        DMCDetailFragment detailFragment = DMCDetailFragment.newInstance(dmcResult);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.frlContainer, detailFragment);
        transaction.addToBackStack("detail");
        transaction.commit();
    }

    void showOnMap(NTDynamicContentResult dmcResult) {
        // Show result location on map
        frlContainer.setVisibility(View.GONE);
        imvBack.setVisibility(View.VISIBLE);
        imvLayer.setVisibility(View.GONE);
        txvHeader.setText("DISPLAY ON MAP");

        Point p = GeometryEngine.project(dmcResult.getLongitude(), dmcResult.getLatitude(), outSR);
        PictureMarkerSymbol pin = new PictureMarkerSymbol(getApplicationContext().getResources()
                .getDrawable(R.drawable.pin_markonmap));

        mGraphicsLayer.addGraphic(new Graphic(p, pin));
        mapView.zoomTo(p, 11);
    }

    void createShareUrl(NTDynamicContentResult dmcResult) {
        curtainView.setVisibility(View.VISIBLE);
        rllShare.setVisibility(View.VISIBLE);

        NTShortLinkParameter param = new NTShortLinkParameter(dmcResult.getName_L());
        param.setDescription(dmcResult.getAddress_L());
        param.setLinkType(NTShortLinkType.SEARCH);
        param.setLanguage(NTLanguage.LOCAL);
        param.setLevel(11);
        param.setMap(NTMapType.STREET_MAP);

        // Call share service and show the url
        NTShortLinkService.executeAsync(param, new IServiceRequestListener<NTShortLinkResult>() {
            @Override
            public void onResponse(NTShortLinkResult result, String responseCode) {
                txvShareUrl.setText(result.getUrl());
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Handle when user touch back
        if (frlContainer.getVisibility() == View.VISIBLE) {
            FragmentManager manager = getSupportFragmentManager();
            if (manager.getBackStackEntryAt(manager.getBackStackEntryCount() - 1)
                    .getName().equalsIgnoreCase("detail")) {
                if (rllShare.getVisibility() == View.VISIBLE) {
                    curtainView.setVisibility(View.GONE);
                    rllShare.setVisibility(View.GONE);
                    txvShareUrl.setText(null);
                    return;
                }
                manager.popBackStack();
                return;
            }
            if (manager.getBackStackEntryCount() > 0) {
                manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            frlContainer.setVisibility(View.GONE);
            imvLayer.setClickable(true);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        } else {
            if (imvBack.getVisibility() == View.VISIBLE) {
                txvHeader.setText("DYNAMIC MAP CONTENT");
                imvLayer.setVisibility(View.VISIBLE);
                imvBack.setVisibility(View.GONE);
                frlContainer.setVisibility(View.VISIBLE);
                mGraphicsLayer.removeAll();
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    LocationDisplayManager ldm = mapView.getLocationDisplayManager();
                    ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                    ldm.start();
                }
                return;
            }
            super.onBackPressed();
        }
    }

    @Override
    public void onStatusChanged(Object source, STATUS status) {
        if (status == STATUS.LAYER_LOADED) {
            // Zoom to current location
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                LocationDisplayManager ldm = mapView.getLocationDisplayManager();
                ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                // For the first run only
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
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(String provider) {}

                    @Override
                    public void onProviderDisabled(String provider) {}
                });
                ldm.start();
            }
        }
    }

    @Override
    protected void onPause() {
        LocationDisplayManager ldm = mapView.getLocationDisplayManager();
        ldm.pause();
        mapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.unpause();
        LocationDisplayManager ldm = mapView.getLocationDisplayManager();
        ldm.resume();
    }

    @Override
    protected void onDestroy() {
        LocationDisplayManager ldm = mapView.getLocationDisplayManager();
        ldm.stop();
        super.onDestroy();
    }
}