package com.nostra.android.sample.multimodalsample;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.loadable.LoadStatusChangedEvent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedListener;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import th.co.nostrasdk.NTSDKEnvironment;
import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;
import th.co.nostrasdk.network.NTLocation;
import th.co.nostrasdk.network.transport.NTMultiModalDirection;
import th.co.nostrasdk.network.transport.NTMultiModalRoute;
import th.co.nostrasdk.network.transport.NTMultiModalTransportParameter;
import th.co.nostrasdk.network.transport.NTMultiModalTransportResult;
import th.co.nostrasdk.network.transport.NTMultiModalTransportService;
import th.co.nostrasdk.network.transport.NTMultiModalTransportationMode;

public class MainActivity extends AppCompatActivity implements LoadStatusChangedListener {
    private MapView mapView;
    private EditText edtToLocation;
    private EditText edtFromLocation;
    private RelativeLayout relativeLayout;
    private TextView txvMinMeter;
    private ImageView imvCurrent;

    private NTMapPermissionResult[] ntMapResults;
    private LocationDisplay locationDisplay;

    private GraphicsOverlay lineGraphicLayer = new GraphicsOverlay(GraphicsOverlay.RenderingMode.STATIC);
    private GraphicsOverlay pinGraphicLayer = new GraphicsOverlay();
    private GraphicsOverlay SymbolCircleGraphicsLayer = new GraphicsOverlay(GraphicsOverlay.RenderingMode.STATIC);
    private GraphicsOverlay pinGraphicToLocation = new GraphicsOverlay();
    private GraphicsOverlay pinGraphicFromLocation = new GraphicsOverlay();
    private Polyline polyline;

    private Point pointToLocation;
    private Point pointFromLocation;
    private int fromLocation = 1;
    private int toLocation = 0;
    private int travelCode = 2;
    private float toLocationCenterX = 0;
    private float toLocationCenterY = 0;
    private float fromLocationCenterX = 0;
    private float fromLocationCenterY = 0;
    private ArrayList<String> resultTravel;
    private NTMultiModalDirection[] directions;
    private String[] directionResults;
    private String[] arrDistanceAndTime;
    private String[] type;
    private double totalMinute;
    private double totalMeter;
    private Envelope lastExtent;

