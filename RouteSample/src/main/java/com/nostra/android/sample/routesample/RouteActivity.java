package com.nostra.android.sample.routesample;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.geometry.CoordinateFormatter;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.loadable.LoadStatusChangedEvent;
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

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import th.co.nostrasdk.NTSDKEnvironment;
import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.common.NTImpedanceMode;
import th.co.nostrasdk.common.NTLanguage;
import th.co.nostrasdk.common.NTPoint;
import th.co.nostrasdk.common.NTTravelMode;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;
import th.co.nostrasdk.network.NTDirection;
import th.co.nostrasdk.network.NTLocation;
import th.co.nostrasdk.network.route.NTRouteParameter;
import th.co.nostrasdk.network.route.NTRouteResult;
import th.co.nostrasdk.network.route.NTRouteService;

public class RouteActivity extends AppCompatActivity implements LoadStatusChangedListener {

    private static final int REQ_FROM_LOCATION = 0;
    private static final int REQ_TO_LOCATION = 1;

    private MapView mapView;
    private Button edtFromLocation;
    private Button edtToLocation;
    private Button btnVehicle;
    private TextView txvMinutes;
    private RelativeLayout relativeLayout;

    private GraphicsOverlay lineSymbolGraphicLayer = new GraphicsOverlay(GraphicsOverlay.RenderingMode.STATIC);
    private GraphicsOverlay pinGraphicLayer = new GraphicsOverlay();

    private NTMapPermissionResult[] ntMapResults;
    private LocationDisplay locationDisplay;
    private BottomSheetBehavior bottomSheetBehavior;
    private double fromLon = 0;
    private double fromLat = 0;
    private double toLon = 0;
    private double toLat = 0;
    private int positionGo;

    private NTRouteResult ntRouteResult;

    private PictureMarkerSymbol fromMarkerSymbol;
    private PictureMarkerSymbol destMarkerSymbol;
    private SimpleMarkerSymbol directionSymbol;
    private SimpleLineSymbol lineSymbol;
    private Envelope lastExtent;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API KEY", this);
        // TODO: Setting Licence ID
        ArcGISRuntimeEnvironment.setLicense("Licence_ID");

        mapView = findViewById(R.id.mapView);
        mapView.setWrapAroundMode(WrapAroundMode.DISABLED);

