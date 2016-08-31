package com.nostra.android.sample.mapsample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.ogc.WMSLayer;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.Point;
import com.esri.core.io.UserCredentials;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import th.co.gissoft.nostrasdk.Base.IServiceRequestListener;
import th.co.gissoft.nostrasdk.Base.NTMapPermissionService;
import th.co.gissoft.nostrasdk.Base.NTSDKEnvironment;
import th.co.gissoft.nostrasdk.Parameter.Class.NTPoint;
import th.co.gissoft.nostrasdk.Parameter.Constant.NTLayerType;
import th.co.gissoft.nostrasdk.Parameter.Constant.NTMapServiceType;
import th.co.gissoft.nostrasdk.Result.NTMapPermissionResult;
import th.co.gissoft.nostrasdk.Result.NTMapPermissionResultSet;

public class MapActivity extends AppCompatActivity {
    private ListView lvBaseMap;
    private ListView lvLayerMap;
    private MapView mapView;

    private DrawerLayout drawerLayout;
    private NTMapPermissionResult[] ntMapResults;
    private List<Integer> serviceOrderList;
    private Point mapPoint;

    private List<String> baseMapList;
    private List<String> layerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

        mapView = (MapView) findViewById(R.id.mapView);
        lvBaseMap = (ListView) findViewById(R.id.lvBaseMap);
        lvLayerMap = (ListView) findViewById(R.id.lvLayerMap);
        serviceOrderList = new ArrayList<>();

        // Add map
        NTMapPermissionService.executeAsync(new IServiceRequestListener<NTMapPermissionResultSet>() {
            @Override
            public void onResponse(NTMapPermissionResultSet result, String responseCode) {
                ntMapResults = result.getResults();
                // Bind layer list
                bindMapLayer();
                // Display default map
                lvBaseMap.performItemClick(lvBaseMap, 1, 1);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        ImageView imvMenuSpecial = (ImageView) findViewById(R.id.imvMenuSpecial);

        lvBaseMap.setOnItemClickListener(baseMapItemClick);
        lvLayerMap.setOnItemClickListener(layerItemClick);

        imvMenuSpecial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        ImageView imvCurrentLocation = (ImageView) findViewById(R.id.imvCurrentLocation);
        imvCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.centerAt(mapPoint, true);
            }
        });
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

    private int findSuiteIndexForLayer(int orderIndex) {
        if (serviceOrderList.isEmpty()) {
            return 0;
        } else {
            int size = serviceOrderList.size();
            int suiteIndex = size;
            for (int i = 0; i < size; i++) {
                if (orderIndex < serviceOrderList.get(i)) {
                    suiteIndex = i;
                    break;
                }
            }
            return suiteIndex;
        }
    }

    private void bindMapLayer() {
        baseMapList = new ArrayList<>();
        layerList = new ArrayList<>();

        if (ntMapResults != null) {
            for (NTMapPermissionResult map : ntMapResults) {
                // Error on map serviceId = 5, so we skip for now.
                if (map.getServiceId() != 5) {
                    if (map.getLayerType() == NTLayerType.TYPE_BASEMAP ||
                            map.getLayerType() == NTLayerType.TYPE_IMAGERY) {
                        baseMapList.add(map.getServiceName());
                    } else if (map.getLayerType() == NTLayerType.TYPE_SPECIAL_LAYER) {
                        layerList.add(map.getServiceName());
                    }
                    // Add all layer with hide by default.
                    addMapLayer(map);
                }
            }
            ArrayAdapter<String> baseMapAdapter = new ArrayAdapter<>(MapActivity.this,
                    android.R.layout.simple_list_item_single_choice, baseMapList);
            lvBaseMap.setAdapter(baseMapAdapter);
            lvBaseMap.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            ArrayAdapter<String> layerAdapter = new ArrayAdapter<>(MapActivity.this,
                    android.R.layout.simple_list_item_multiple_choice, layerList);
            lvLayerMap.setAdapter(layerAdapter);
            lvLayerMap.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }
    }

    private AdapterView.OnItemClickListener baseMapItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            lvBaseMap.setItemChecked(position, true);

