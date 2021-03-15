package com.nostra.android.sample.dynamiccontentsample;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;

import java.util.concurrent.ExecutionException;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.common.NTLanguage;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;
import th.co.nostrasdk.network.NTLocation;
import th.co.nostrasdk.query.dynamic.NTDynamicContentListResult;
import th.co.nostrasdk.query.dynamic.NTDynamicContentListResultSet;
import th.co.nostrasdk.query.dynamic.NTDynamicContentListService;
import th.co.nostrasdk.query.dynamic.NTDynamicContentResult;
import th.co.nostrasdk.share.link.NTShortLinkParameter;
import th.co.nostrasdk.share.link.NTShortLinkResult;
import th.co.nostrasdk.share.link.NTShortLinkService;

public class DynamicContentActivity extends AppCompatActivity {
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
    private LocationDisplay locationDisplay;

    private GraphicsOverlay graphicsOverlay;
    private SpatialReference webMercator = SpatialReference.create(102100);
    private NTMapPermissionResult[] ntMapResults;
    private NTDynamicContentListResult[] dynamicLayers;
    private double lat, lon;
    private DMCResultFragment resultFragment;
    private LocationManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dynamic_map_content);

        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
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
                    LocationDisplay ldm = mapView.getLocationDisplay();
                    ldm.setAutoPanMode(LocationDisplay.AutoPanMode.OFF);
                    ldm.startAsync();
                }
            }
        });
    }

    private void initializeMap() {
        // Call map service to add map layer
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

                    UserCredential credentials =  UserCredential.createFromToken(token,referrer);

                    ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer(url);
                    tiledLayer.setCredential(credentials);
                    Basemap baseMap = new Basemap(tiledLayer);
                    ArcGISMap mMap = new ArcGISMap(baseMap);
                    mapView.setMap(mMap);
                    mMap.addDoneLoadingListener(doneLoadingListener);
                }
                // Add graphic layer for add pin
                graphicsOverlay = new GraphicsOverlay();
                mapView.getGraphicsOverlays().add(graphicsOverlay);

            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(DynamicContentActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private Runnable doneLoadingListener = new Runnable() {
        @Override
        public void run() {
            // Zoom to current location
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationDisplay = mapView.getLocationDisplay();
                locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.OFF);
                // For the first run only
                locationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
                    boolean firstRun = true;

                    @Override
                    public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {

                        Point location = locationChangedEvent.getLocation().getPosition();
                        lat = location.getY();
                        lon = location.getX();
                        if (firstRun) {
                            Point p = new Point(lon, lat, SpatialReferences.getWgs84());
                            mapView.setViewpointCenterAsync(p, 1);
                            firstRun = false;
                        }
                    }
                });

                locationDisplay.startAsync();
            }
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

    private void generateLayerList() {
        // Call service and show in drawer list
        NTDynamicContentListService.executeAsync(new ServiceRequestListener<NTDynamicContentListResultSet>() {
            @Override
            public void onResponse(NTDynamicContentListResultSet resultSet) {
                dynamicLayers = resultSet.getResults();
                DMCLayerAdapter layerAdapter = new DMCLayerAdapter(dynamicLayers, new DMCLayerAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(int position) {
                        // Show result in fragment
                        resultFragment = DMCResultFragment.newInstance(dynamicLayers[position].getLayerId(), lat, lon);
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
            public void onError(String errorMessage, int statusCode) {
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

    void showOnMap(PoiItem poiItem) {
        // Show result location on map
        frlContainer.setVisibility(View.GONE);
        imvBack.setVisibility(View.VISIBLE);
        imvLayer.setVisibility(View.GONE);
        txvHeader.setText(R.string.display_on_map);

        Point p = new Point(poiItem.getLongitude(), poiItem.getLatitude(), SpatialReferences.getWgs84());
        Drawable drawable = ContextCompat
                .getDrawable(this, R.drawable.pin_markonmap);
        ListenableFuture<PictureMarkerSymbol> pin =  PictureMarkerSymbol.createAsync((BitmapDrawable)drawable);
        try {
            graphicsOverlay.getGraphics().add(new Graphic(p, pin.get()));
            mapView.setViewpointCenterAsync(p, 50000);
        } catch (ExecutionException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

    }

    void createShareUrl(PoiItem poiItem) {
        if (poiItem == null)
            return;

        curtainView.setVisibility(View.VISIBLE);
        rllShare.setVisibility(View.VISIBLE);
        NTShortLinkParameter param = new NTShortLinkParameter(new NTLocation(poiItem.getLocalName(),poiItem.getLatitude(),poiItem.getLongitude()));
        param.setLanguage(NTLanguage.LOCAL);
        // Call share service and show the url
        NTShortLinkService.executeAsync(param, new ServiceRequestListener<NTShortLinkResult>() {
            @Override
            public void onResponse(NTShortLinkResult result) {
                txvShareUrl.setText(result.getUrl());
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(DynamicContentActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
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
                txvHeader.setText(R.string.dynamic_map_content);
                imvLayer.setVisibility(View.VISIBLE);
                imvBack.setVisibility(View.GONE);
                frlContainer.setVisibility(View.VISIBLE);
                graphicsOverlay.getGraphics().clear();
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.OFF);
                    locationDisplay.startAsync();
                }
                return;
            }
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        mapView.getLocationDisplay().stop();
        mapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.resume();
        LocationDisplay locationDisplay = mapView.getLocationDisplay();
        if (!locationDisplay.isStarted()) {
            mapView.getLocationDisplay().startAsync();
        }
    }

    @Override
    protected void onDestroy() {
        mapView.getLocationDisplay().stop();
        super.onDestroy();
    }
}