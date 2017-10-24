package com.nostra.android.sample.weathersample;

import android.location.Location;
import android.location.LocationListener;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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

public class WeatherActivity extends AppCompatActivity
        implements OnStatusChangedListener, OnSingleTapListener, OnLongPressListener {

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
    private GraphicsLayer graphicsLayer = new GraphicsLayer();
    private String locationName;
    private String urlIcon;
    private double temperature;
    private double temperatureMin;
    private double temperatureMax;
    private String description;
    private String time;
    private BottomSheetBehavior bottomSheetBehavior;
    private Callout mapCallout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("TOKEN_SDK", this);
        // TODO: Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

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

                    UserCredentials credentials = new UserCredentials();
                    credentials.setUserToken(token, referrer);
                    credentials.setAuthenticationType(UserCredentials.AuthenticationType.TOKEN);

                    ArcGISTiledMapServiceLayer layer = new ArcGISTiledMapServiceLayer(url, credentials);
                    double lat = 0;
                    double lon = 0;
                    point = new Point(lat, lon);
                    String s = CoordinateConversion.pointToDecimalDegrees(point, SpatialReference.create
                            (SpatialReference.WKID_WGS84), 7);
                    point = CoordinateConversion.decimalDegreesToPoint(s, SpatialReference.create
                            (SpatialReference.WKID_WGS84_WEB_MERCATOR));
                    mapView.addLayer(layer);
                    mapCallout = mapView.getCallout();
                    mapView.centerAt(point, true);
                    mapView.addLayer(graphicsLayer);
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(WeatherActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        // Current Location
        mapView.setOnStatusChangedListener(this);
        mapView.setOnLongPressListener(this);
        mapView.setOnSingleTapListener(this);
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
    public void onStatusChanged(Object source, STATUS status) {
        if (source == mapView && status == OnStatusChangedListener.STATUS.INITIALIZED) {
            LocationDisplayManager locationManager = mapView.getLocationDisplayManager();
            locationManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
            locationManager.setLocationListener(new LocationListener() {
                boolean locationChanged = false;

                // Zooms to the current txvLocation when first GPS fix arrives.
                @Override
                public void onLocationChanged(Location loc) {
                    if (!locationChanged) {
                        locationChanged = true;
                        double locY = loc.getLatitude();
                        double locX = loc.getLongitude();
                        Point wgsPoint = new Point(locX, locY);
                        Point mapPoint = (Point) GeometryEngine.project(wgsPoint,
                                SpatialReference.create(4326),
                                mapView.getSpatialReference());

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
            });  // Action Listener
            locationManager.start();
        }
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onLongPress(float x, float y) {
        point = mapView.toMapPoint(x, y);
        // Convert WebMercator coordinate to WGS coordinate
        Point wgsPoint = (Point) GeometryEngine.project(point,
                SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE),
                SpatialReference.create(SpatialReference.WKID_WGS84));

        NTWeatherParameter parameter = new NTWeatherParameter(wgsPoint.getY(), wgsPoint.getX());
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
                        mapCallout.hide();
                    }
                    mapCallout.setContent(createCalloutView());
                    mapCallout.setCoordinates(point);
                    mapCallout.show(point);
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(WeatherActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        return true;
    }

    @Override
    public void onSingleTap(float v, float v1) {
        if (mapView.isLoaded()) {
            if (mapCallout.isShowing() && bottomSheetBehavior.isHideable()) {
                graphicsLayer.removeAll();
                mapCallout.hide();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        } else {
            Toast.makeText(WeatherActivity.this, "Map Loading", Toast.LENGTH_SHORT).show();
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
                mapView.centerAt(point, true);

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
