package com.nostra.android.sample.routesample;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.CoordinateConversion;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;

import java.util.Locale;

import th.co.nostrasdk.NTSDKEnvironment;
import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.common.NTImpedanceMode;
import th.co.nostrasdk.common.NTLanguage;
import th.co.nostrasdk.common.NTTravelMode;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;
import th.co.nostrasdk.network.NTDirection;
import th.co.nostrasdk.network.NTLocation;
import th.co.nostrasdk.network.NTPoint;
import th.co.nostrasdk.network.route.NTRouteParameter;
import th.co.nostrasdk.network.route.NTRouteResult;
import th.co.nostrasdk.network.route.NTRouteService;

public class RouteActivity extends AppCompatActivity implements OnStatusChangedListener {

    private static final int REQ_FROM_LOCATION = 0;
    private static final int REQ_TO_LOCATION = 1;

    private MapView mapView;
    private Button edtFromLocation;
    private Button edtToLocation;
    private Button btnVehicle;
    private TextView txvMinutes;
    private RelativeLayout relativeLayout;
    private ListView lvVehicle;
    private ImageView imvCurrentLocation;
    private ImageButton imbNavigation;
    private ImageButton imbDirection;

    private GraphicsLayer lineSymbolGraphicLayer = new GraphicsLayer();
    private GraphicsLayer pinGraphicLayer = new GraphicsLayer();

    private NTMapPermissionResult[] ntMapResults;
    private LocationDisplayManager ldm;
    private BottomSheetBehavior bottomSheetBehavior;
    private double fromLon = 0;
    private double fromLat = 0;
    private double toLon = 0;
    private double toLat = 0;
    private int positionGo;

    private Point mapPoint;
    private NTRouteResult ntRouteResult;

    private PictureMarkerSymbol fromMarkerSymbol;
    private PictureMarkerSymbol destMarkerSymbol;
    private SimpleMarkerSymbol directionSymbol;
    private SimpleLineSymbol lineSymbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        //todo Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("GpaFVfndCwAsINg8V7ruX9DNKvwyOOg(OtcKjh7dfAyIppXlmS9I)Q1mT8X0W685UxrXVI6V7XuNSRz7IyuXWSm=====2", this);
        //todo Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.enableWrapAround(true);