    private DecimalFormat df = new DecimalFormat("0.00");
    private final SpatialReference INPUT_SR = SpatialReferences.getWgs84();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multimodal);

        // TODO: Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // TODO: Setting Licence ID
        ArcGISRuntimeEnvironment.setLicense("Licence_ID");

        edtToLocation = findViewById(R.id.edtToLocation);
        edtFromLocation = findViewById(R.id.edtFromLocation);
        ImageButton imbNavigation = findViewById(R.id.imbNavigation);
        imbNavigation.setOnClickListener(navigation);
        TextView txvTravelBy = findViewById(R.id.txvTravelBy);
        relativeLayout = findViewById(R.id.relativeLayoutMinute);
        txvMinMeter = findViewById(R.id.txvMinMeter);
        ImageView imvResultsDirection = findViewById(R.id.imvResultsDirection);
        imvResultsDirection.setOnClickListener(direction);
        imvCurrent = findViewById(R.id.imvCurrent);
        mapView = findViewById(R.id.mapView);

        initialMap();

        txvTravelBy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TravelByActivity.class);
                startActivityForResult(intent, travelCode);
                lastExtent = mapView.getVisibleArea().getExtent();
            }
        });
    }

    //Add map and current location
    @SuppressLint("ClickableViewAccessibility")
    private void initialMap() {
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
                    arcGISMap.addLoadStatusChangedListener(MainActivity.this);

                    mapView.setMap(arcGISMap);
                    mapView.setAttributionTextVisible(false);
                    mapView.getGraphicsOverlays().add(lineGraphicLayer);
                    mapView.getGraphicsOverlays().add(pinGraphicLayer);
                    mapView.getGraphicsOverlays().add(SymbolCircleGraphicsLayer);
                    mapView.getGraphicsOverlays().add(pinGraphicFromLocation);
                    mapView.getGraphicsOverlays().add(pinGraphicToLocation);

                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        //Zoom to current location
        imvCurrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.setViewpointCenterAsync(mapView.getLocationDisplay().getLocation().getPosition());
            }
        });

        edtFromLocation.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                Intent intent = new Intent(MainActivity.this, PinMarkOnMapActivity.class);
                intent.putExtra("Location", "fromLocation");
                startActivityForResult(intent, fromLocation);
                lastExtent = mapView.getVisibleArea().getExtent();
            }
            return true;
        });

        edtToLocation.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                Intent intent = new Intent(MainActivity.this, PinMarkOnMapActivity.class);
                intent.putExtra("Location", "toLocation");
                startActivityForResult(intent, toLocation);
                lastExtent = mapView.getVisibleArea().getExtent();
            }
            return true;
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

    //Get Attributes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == toLocation && resultCode == RESULT_OK) {
            toLocationCenterX = (float) data.getExtras().getDouble("CenterX");
            toLocationCenterY = (float) data.getExtras().getDouble("CenterY");
            edtToLocation.setText(R.string.to_location);
            pinMarkOnMap();
        } else if (requestCode == fromLocation && resultCode == RESULT_OK) {
            fromLocationCenterX = (float) data.getExtras().getDouble("CenterX");
            fromLocationCenterY = (float) data.getExtras().getDouble("CenterY");
            edtFromLocation.setText(R.string.from_location);
            pinMarkOnMap();
        } else if (requestCode == travelCode && resultCode == RESULT_OK) {
            resultTravel = data.getExtras().getStringArrayList("resultTravel");
        }
    }

    //Calculate Navigation
    private View.OnClickListener navigation = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (toLocationCenterX == 0 && toLocationCenterY == 0 && fromLocationCenterY == 0 && fromLocationCenterX == 0) {
                Toast.makeText(getApplication(), "Please select Location", Toast.LENGTH_SHORT).show();

            } else if (toLocationCenterX != 0 && toLocationCenterY != 0 && fromLocationCenterY == 0 && fromLocationCenterX == 0) {
                Toast.makeText(MainActivity.this, "Select fromLocation", Toast.LENGTH_SHORT).show();

            } else if (toLocationCenterX == 0 && toLocationCenterY == 0 && fromLocationCenterY != 0 && fromLocationCenterX != 0) {
                Toast.makeText(MainActivity.this, "Select toLocation", Toast.LENGTH_SHORT).show();

            } else if (resultTravel == null) {
                Toast.makeText(MainActivity.this, "Select Travel", Toast.LENGTH_SHORT).show();
            } else if (toLocationCenterX != 0 && toLocationCenterY != 0 & fromLocationCenterX != 0 &&
                    fromLocationCenterY != 0 && resultTravel.size() != 0) {
                pointToLocation = new Point(toLocationCenterX, toLocationCenterY, SpatialReferences.getWebMercator());
                String decimalDegrees = CoordinateFormatter.toLatitudeLongitude(pointToLocation, CoordinateFormatter.LatitudeLongitudeFormat.DECIMAL_DEGREES,
                        7);
                final Point pointToLocation = CoordinateFormatter.fromLatitudeLongitude(decimalDegrees, SpatialReferences.getWgs84());

                pointFromLocation = new Point(fromLocationCenterX, fromLocationCenterY, SpatialReferences.getWebMercator());
                String decimalDegrees2 = CoordinateFormatter.toLatitudeLongitude(pointFromLocation, CoordinateFormatter.LatitudeLongitudeFormat.DECIMAL_DEGREES,
                        7);
                final Point pointFromLocation = CoordinateFormatter.fromLatitudeLongitude(decimalDegrees2, SpatialReferences.getWgs84());

                NTLocation[] stops = new NTLocation[]{
                        new NTLocation("toLocation", pointToLocation.getY(), pointToLocation.getX()),
                        new NTLocation("fromLocation", pointFromLocation.getY(), pointFromLocation.getX())
                };
                String[] mode;
                String airplane = "AIRPLANE";
                String bus = "BUS";
                String mrt = "MRT";
                String bts = "BTS";
                String brt = "BRT";
                String airportRailLink = "AIRPORT RAIL LINK";
                String rail = "RAIL";
                String boat = "BOAT";
                String bmta = "BMTA";
                mode = new String[resultTravel.size()];
                for (int i = 0; i < mode.length; i++) {
                    String[] ResultMode = new String[resultTravel.size()];
                    ResultMode[i] = resultTravel.get(i);
                    String travel = ResultMode[i];
                    if (travel.equals(airplane)) {
                        mode[i] = NTMultiModalTransportationMode.AIR;
                    } else if (travel.equals(bus)) {
                        mode[i] = NTMultiModalTransportationMode.BUS;
                    } else if (travel.equals(mrt)) {
                        mode[i] = NTMultiModalTransportationMode.MRT;
                    } else if (travel.equals(bts)) {
                        mode[i] = NTMultiModalTransportationMode.BTS;
                    } else if (travel.equals(brt)) {
                        mode[i] = NTMultiModalTransportationMode.BRT;
                    } else if (travel.equals(airportRailLink)) {
                        mode[i] = NTMultiModalTransportationMode.ARL;
                    } else if (travel.equals(rail)) {
                        mode[i] = NTMultiModalTransportationMode.RAIL;
                    } else if (travel.equals(boat)) {
                        mode[i] = NTMultiModalTransportationMode.BOAT;
                    } else if (travel.equals(bmta)) {
                        mode[i] = NTMultiModalTransportationMode.BMTA;
                    }
                }

                NTMultiModalTransportParameter param = new NTMultiModalTransportParameter(stops, mode);
                NTMultiModalTransportService.executeAsync(param, new ServiceRequestListener<NTMultiModalTransportResult>() {
                    @Override
                    public void onResponse(NTMultiModalTransportResult result) {
                        try {
                            NTMultiModalRoute minute = result.getMinute();
                            if (minute != null) {
                                directions = minute.getDirections();
                            }
                            pinGraphicToLocation.getGraphics().clear();
                            pinGraphicFromLocation.getGraphics().clear();
                            pinGraphicLayer.getGraphics().clear();
                            lineGraphicLayer.getGraphics().clear();
                            SymbolCircleGraphicsLayer.getGraphics().clear();
                            for (int i = 0; i < directions.length; i++) {
                                String json = directions[i].getPathJson();
                                Geometry geometry = Geometry.fromJson(json);
                                polyline = (Polyline) GeometryEngine.project(geometry, INPUT_SR);
                                lineGraphicLayer.getGraphics().add(new Graphic(polyline, new SimpleLineSymbol
                                        (SimpleLineSymbol.Style.SOLID, ContextCompat.getColor(getApplicationContext(), R.color.colorGreen), 5)));
                                SimpleMarkerSymbol SymbolCircle = new SimpleMarkerSymbol
                                        (SimpleMarkerSymbol.Style.CIRCLE, ContextCompat.getColor(getApplicationContext(), R.color.colorLightGreen), 10);
                                List<double[]> PathCircle = directions[i].getPath();
                                if (PathCircle != null) {
                                    double[] Circle = PathCircle.get(0);
                                    Geometry point = GeometryEngine.project(new Point(Circle[0], Circle[1]), INPUT_SR);
                                    Graphic pointGraphic = new Graphic(point, SymbolCircle);
                                    SymbolCircleGraphicsLayer.getGraphics().add(pointGraphic);
                                }

                            }
                            BitmapDrawable fromFlagDrawable = (BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.flag, getTheme());
                            PictureMarkerSymbol pinFinish = PictureMarkerSymbol.createAsync(fromFlagDrawable).get();

                            List<double[]> Path = directions[0].getPath();
                            List<double[]> Path2 = directions[directions.length - 1].getPath();
                            if (Path != null && Path2 != null) {
                                double[] firstPoint = Path.get(0);
                                Geometry point = GeometryEngine.project(new Point(firstPoint[0], firstPoint[1]), INPUT_SR);
                                Graphic graphicPintoLocation = new Graphic(point, pinFinish);

                                BitmapDrawable destFlagDrawable = (BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.flag_des, getTheme());
                                PictureMarkerSymbol pinDest = PictureMarkerSymbol.createAsync(destFlagDrawable).get();
                                double[] lastPoint = Path2.get(Path2.size() - 1);
                                Geometry point2 = GeometryEngine.project(new Point(lastPoint[0], lastPoint[1]), INPUT_SR);
                                Graphic graphicPinFromLocation = new Graphic(point2, pinDest);

                                pinGraphicLayer.getGraphics().add(graphicPintoLocation);
                                pinGraphicLayer.getGraphics().add(graphicPinFromLocation);
                            }
                            relativeLayout.setVisibility(View.VISIBLE);
                            NTMultiModalRoute meterResult = result.getMeter();
                            if (meterResult!=null){
                                totalMeter = meterResult.getLength();
                            }
                            totalMinute = result.getMinute().getTime();
                            int meter = (int) totalMeter;
                            txvMinMeter.setText(String.valueOf(df.format(totalMinute) + "Min" + "(" +
                                    String.valueOf(meter) + "m." + ")"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(String errorMessage, int statusCode) {
                        Toast.makeText(getApplication(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };

    private View.OnClickListener direction = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (directions == null) {
                Toast.makeText(MainActivity.this, "No direction", Toast.LENGTH_SHORT).show();
            } else {
                directionResults = new String[directions.length];
                arrDistanceAndTime = new String[directions.length];
                type = new String[directions.length];
                int lengthType = 0;
                int lengthDirection = 0;
                int lengthDistanceAndTime = 0;
                int intTime;
                String endText;
                String startText;
                double distance;
                double time;
                for (int i = directions.length - 1; i >= 0; i--) {
                    startText = directions[i].getStartText();
                    endText = directions[i].getEndText();
                    distance = directions[i].getLength();
                    time = directions[i].getTime();
                    intTime = (int) time;
                    if (lengthDirection < directions.length && lengthType < directions.length &&
                            lengthDistanceAndTime < directions.length) {
                        directionResults[lengthDirection++] = "Start :" + " " + startText + "\n" + "END :" + " " + endText;
                        type[lengthType++] = directions[i].getType();
                        arrDistanceAndTime[lengthDistanceAndTime++] = "Distance" + " " +
                                String.valueOf(df.format(distance)) + " " + "m." + "\n" + "Time" + " " +
                                String.valueOf(intTime) + " " + "min.";
                    }
                }
            }
            Intent intent = new Intent(MainActivity.this, ResultsDirectionActivity.class);
            intent.putExtra("directions", directionResults);
            intent.putExtra("type", type);
            intent.putExtra("distance_time", arrDistanceAndTime);
            startActivity(intent);
        }
    };

    //Add pin on initialMap
    private void pinMarkOnMap() {
        pinGraphicToLocation.getGraphics().clear();
        if (toLocationCenterX != 0 && toLocationCenterY != 0) {
            pointToLocation = new Point(toLocationCenterX, toLocationCenterY);
            BitmapDrawable fromFlagDrawable = (BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.flag, getTheme());
            try {
                PictureMarkerSymbol pinMarkToLocation = PictureMarkerSymbol.createAsync(fromFlagDrawable).get();
                Graphic graphic = new Graphic(pointToLocation, pinMarkToLocation);
                pinGraphicToLocation.getGraphics().add(graphic);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "Select start.", Toast.LENGTH_SHORT).show();
        }

        pinGraphicFromLocation.getGraphics().clear();
        if (fromLocationCenterX != 0 && fromLocationCenterY != 0) {
            pointFromLocation = new Point(fromLocationCenterX, fromLocationCenterY);

            BitmapDrawable destFlagDrawable = (BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.flag_des, getTheme());
            try {
                PictureMarkerSymbol pinMarkToLocation = PictureMarkerSymbol.createAsync(destFlagDrawable).get();
                Graphic graphic = new Graphic(pointFromLocation, pinMarkToLocation);
                pinGraphicFromLocation.getGraphics().add(graphic);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "Select destination.", Toast.LENGTH_SHORT).show();
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
        if(lastExtent != null) {
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