        ListView lvVehicle = findViewById(R.id.lvVehicle);
        btnVehicle = findViewById(R.id.btnVehicle);
        txvMinutes = findViewById(R.id.txvMinutes);
        relativeLayout = findViewById(R.id.layoutMinute);
        edtFromLocation = findViewById(R.id.edtFromLocation);
        edtToLocation = findViewById(R.id.edtToLocation);
        ImageButton imbNavigation = findViewById(R.id.imbNavigation);
        ImageButton imbDirection = findViewById(R.id.imbDirection);
        ImageButton imbSearch = findViewById(R.id.imbSearch);
        ImageView imvCurrentLocation = findViewById(R.id.imbCurrentLocation);
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet));

        // Create symbols'
        try {
            BitmapDrawable fromFlagDrawable = (BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.flag, getTheme());
            fromMarkerSymbol = PictureMarkerSymbol.createAsync(fromFlagDrawable).get();

            BitmapDrawable destFlagDrawable = (BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.flag_des, getTheme());
            destMarkerSymbol = PictureMarkerSymbol.createAsync(destFlagDrawable).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        int lightGreen = ResourcesCompat.getColor(getResources(), R.color.colorLightGreen, getTheme());
        directionSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, lightGreen, 16F);

        int green = ResourcesCompat.getColor(getResources(), R.color.colorGreen, getTheme());
        lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, green, 8F);


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
                    // TODO: Insert referrer
                    String referrer = "REFERRER";

                    UserCredential credentials = UserCredential.createFromToken(token, referrer);
                    ArcGISTiledLayer layer = new ArcGISTiledLayer(url);
                    layer.setCredential(credentials);
                    Basemap basemap = new Basemap(layer);
                    ArcGISMap arcGISMap = new ArcGISMap(basemap);

                    // Set listener
                    arcGISMap.addLoadStatusChangedListener(RouteActivity.this);

                    mapView.setMap(arcGISMap);
                    mapView.setAttributionTextVisible(false);
                    mapView.getGraphicsOverlays().add(lineSymbolGraphicLayer);
                    mapView.getGraphicsOverlays().add(pinGraphicLayer);
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

        lvVehicle.setOnItemClickListener((parent, view, position, id) -> {
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
        });

        edtFromLocation.setOnClickListener(edtFromClick);
        edtToLocation.setOnClickListener(edtToClick);
        btnVehicle.setOnClickListener(btnVehicleClick);
        imbNavigation.setOnClickListener(btnNavigateClick);
        imbDirection.setOnClickListener(btnDirectionClick);
        imbSearch.setOnClickListener(btnSearchClick);
        imvCurrentLocation.setOnClickListener(imvCurrentLocationClick);

        mapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mapView) {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
                return true;
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
            lastExtent = mapView.getVisibleArea().getExtent();
        }
    };

    private View.OnClickListener edtToClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(RouteActivity.this, PinMarkerActivity.class);
            startActivityForResult(intent, REQ_TO_LOCATION);
            lastExtent = mapView.getVisibleArea().getExtent();
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
            mapView.setViewpointCenterAsync(mapView.getLocationDisplay().getLocation().getPosition());
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
                    lineSymbolGraphicLayer.getGraphics().clear();
                    pinGraphicLayer.getGraphics().clear();

                    if (ntRouteResult != null && ntRouteResult.getShape() != null) {
                        relativeLayout.setVisibility(View.VISIBLE);
                        txvMinutes.setText(String.format(Locale.ENGLISH, "%.2f นาที", ntRouteResult.getTotalTime()));
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                        try {
                            Geometry geometry = Geometry.fromJson(ntRouteResult.getShape());

                            if (geometry != null && !geometry.isEmpty()) {
                                geometry = GeometryEngine.project(geometry,
                                        SpatialReferences.getWgs84());
                                Graphic lineSymbolGraphic = new Graphic(geometry, lineSymbol);
                                lineSymbolGraphicLayer.getGraphics().add(lineSymbolGraphic);
                            }

                            Point fromPoint = new Point(toLon, toLat);
                            fromPoint = (Point) GeometryEngine.project(fromPoint,
                                    SpatialReferences.getWgs84());
                            Graphic graphicPinFromLocation = new Graphic(fromPoint, fromMarkerSymbol);

                            Point point = null;
                            if (ntRouteResult.getDirections() != null) {
                                for (NTDirection direction : ntRouteResult.getDirections()) {
                                    NTPoint facilityGeometry = direction.getPoint();
                                    if (facilityGeometry != null) {
                                        point = new Point(facilityGeometry.getX(), facilityGeometry.getY());
                                    }
                                    point = (Point) GeometryEngine.project(point,
                                            SpatialReferences.getWgs84());
                                    Graphic pointGraphic = new Graphic(point, directionSymbol);
                                    pinGraphicLayer.getGraphics().add(pointGraphic);
                                }
                            }
                            Graphic graphicPinToLocation = new Graphic(point, destMarkerSymbol);
                            pinGraphicLayer.getGraphics().add(graphicPinToLocation);
                            pinGraphicLayer.getGraphics().add(graphicPinFromLocation);

                            // Set extent
                            mapView.setViewpointGeometryAsync(geometry, 100);
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

    View.OnClickListener btnSearchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (ntRouteResult != null && ntRouteResult.getDirections() != null) {
                NTDirection[] directions = ntRouteResult.getDirections();
                int size = directions.length;

                double[] pointX = new double[size];
                double[] pointY = new double[size];
                NTPoint point;
                for (int i = 0; i < size; i++) {
                    point = directions[i].getPoint();
                    if (point != null) {
                        pointX[i] = point.getX();
                        pointY[i] = point.getY();
                    }
                }
                Intent intent = new Intent(RouteActivity.this, SearchAlongRouteActivity.class);
                intent.putExtra("pointX", pointX);
                intent.putExtra("pointY", pointY);
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
            Point pickPoint = new Point(pointX, pointY, SpatialReferences.getWebMercator());

            String decimalDegrees = CoordinateFormatter.toLatitudeLongitude(pickPoint, CoordinateFormatter.LatitudeLongitudeFormat.DECIMAL_DEGREES,
                    7);
            final Point wgsPoint = CoordinateFormatter.fromLatitudeLongitude(decimalDegrees, SpatialReferences.getWgs84());

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
    public void loadStatusChanged(LoadStatusChangedEvent loadStatusChangedEvent) {
        String mapLoadStatus = loadStatusChangedEvent.getNewLoadStatus().name();
        if ("LOADED".equals(mapLoadStatus)) {
            Envelope env = new Envelope(1.0672849926751213E7, 593515.9027621585,
                    1.1905414975501748E7, 2375599.5357473083, SpatialReference.create(102100));
            Viewpoint viewpoint = new Viewpoint(env);
            mapView.setViewpoint(viewpoint);
            locationDisplay = mapView.getLocationDisplay();
            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
            locationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
                boolean locationChanged = false;

                @Override
                public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
                    if (!locationChanged) {
                        locationChanged = true;
                        LinearUnit unit = new LinearUnit(LinearUnitId.MILES);
                        double zoomWidth = unit.toMeters(50);
                        mapView.setViewpointCenterAsync(locationDisplay.getLocation().getPosition(), zoomWidth);
                    }
                }
            });
            locationDisplay.startAsync();
        }

    }

    @Override
    protected void onPause() {
        mapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.resume();
        if (lastExtent != null) {
            Viewpoint viewpoint = new Viewpoint(lastExtent);
            mapView.setViewpointAsync(viewpoint);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.dispose();
    }
}