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

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISMapImageLayer;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.layers.WmsLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.UserCredential;

import java.util.ArrayList;
import java.util.List;

import th.co.nostrasdk.NTSDKEnvironment;
import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.common.NTPoint;
import th.co.nostrasdk.map.NTMapLayerType;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;
import th.co.nostrasdk.map.NTMapServiceType;

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

        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // TODO: Setting Licence ID
        ArcGISRuntimeEnvironment.setLicense("Licence_ID");

        mapView = (MapView) findViewById(R.id.mapView);
        lvBaseMap = (ListView) findViewById(R.id.lvBaseMap);
        lvLayerMap = (ListView) findViewById(R.id.lvLayerMap);
        serviceOrderList = new ArrayList<>();
        mapView.setMap(new ArcGISMap());
        // Add map
        NTMapPermissionService.executeAsync(new ServiceRequestListener<NTMapPermissionResultSet>() {
            @Override
            public void onResponse(NTMapPermissionResultSet result) {
                ntMapResults = result.getResults();
                // Bind layer list
                bindMapLayer();
                // Display default map
                lvBaseMap.performItemClick(lvBaseMap, 1, 1);
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(MapActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
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
                mapView.setViewpointCenterAsync(mapPoint, 5000);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.resume();
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
                if (map.getLayerType() == NTMapLayerType.TYPE_BASEMAP ||
                        map.getLayerType() == NTMapLayerType.TYPE_IMAGERY) {
                    baseMapList.add(map.getName());
                } else if (map.getLayerType() == NTMapLayerType.TYPE_SPECIAL_LAYER) {
                    layerList.add(map.getName());
                }
                // Add all layer with hide by default.
                addMapLayer(map);
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
        NTMapServiceInfo info = map.getLocalService();
        String url = info.getServiceUrl();
        String token = info.getServiceToken();
        // TODO: Insert referrer
        String referrer = "REFERRER";
        boolean isImageryMap = map.getLayerType() == NTMapLayerType.TYPE_IMAGERY;

        // If layer is NOT imagery, requires url or token to access.
        // Only imagery layer can be used without user credentials.
        if (!isImageryMap && (TextUtils.isEmpty(url) || TextUtils.isEmpty(token)))
            return;

        UserCredential credentials = null;
        if (!isImageryMap) {
            credentials = UserCredential.createFromToken(token, referrer);
        }
        int mapSortIndex = map.getSortIndex();
        int index = findSuiteIndexForLayer(mapSortIndex);

        // Initiate layer by type.
        Layer layer = null;
        if (map.getMapServiceType() == NTMapServiceType.TILED_MAP_SERVICE) {
            layer = new ArcGISTiledLayer(url);
            ((ArcGISTiledLayer) layer).setCredential(credentials);
        } else if (map.getMapServiceType() == NTMapServiceType.DYNAMIC_MAP_SERVICE) {
            layer = new ArcGISMapImageLayer(url);
            ((ArcGISMapImageLayer) layer).setCredential(credentials);
        } else if (map.getMapServiceType() == NTMapServiceType.FEATURE_SERVICE) {
            ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable(url);
            serviceFeatureTable.setCredential(credentials);
            layer = new FeatureLayer(serviceFeatureTable);
            ((FeatureLayer) layer).setRenderingMode((FeatureLayer.RenderingMode.STATIC));

        } else if (map.getMapServiceType() == NTMapServiceType.WEB_MAP_SERVICE) {
            List<String> wmsLayerNames = new ArrayList<>();
            wmsLayerNames.add("1");
            layer = new WmsLayer(url, wmsLayerNames);
        }

        // Add new layer and hide all layer by default.
        if (layer != null) {
            layer.setName(map.getName());
            layer.setVisible(false);
            mapView.getMap().getOperationalLayers().add(layer);
            serviceOrderList.add(index, mapSortIndex);
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

        for (Layer layer : mapView.getMap().getOperationalLayers()) {
            if (openedLayerName.contains(layer.getName())) {
                int position = layerList.indexOf(layer.getName());
                if (position >= 0) {
                    lvLayerMap.setItemChecked(position, true);
                }
                layer.setVisible(true);

                // Zoom to center of layer (if possible)
                if (TextUtils.equals(name, layer.getName()) && map != null) {
                    NTPoint ntPoint = map.getDefaultLocation();
                    if (ntPoint != null) {
                        mapPoint = new Point(ntPoint.getX(), ntPoint.getY(), SpatialReferences.getWgs84());
                        mapView.setViewpointCenterAsync(
                                mapPoint, 50000);
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
        for (Layer layer : mapView.getMap().getOperationalLayers()) {
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
            if (TextUtils.equals(map.getName(), name)) {
                targetMap = map;
            }
        }
        return targetMap;
    }

    private String findMapServiceNameById(int serviceId) {
        String serviceName = null;
        for (NTMapPermissionResult map : ntMapResults) {
            if (map.getServiceId() == serviceId) {
                serviceName = map.getName();
            }
        }
        return serviceName;
    }
}