            drawerLayout.closeDrawer(GravityCompat.START);
            String serviceName = parent.getItemAtPosition(position).toString();
            visibleOnlyLayerByName(serviceName);
        }
    };

    private AdapterView.OnItemClickListener layerItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String serviceName = parent.getItemAtPosition(position).toString();
            setLayerVisibleByName(serviceName, lvLayerMap.isItemChecked(position));
        }
    };

    private void addMapLayer(@NonNull NTMapPermissionResult map) {
        String url = map.getServiceUrl_L();
        String token = map.getServiceToken_L();
        String referrer = "Referrer";    // TODO: Insert referrer
        boolean isImageryMap = map.getLayerType() == NTLayerType.TYPE_IMAGERY;

        if (!isImageryMap && (TextUtils.isEmpty(url) || TextUtils.isEmpty(token))) {
            // Do nothing.
        } else {
            UserCredentials credentials = null;
            if (!isImageryMap) {
                credentials = new UserCredentials();
                credentials.setUserToken(token, referrer);
                credentials.setAuthenticationType(UserCredentials.AuthenticationType.TOKEN);
            }
            int mapSortIndex = map.getSortIndex();
            int index = findSuiteIndexForLayer(mapSortIndex);

            // Initiate layer by type.
            Layer layer = null;
            if (map.getMapServiceType() == NTMapServiceType.TYPE_TILED_SERVICE) {
                layer = new ArcGISTiledMapServiceLayer(url, credentials);
            } else if(map.getMapServiceType() == NTMapServiceType.TYPE_DYNAMIC_SERVICE) {
                layer = new ArcGISDynamicMapServiceLayer(url, new int[] {}, credentials);
            } else if (map.getMapServiceType() == NTMapServiceType.TYPE_FEATURE_SERVICE) {
                ArcGISFeatureLayer.Options options = new ArcGISFeatureLayer.Options();
                options.mode = ArcGISFeatureLayer.MODE.ONDEMAND;
                layer = new ArcGISFeatureLayer(url, options, credentials);
            } else if (map.getMapServiceType() == NTMapServiceType.TYPE_WEB_MAP_SERVICE) {
                layer = new WMSLayer(url, null, true, null, credentials, true);
            }

            // Add new layer and hide all layer by default.
            if (layer != null) {
                layer.setName(map.getServiceName());
                layer.setVisible(false);
                mapView.addLayer(layer, index);
                serviceOrderList.add(index, mapSortIndex);
            }
        }
    }

    private void visibleOnlyLayerByName(String name) {
        // Loop for check
        List<String> openedLayerName = new ArrayList<>();
        openedLayerName.add(name);

        NTMapPermissionResult map = findMapServiceByName(name);
        if (map != null) {
            // Add all depend layer name.
            if (map.getDependMap() != null) {
                int[] dependMap = map.getDependMap();
                for (int dependId : dependMap) {
                    String dependName = findMapServiceNameById(dependId);
                    if (dependName != null && dependName.length() > 0) {
                        openedLayerName.add(dependName);
                    }
                }
            }
        }

        for (Layer layer : mapView.getLayers()) {
            if (openedLayerName.contains(layer.getName())) {
                int position = layerList.indexOf(layer.getName());
                if (position >= 0) {
                    lvLayerMap.setItemChecked(position, true);
                }
                layer.setVisible(true);

                // Zoom to center of layer (if possible)
                if (TextUtils.equals(name, layer.getName()) && map != null) {
                    NTPoint ntPoint = map.getDefaultZoom();
                    if (ntPoint != null) {
                        mapPoint = new Point(ntPoint.getX(), ntPoint.getY());
                        mapView.centerAndZoom(ntPoint.getX(), ntPoint.getY(), map.getDefaultLevel());
                    }
                }
            } else {
                int position = layerList.indexOf(layer.getName());
                if (position >= 0) {
                    lvLayerMap.setItemChecked(position, false);
                }
                layer.setVisible(false);
            }
        }
    }

    // For layer list
    private void setLayerVisibleByName(String name, boolean visible) {
        for (Layer layer : mapView.getLayers()) {
            if (TextUtils.equals(layer.getName(), name)) {
                layer.setVisible(visible);
                break;
            }
        }
    }

    private NTMapPermissionResult findMapServiceByName(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        NTMapPermissionResult targetMap = null;
        for (NTMapPermissionResult map : ntMapResults) {
            if (TextUtils.equals(map.getServiceName(), name)) {
                targetMap = map;
            }
        }
        return targetMap;
    }

    private String findMapServiceNameById(int serviceId) {
        String serviceName = null;
        for (NTMapPermissionResult map : ntMapResults) {
            if (map.getServiceId() == serviceId) {
                serviceName = map.getServiceName();
            }
        }
        return serviceName;
    }
}