        lvVehicle = (ListView) findViewById(R.id.lvVehicle);
        btnVehicle = (Button) findViewById(R.id.btnVehicle);
        txvMinutes = (TextView) findViewById(R.id.txvMinutes);
        relativeLayout = (RelativeLayout) findViewById(R.id.layoutMinute);
        edtFromLocation = (Button) findViewById(R.id.edtFromLocation);
        edtToLocation = (Button) findViewById(R.id.edtToLocation);
        imbNavigation = (ImageButton) findViewById(R.id.imbNavigation);
        imbDirection = (ImageButton) findViewById(R.id.imbDirection);
        imvCurrentLocation = (ImageButton) findViewById(R.id.imbCurrentLocation);
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet));

        // Create symbols
        Drawable fromFlagDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.flag, getTheme());
        fromMarkerSymbol = new PictureMarkerSymbol(fromFlagDrawable);

        Drawable destFlagDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.flag_des, getTheme());
        destMarkerSymbol = new PictureMarkerSymbol(destFlagDrawable);

        int lightGreen = ResourcesCompat.getColor(getResources(), R.color.colorLightGreen, getTheme());
        directionSymbol = new SimpleMarkerSymbol(lightGreen, 16, SimpleMarkerSymbol.STYLE.CIRCLE);

        int green = ResourcesCompat.getColor(getResources(), R.color.colorGreen, getTheme());
        lineSymbol = new SimpleLineSymbol(green, 8F, SimpleLineSymbol.STYLE.SOLID);

        // Add map
        NTMapPermissionService.executeAsync(new ServiceRequestListener<NTMapPermissionResultSet>() {
            @Override
            public void onResponse(NTMapPermissionResultSet result) {
                ntMapResults = result.getResults();
                NTMapPermissionResult map = getThailandBasemap();
                if (map != null) {
                    NTMapServiceInfo info = map.getLocalService();
                    String url = info.getServiceUrl();
                    String token = info.getServiceToken();
                    String referrer = "geotalent_dmd.nostramap.com";    // TODO: Insert referrer

                    UserCredentials credentials = new UserCredentials();
                    credentials.setUserToken(token, referrer);
                    credentials.setAuthenticationType(UserCredentials.AuthenticationType.TOKEN);

                    ArcGISTiledMapServiceLayer layer = new ArcGISTiledMapServiceLayer(url, credentials);
                    mapView.addLayer(layer);
                    // Add graphic layer
                    mapView.addLayer(lineSymbolGraphicLayer);
                    mapView.addLayer(pinGraphicLayer);
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(RouteActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        String[] arrVehicle = new String[]{"CAR", "MOTORCYCLE", "BIKE", "WALK"};
        VehicleAdapter adapter = new VehicleAdapter(RouteActivity.this, arrVehicle);
        lvVehicle.setAdapter(adapter);

        lvVehicle.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    positionGo = position;
                    btnVehicle.setText(R.string.car);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                } else if (position == 1) {
                    positionGo = position;
                    btnVehicle.setText(R.string.motorcycle);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                } else if (position == 2) {
                    positionGo = position;
                    btnVehicle.setText(R.string.bike);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                } else if (position == 3) {
                    positionGo = position;
                    btnVehicle.setText(R.string.walk);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }
        });
        // Set listener
        mapView.setOnStatusChangedListener(this);
        edtFromLocation.setOnClickListener(edtFromClick);
        edtToLocation.setOnClickListener(edtToClick);
        btnVehicle.setOnClickListener(btnVehicleClick);
        imbNavigation.setOnClickListener(btnNavigateClick);
        imbDirection.setOnClickListener(btnDirectionClick);
        imvCurrentLocation.setOnClickListener(imvCurrentLocationClick);

        mapView.setOnSingleTapListener(new OnSingleTapListener() {
            @Override
            public void onSingleTap(float v, float v1) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }
        });
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


    private View.OnClickListener edtFromClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(RouteActivity.this, PinMarkerActivity.class);
            startActivityForResult(intent, REQ_FROM_LOCATION);
        }
    };

    private View.OnClickListener edtToClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(RouteActivity.this, PinMarkerActivity.class);
            startActivityForResult(intent, REQ_TO_LOCATION);
        }
    };

    private View.OnClickListener btnVehicleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    };

    private View.OnClickListener imvCurrentLocationClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mapView.centerAt(mapPoint, true);
        }
    };

    private View.OnClickListener btnNavigateClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            NTLocation[] stops = new NTLocation[]{
                    new NTLocation("To_Location", toLat, toLon),
                    new NTLocation("From_Location", fromLat, fromLon)
            };

            String travelMode;
            switch (positionGo) {
                case 3:
                    travelMode = NTTravelMode.WALK;
                    break;
                case 2:
                    travelMode = NTTravelMode.BICYCLE;
                    break;
                case 1:
                    travelMode = NTTravelMode.MOTORCYCLE;
                    break;
                case 0:
                default:
                    travelMode = NTTravelMode.CAR;
                    break;
            }
            NTRouteParameter param = new NTRouteParameter(stops);
            param.setTravelMode(travelMode);
            param.setImpedance(NTImpedanceMode.TIME);
            param.setLanguage(NTLanguage.LOCAL);
            param.setReturnRouteDetail(true);
            param.setFindBestSequence(true);
            param.setPreserveFirstStop(true);
            param.setPreserveLastStop(true);
            param.setUseTollRoad(true);

            NTRouteService.executeAsync(param, new ServiceRequestListener<NTRouteResult>() {
                @Override
                public void onResponse(NTRouteResult result) {
                    ntRouteResult = result;

                    // Remove previous graphics.
                    lineSymbolGraphicLayer.removeAll();
                    pinGraphicLayer.removeAll();

                    if (ntRouteResult != null) {
                        relativeLayout.setVisibility(View.VISIBLE);
                        txvMinutes.setText(String.format(Locale.ENGLISH, "%.2f นาที", ntRouteResult.getTotalTime()));
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                        try {
                            JsonParser parser = new JsonFactory().createJsonParser(ntRouteResult.getShape());
                            MapGeometry mapGeometry = GeometryEngine.jsonToGeometry(parser);
                            Geometry geometry = mapGeometry.getGeometry();

                            if (geometry != null && !geometry.isEmpty()) {
                                geometry = GeometryEngine.project(geometry,
                                        SpatialReference.create(4326),
                                        SpatialReference.create(102100));
                                Graphic lineSymbolGraphic = new Graphic(geometry, lineSymbol);
                                lineSymbolGraphicLayer.addGraphic(lineSymbolGraphic);
                            }

                            Point fromPoint = new Point(toLon, toLat);
                            fromPoint = (Point) GeometryEngine.project(fromPoint,
                                    SpatialReference.create(4326),
                                    SpatialReference.create(102100));
                            Graphic graphicPinFromLocation = new Graphic(fromPoint, fromMarkerSymbol);

                            Point point = null;
                            if (ntRouteResult.getDirections() != null) {
                                for (NTDirection direction : ntRouteResult.getDirections()) {
                                    NTPoint facilityGeometry = direction.getPoint();
                                    if (facilityGeometry != null) {
                                        point = new Point(facilityGeometry.getX(), facilityGeometry.getY());
                                    }
                                    point = (Point) GeometryEngine.project(point,
                                            SpatialReference.create(4326),
                                            SpatialReference.create(102100));
                                    Graphic pointGraphic = new Graphic(point, directionSymbol);
                                    pinGraphicLayer.addGraphic(pointGraphic);
                                }
                            }
                            Graphic graphicPinToLocation = new Graphic(point, destMarkerSymbol);
                            pinGraphicLayer.addGraphic(graphicPinToLocation);
                            pinGraphicLayer.addGraphic(graphicPinFromLocation);
                            // Set extent
//                            mapView.setExtent(geometry, 100, true);
                        } catch (Exception e) {
                            Toast.makeText(RouteActivity.this, "Error ", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onError(String errorMessage, int statusCode) {
                    Toast.makeText(RouteActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    View.OnClickListener btnDirectionClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (ntRouteResult != null && ntRouteResult.getDirections() != null) {
                NTDirection[] directions = ntRouteResult.getDirections();
                int size = directions.length;

                int[] arrResultLength = new int[size];
                String[] arrResultRouteDirection = new String[size];

                for (int i = 0; i < size; i++) {
                    arrResultLength[i] = (int) directions[i].getLength();
                    arrResultRouteDirection[i] = directions[i].getText();
                }
                Intent intent = new Intent(RouteActivity.this, DirectionActivity.class);
                intent.putExtra("Results_RouteDirections", arrResultRouteDirection);
                intent.putExtra("ResultLength", arrResultLength);
                startActivity(intent);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            double pointX = data.getDoubleExtra("locationX", 0);
            double pointY = data.getDoubleExtra("locationY", 0);
            Point pickPoint = new Point(pointX, pointY);
            String decimalDegrees = CoordinateConversion.pointToDecimalDegrees(pickPoint,
                    mapView.getSpatialReference(), 7);
            final Point wgsPoint = CoordinateConversion.decimalDegreesToPoint(decimalDegrees,
                    SpatialReference.create(SpatialReference.WKID_WGS84));

            switch (requestCode) {
                case REQ_TO_LOCATION:
                    toLon = wgsPoint.getX();
                    toLat = wgsPoint.getY();
                    edtToLocation.setText(String.format(
                            Locale.ENGLISH, "%.6f, %.6f", toLat, toLon));
                    break;
                case REQ_FROM_LOCATION:
                    fromLon = wgsPoint.getX();
                    fromLat = wgsPoint.getY();
                    edtFromLocation.setText(String.format(
                            Locale.ENGLISH, "%.6f, %.6f", fromLat, fromLon));
                    break;
                default:
                    break;
            }
        }
    }

    //Set state bottom sheet
    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onStatusChanged(Object source, STATUS status) {
        if (source == mapView && status == OnStatusChangedListener.STATUS.INITIALIZED) {
            ldm = mapView.getLocationDisplayManager();
            ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
            ldm.setLocationListener(new LocationListener() {
                boolean locationChanged = false;

                // Zooms to the current location when first GPS fix arrives.
                @Override
                public void onLocationChanged(Location loc) {
                    if (!locationChanged) {
                        locationChanged = true;

                        double locY = loc.getLatitude();
                        double locX = loc.getLongitude();
                        Point wgsPoint = new Point(locX, locY);
                        mapPoint = (Point) GeometryEngine
                                .project(wgsPoint,
                                        SpatialReference.create(4326),
                                        mapView.getSpatialReference());

                        Unit mapUnit = mapView.getSpatialReference().getUnit();
                        double zoomWidth = Unit.convertUnits(5,
                                Unit.create(LinearUnit.Code.MILE_US), mapUnit);
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
            ldm.start();
        }
    }
}