package com.nostra.android.sample.measurementtoolssample;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.loadable.LoadStatusChangedListener;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;

import java.util.concurrent.ExecutionException;

import th.co.nostrasdk.NTSDKEnvironment;
import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private ImageButton clearButton;
    private ImageButton currenLocationButton;
    private TextView tvDistance;
    private GraphicsOverlay lineGraphicOverlays;
    private GraphicsOverlay dotGraphicOverlays;
    private GraphicsOverlay greyDotGraphicOverlays;
    private GraphicsOverlay pictureGraphicoverlays;
    private LocationDisplay locationDisplay;
    private PointCollection pointCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // TODO: Setting Licence ID
        ArcGISRuntimeEnvironment.setLicense("Licence_ID");
        initInstances();
        setupMap();
    }

    //initial view
    private void initInstances() {
        mapView = findViewById(R.id.mapView);
        clearButton = findViewById(R.id.btnClear);
        tvDistance = findViewById(R.id.tvDistance);
        currenLocationButton = findViewById(R.id.btnCurrentLocation);

        //Create empty graphic overlays
        lineGraphicOverlays = new GraphicsOverlay();
        dotGraphicOverlays = new GraphicsOverlay();
        greyDotGraphicOverlays = new GraphicsOverlay();
        pictureGraphicoverlays = new GraphicsOverlay();
        locationDisplay = mapView.getLocationDisplay();

        //create point collection to storing point which is wgs84 point
        pointCollection = new PointCollection(SpatialReferences.getWgs84());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.resume();
        if (!locationDisplay.isStarted()) {
            locationDisplay.startAsync();
        }
    }

    @Override
    protected void onPause() {
        mapView.pause();
        locationDisplay.stop();
        super.onPause();
    }

    //init map
    private void setupMap() {
        //Hide attribution text watermark
        mapView.setAttributionTextVisible(false);

        //Set wraparound mode enable
        mapView.setWrapAroundMode(WrapAroundMode.ENABLE_WHEN_SUPPORTED);

        //Add graphicOverlays into map for waiting to any graphics.
        mapView.getGraphicsOverlays().add(lineGraphicOverlays);
        mapView.getGraphicsOverlays().add(dotGraphicOverlays);
        mapView.getGraphicsOverlays().add(greyDotGraphicOverlays);
        mapView.getGraphicsOverlays().add(pictureGraphicoverlays);

        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // TODO: Setting Licence ID
        ArcGISRuntimeEnvironment.setLicense("Licence_ID");

        //Add layer map
        NTMapPermissionService.executeAsync(new ServiceRequestListener<NTMapPermissionResultSet>() {
            @Override
            public void onResponse(NTMapPermissionResultSet result) {
                NTMapPermissionResult[] ntMapResults = result.getResults();
                NTMapPermissionResult map = getThailandBasemap(ntMapResults);
                if (map != null) {
                    NTMapServiceInfo info = map.getLocalService();
                    String url = info.getServiceUrl();
                    String token = info.getServiceToken();
                    // TODO: Insert referrer
                    String referrer = "REFERRER";

                    //If need to create credential by username and password see use UserCredential("userName","password")
                    UserCredential credentials = UserCredential.createFromToken(token, referrer);

                    //Create tiled layer from url of map service.
                    ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer(url);
                    tiledLayer.setCredential(credentials);
                    //Create baseMap from tiled layer.
                    Basemap basemap = new Basemap(tiledLayer);

                    ArcGISMap mMap = new ArcGISMap(basemap);

                    //Create envelop from default extent of Thailand
                    Envelope env = new Envelope(
                            1.0672849926751213E7,
                            593515.9027621585,
                            1.1905414975501748E7,
                            2375599.5357473083,
                            SpatialReference.create(102100)
                    );
                    //Create view point from above envelop.
                    // then set it as initial viewpoint
                    Viewpoint vp = new Viewpoint(env);
                    mMap.setInitialViewpoint(vp);
                    mapView.setMap(mMap);
                    //add listener when map's loading status has changed
                    mMap.addLoadStatusChangedListener(onLoadStatusChangeListener);
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Log.i("load status", errorMessage + "" + statusCode);
            }
        });
    }

    //get Thailand base map from ntMapResults
    private NTMapPermissionResult getThailandBasemap(NTMapPermissionResult[] ntMapResults) {
        for (NTMapPermissionResult result : ntMapResults) {
            // Thailand basemap service id is 2
            if (result.getServiceId() == 2) {
                return result;
            }
        }
        return null;
    }

    private LocationDisplay.LocationChangedListener locationChangedListener
            = locationChangeListener -> {
        Point p = locationChangeListener.getLocation().getPosition();
        Log.i("location", p.getX() + "," + p.getY());
    };

    private View.OnClickListener onClickListener = v -> {
        switch (v.getId()) {
            case R.id.btnClear:
                pointCollection.clear();
                clearAllGraphics();
                greyDotGraphicOverlays.getGraphics().clear();
                break;
            case R.id.btnCurrentLocation:
                if (locationDisplay != null && locationDisplay.getLocation() != null
                        && locationDisplay.getLocation().getPosition() != null) {
                    Point p = locationDisplay.getLocation().getPosition();
                    int lastPosition = pointCollection.size() - 1;

                    if (lastPosition >= 0 && p != pointCollection.get(lastPosition)) {
                        addPointToCollection(p, false);
                    }

                    mapView.setViewpointCenterAsync(p, mapView.getMapScale());
                }
                break;
            default:
                break;
        }

    };

    private LoadStatusChangedListener onLoadStatusChangeListener
            = loadStatusChangedEvent -> {
        if (loadStatusChangedEvent.getNewLoadStatus() == LoadStatus.LOADED) {
            //when map load completed
            //TODO set view listener which related with map here to prevent app from crashing.
            clearButton.setOnClickListener(onClickListener);

            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
            locationDisplay.addLocationChangedListener(locationChangedListener);
            locationDisplay.startAsync();

            currenLocationButton.setOnClickListener(onClickListener);
            setMapTouchable(MainActivity.this);
        } else {
            Log.i("load status", loadStatusChangedEvent.getNewLoadStatus().name());
        }
    };

    //split function for easily reading.
    @SuppressLint("ClickableViewAccessibility")
    private void setMapTouchable(Context context) {
        mapView.setOnTouchListener(new DefaultMapViewOnTouchListener(context, mapView) {

            //handle event when user tab on map
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Point point = screenToLocation(e.getX(), e.getY());
                addPointToCollection(point, false);
                return true;
            }

            //handle event when user tab and hoed on map
            @Override
            public void onLongPress(MotionEvent e) {
                Point point = screenToLocation(e.getX(), e.getY());
                addPointToCollection(point, true);
            }
        });
    }

    private void addPointToCollection(Point point, boolean isPicture) {
        pointCollection.add(point);

        //Clear line before redraw
        lineGraphicOverlays.getGraphics().clear();

        //Create geometry of line from pointCollection
        Polyline polyline = new Polyline(pointCollection);

        ListenableFuture<PictureMarkerSymbol> pictureMarkerSymbol = null;
        SimpleMarkerSymbol blueDotSymbol = null;

        if (isPicture) {
            BitmapDrawable launcherSymbol = (BitmapDrawable) ResourcesCompat.
                    getDrawable(getResources(), R.drawable.pin_markonmap, getTheme());
            pictureMarkerSymbol = PictureMarkerSymbol.createAsync(launcherSymbol);

        } else {
            //Create blue circle
            blueDotSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,
                    Color.BLUE, 18);
        }

        //Create yellow line
        SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID,
                Color.YELLOW, 5f);


        //Create graphic from geometry and symbol
        Graphic lineGraphic = new Graphic(polyline, lineSymbol);
        if (blueDotSymbol != null) {
            Graphic dotGraphic = new Graphic(point, blueDotSymbol);
            dotGraphicOverlays.getGraphics().add(dotGraphic);
        }
        if (pictureMarkerSymbol != null) {
            try {
                Graphic pinGraphic = new Graphic(point, pictureMarkerSymbol.get());
                dotGraphicOverlays.getGraphics().add(pinGraphic);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        lineGraphicOverlays.getGraphics().add(lineGraphic);

        calDistance();
    }


    private void clearAllGraphics() {
        lineGraphicOverlays.getGraphics().clear();
        dotGraphicOverlays.getGraphics().clear();
        tvDistance.setText("0 m.");
    }

    //Create Point from screen location
    private Point screenToLocation(Float x, Float y) {
        try {
            Point mapPoint = mapView.screenToLocation(new android.graphics.Point(Math.round(x), Math.round(y)));
            //Use geometry engine convert 'Point(x,y)' to 'Point(longitude,latitude)'
            return (Point) GeometryEngine.project(mapPoint, SpatialReferences.getWgs84());
        } catch (NullPointerException e) {
            return new Point(0, 0);
        }
    }

    //Calculate distance between point
    private void calDistance() {
        if (pointCollection.size() > 1) {
            double distance = 0.0;
            for (int i = 1; i <= pointCollection.size() - 1; i++) {
                Point p0 = pointCollection.get(i - 1);
                Point p1 = pointCollection.get(i);

                distance += Calculator.calDistance(p0.getY(), p0.getX(), p1.getY(), p1.getX());
            }

            String mDistance = String.format("%.0f m.", distance);
            tvDistance.setText(mDistance);
        }
    }

}