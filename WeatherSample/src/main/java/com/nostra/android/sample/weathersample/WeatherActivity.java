package com.nostra.android.sample.weathersample;

import android.annotation.SuppressLint;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.UserCredential;

import java.util.Locale;

import th.co.nostrasdk.NTSDKEnvironment;
import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.info.weather.NTWeather;
import th.co.nostrasdk.info.weather.NTWeatherParameter;
import th.co.nostrasdk.info.weather.NTWeatherResult;
import th.co.nostrasdk.info.weather.NTWeatherService;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;

public class WeatherActivity extends AppCompatActivity {

    private MapView mapView;
    private TextView txvTime;
    private TextView txvAvgTemperature;
    private TextView txvTemperatureMax;
    private TextView txvTemperatureMin;
    private TextView txvLocation;
    private TextView txvWeather;
    private ImageView imvIcon;

    private NTMapPermissionResult[] ntMapResults;
    private Point point;
    private GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
    private String locationName;
    private String urlIcon;
    private double temperature;
    private double temperatureMin;
    private double temperatureMax;
    private String description;
    private String time;
    private BottomSheetBehavior bottomSheetBehavior;
    private Callout mapCallout;
    private boolean locationChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);

        // TODO: Setting Licence ID
        ArcGISRuntimeEnvironment.setLicense("Licence_ID");

        mapView = (MapView) findViewById(R.id.mapView);
        txvTime = (TextView) findViewById(R.id.txvTime);
        txvAvgTemperature = (TextView) findViewById(R.id.txvAvgTemperature);
        txvTemperatureMax = (TextView) findViewById(R.id.txvTemperatureMax);
        txvTemperatureMin = (TextView) findViewById(R.id.txvTemperatureMin);
        txvLocation = (TextView) findViewById(R.id.txvLocation);
        txvWeather = (TextView) findViewById(R.id.txvWeather);
        imvIcon = (ImageView) findViewById(R.id.imvIcon);
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet_layout));
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        //Add layer map
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

                    UserCredential credentials = UserCredential.createFromToken(token,referrer);

                    ArcGISTiledLayer layer = new ArcGISTiledLayer(url);
                    layer.setCredential(credentials);
                    double lat = 0;
                    double lon = 0;
                    point = new Point(lat, lon, SpatialReferences.getWgs84());

                    Basemap basemap = new Basemap(layer);
                    Envelope env = new Envelope(
                            1.0672849926751213E7,
                            593515.9027621585,
                            1.1905414975501748E7,
                            2375599.5357473083,
                            SpatialReference.create(102100)
                    );

                    ArcGISMap mMap = new ArcGISMap(basemap);
                    Viewpoint vp = new Viewpoint(env);
                    mMap.setInitialViewpoint(vp);
                    mapView.setMap(mMap);
                    mMap.addDoneLoadingListener(doneLoadingListener);
                    mapCallout = mapView.getCallout();
                    mapView.getGraphicsOverlays().add(graphicsOverlay);
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(WeatherActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Runnable doneLoadingListener = new Runnable() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void run() {

            LocationDisplay locationManager = mapView.getLocationDisplay();
            locationManager.setAutoPanMode(LocationDisplay.AutoPanMode.OFF);
            locationManager.addLocationChangedListener(locationChangedEvent -> {
                // Zooms to the current txvLocation when first GPS fix arrives.

                if (!locationChanged) {
                    locationChanged = true;
                    Point loc = locationChangedEvent.getLocation().getPosition();
                    double locY = loc.getY();
                    double locX = loc.getX();
                    Point wgsPoint = new Point(locX, locY, SpatialReference.create(4326));
                    mapView.setViewpointCenterAsync(wgsPoint, 50000);
                }

            });

            mapView.setOnTouchListener(
                    new DefaultMapViewOnTouchListener(WeatherActivity.this, mapView) {

                        @Override
                        public boolean onSingleTapConfirmed(MotionEvent e) {
                            if (mapCallout.isShowing() && bottomSheetBehavior.isHideable()) {
                                graphicsOverlay.getGraphics().clear();
                                mapCallout.dismiss();
                                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                            }
                            return true;
                        }

                        @Override
                        public void onLongPress(MotionEvent e) {
                            point = screenToLocation(e.getX(), e.getY());

                            NTWeatherParameter parameter = new NTWeatherParameter(point.getY(), point.getX());
                            NTWeatherService.executeAsync(parameter, new ServiceRequestListener<NTWeatherResult>() {
                                @Override
                                public void onResponse(NTWeatherResult result) {
                                    locationName = result.getLocationName();
                                    NTWeather[] weathers = result.getWeathers();
                                    if (weathers.length > 0) {
                                        urlIcon = weathers[0].getIcon();
                                        temperature = weathers[0].getTemperature().getAverage();
                                        temperatureMin = weathers[0].getTemperature().getMin();
                                        temperatureMax = weathers[0].getTemperature().getMax();
                                        time = weathers[0].getDatetime();
                                        description = weathers[0].getWeatherDescription();

                                        // Sets custom content view to Callout
                                        mapCallout = mapView.getCallout();
                                        if (mapCallout != null && mapCallout.isShowing()) {
                                            mapCallout.dismiss();
                                        }
                                        mapCallout.setContent(createCalloutView());
                                        mapCallout.setLocation(point);
                                        mapCallout.show();
                                    }
                                }

                                @Override
                                public void onError(String errorMessage, int statusCode) {
                                    Toast.makeText(WeatherActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
        }
    };

    private Point screenToLocation(Float x, Float y) {
        try {
            Point mapPoint = mapView.screenToLocation(new android.graphics.Point(Math.round(x), Math.round(y)));
            return (Point) GeometryEngine.project(mapPoint, SpatialReferences.getWgs84());
        } catch (NullPointerException e) {
            return new Point(0, 0);
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

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            super.onBackPressed();
        }
    }

    //Content Callout
    private View createCalloutView() {
        View view = LayoutInflater.from(WeatherActivity.this).inflate(R.layout.activity_callout, null);
        final ImageView newIcon = (ImageView) view.findViewById(R.id.imvWeather);
        final TextView txvTemperature = (TextView) view.findViewById(R.id.temperature);
        txvTemperature.setText(String.format(Locale.ENGLISH, "%.2f \u00b0", temperature));

        //Load picture from URL
        new LoadPictureTask(newIcon, imvIcon).execute(urlIcon);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Pan to point
                mapView.setViewpointCenterAsync(point, 50000);

                // Display detail as bottom sheet
                txvTime.setText(time);
                txvAvgTemperature.setText(String.format(Locale.ENGLISH, "%.2f \u00b0", temperature));
                txvTemperatureMax.setText(String.format(Locale.ENGLISH, "%.2f \u00b0", temperatureMax));
                txvTemperatureMin.setText(String.format(Locale.ENGLISH, "%.2f \u00b0", temperatureMin));
                txvLocation.setText(locationName);
                txvWeather.setText(description);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return view;
    }
}
