package com.nostra.android.sample.multimodalsample;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
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
import com.esri.core.geometry.CoordinateConversion;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import th.co.nostrasdk.Base.IServiceRequestListener;
import th.co.nostrasdk.Base.NTMapPermissionService;
import th.co.nostrasdk.Base.NTMultiModalTransportService;
import th.co.nostrasdk.Base.NTSDKEnvironment;
import th.co.nostrasdk.Parameter.Class.NTLocation;
import th.co.nostrasdk.Parameter.Class.NTMultiModalDirection;
import th.co.nostrasdk.Parameter.Constant.NTMultiModalTransportMode;
import th.co.nostrasdk.Parameter.NTMultiModalTransportParameter;
import th.co.nostrasdk.Result.NTMapPermissionResult;
import th.co.nostrasdk.Result.NTMapPermissionResultSet;
import th.co.nostrasdk.Result.NTMultiModalTransportResult;

public class MainActivity extends AppCompatActivity implements OnStatusChangedListener {
    private MapView mapView;
    private EditText edtToLocation;
    private EditText edtFromLocation;
    private RelativeLayout relativeLayout;
    private TextView txvMinMeter;
    private ImageView imvCurrent;
    private TextView txvTravelBy;

    private NTMapPermissionResult[] ntMapResults;
    private LocationDisplayManager locationManager;

    private GraphicsLayer lineGraphicLayer = new GraphicsLayer();
    private GraphicsLayer pinGraphicLayer = new GraphicsLayer();
    private GraphicsLayer SymbolCircleGraphicsLayer = new GraphicsLayer();
    private GraphicsLayer pinGraphicToLocation = new GraphicsLayer();
    private GraphicsLayer pinGraphicFromLocation = new GraphicsLayer();
    private MapGeometry mapGeometry;
    private Polyline polyline;

    private Point pointToLocation;
    private Point pointFromLocation;
    private Point mapPoint;
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

    private DecimalFormat df = new DecimalFormat("0.00");
    private final SpatialReference INPUT_SR = SpatialReference.create(SpatialReference.WKID_WGS84);
    private final SpatialReference OUTPUT_SR = SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multimodal);

        // Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

        edtToLocation = (EditText) findViewById(R.id.edtToLocation);
        edtFromLocation = (EditText) findViewById(R.id.edtFromLocation);
        ImageButton imbNavigation = (ImageButton) findViewById(R.id.imbNavigation);
        imbNavigation.setOnClickListener(navigation);
        txvTravelBy = (TextView) findViewById(R.id.txvTravelBy);
        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayoutMinute);
        txvMinMeter = (TextView) findViewById(R.id.txvMinMeter);
        ImageView imvResultsDirection = (ImageView) findViewById(R.id.imvResultsDirection);
        imvResultsDirection.setOnClickListener(direction);
        imvCurrent = (ImageView) findViewById(R.id.imvCurrent);
        mapView = (MapView) findViewById(R.id.mapView);

        initialMap();

        txvTravelBy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TravelByActivity.class);
                startActivityForResult(intent, travelCode);
            }
        });
    }

    //Add map and current location
    private void initialMap() {
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
                    mapView.addLayer(lineGraphicLayer);
                    mapView.addLayer(pinGraphicLayer);
                    mapView.addLayer(SymbolCircleGraphicsLayer);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        mapView.setOnStatusChangedListener(this);

        //Zoom to current location
        imvCurrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.centerAt(mapPoint, true);
            }
        });

        edtFromLocation.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    Intent intent = new Intent(MainActivity.this, PinMarkOnMapActivity.class);
                    intent.putExtra("Location", "fromLocation");
                    startActivityForResult(intent, fromLocation);
                }
                return true;
            }
        });

        edtToLocation.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    Intent intent = new Intent(MainActivity.this, PinMarkOnMapActivity.class);
                    intent.putExtra("Location", "toLocation");
                    startActivityForResult(intent, toLocation);
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

    //Get Attributes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == toLocation && resultCode == RESULT_OK) {
            toLocationCenterX = (float) data.getExtras().getDouble("CenterX");
            toLocationCenterY = (float) data.getExtras().getDouble("CenterY");
            edtToLocation.setText(" To_Location");
            pinMarkOnMap();
        } else if (requestCode == fromLocation && resultCode == RESULT_OK) {
            fromLocationCenterX = (float) data.getExtras().getDouble("CenterX");
            fromLocationCenterY = (float) data.getExtras().getDouble("CenterY");
            edtFromLocation.setText(" From_Location");
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
                pointToLocation = new Point(toLocationCenterX, toLocationCenterY);
                String decimalDegrees = CoordinateConversion.pointToDecimalDegrees(pointToLocation,
                        mapView.getSpatialReference(), 7);
                final Point pointToLocation = CoordinateConversion.decimalDegreesToPoint(decimalDegrees,
                        SpatialReference.create(SpatialReference.WKID_WGS84));

                pointFromLocation = new Point(fromLocationCenterX, fromLocationCenterY);
                String decimalDegrees2 = CoordinateConversion.pointToDecimalDegrees(pointFromLocation,
                        mapView.getSpatialReference(), 7);
                final Point pointFromLocation = CoordinateConversion.decimalDegreesToPoint(decimalDegrees2,
                        SpatialReference.create(SpatialReference.WKID_WGS84));

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
                        mode[i] = NTMultiModalTransportMode.AIR;
                    } else if (travel.equals(bus)) {
                        mode[i] = NTMultiModalTransportMode.BUS;
                    } else if (travel.equals(mrt)) {
                        mode[i] = NTMultiModalTransportMode.MRT;
                    } else if (travel.equals(bts)) {
                        mode[i] = NTMultiModalTransportMode.BTS;
                    } else if (travel.equals(brt)) {
                        mode[i] = NTMultiModalTransportMode.BRT;
                    } else if (travel.equals(airportRailLink)) {
                        mode[i] = NTMultiModalTransportMode.ARL;
                    } else if (travel.equals(rail)) {
                        mode[i] = NTMultiModalTransportMode.RAIL;
                    } else if (travel.equals(boat)) {
                        mode[i] = NTMultiModalTransportMode.BOAT;
                    } else if (travel.equals(bmta)) {
                        mode[i] = NTMultiModalTransportMode.BMTA;
                    }
                }

                NTMultiModalTransportParameter param = new NTMultiModalTransportParameter(stops, mode);
                NTMultiModalTransportService.executeAsync(param, new IServiceRequestListener<NTMultiModalTransportResult>() {
                    @Override
                    public void onResponse(NTMultiModalTransportResult result, String responseCode) {
                        try {
                            directions = result.getMinute().getDirections();
                            pinGraphicToLocation.removeAll();
                            pinGraphicFromLocation.removeAll();
                            pinGraphicLayer.removeAll();
                            lineGraphicLayer.removeAll();
                            SymbolCircleGraphicsLayer.removeAll();
                            for (int i = 0; i < directions.length; i++) {
                                String json = directions[i].getPathJson();
                                JsonParser parser = new JsonFactory().createJsonParser(json);
                                mapGeometry = GeometryEngine.jsonToGeometry(parser);
                                polyline = (Polyline) GeometryEngine.project(mapGeometry.getGeometry(), INPUT_SR, OUTPUT_SR);
                                lineGraphicLayer.addGraphic(new Graphic(polyline, new SimpleLineSymbol
                                        (getResources().getColor(R.color.colorGreen), 5)));

                                SimpleMarkerSymbol SymbolCircle = new SimpleMarkerSymbol
                                        (getResources().getColor(R.color.colorLightGreen), 10,
                                                SimpleMarkerSymbol.STYLE.CIRCLE);
                                List<double[]> PathCircle = directions[i].getPath();
                                double[] Circle = PathCircle.get(0);
                                Point point = GeometryEngine.project(Circle[0], Circle[1], OUTPUT_SR);
                                Graphic pointGraphic = new Graphic(point, SymbolCircle);
                                SymbolCircleGraphicsLayer.addGraphic(pointGraphic);
                            }
                            PictureMarkerSymbol pinFinish = new PictureMarkerSymbol(MainActivity.this,
                                    getResources().getDrawable(R.drawable.flag));
                            List<double[]> Path = directions[0].getPath();
                            double[] firstPoint = Path.get(0);
                            Point point = GeometryEngine.project(firstPoint[0], firstPoint[1], OUTPUT_SR);
                            Graphic graphicPintoLocation = new Graphic(point, pinFinish);

                            PictureMarkerSymbol pinStart = new PictureMarkerSymbol(MainActivity.this,
                                    getResources().getDrawable(R.drawable.flag_des));
                            List<double[]> Path2 = directions[directions.length - 1].getPath();
                            double[] lastPoint = Path2.get(Path2.size() - 1);
                            Point point2 = GeometryEngine.project(lastPoint[0], lastPoint[1], OUTPUT_SR);
                            Graphic graphicPinFromLocation = new Graphic(point2, pinStart);

                            pinGraphicLayer.addGraphics(new Graphic[]{graphicPintoLocation, graphicPinFromLocation});
                            relativeLayout.setVisibility(View.VISIBLE);
                            totalMeter = result.getMeter().getLength();
                            totalMinute = result.getMinute().getTime();
                            int meter = (int) totalMeter;
                            txvMinMeter.setText(String.valueOf(df.format(totalMinute) + "Min" + "(" +
                                    String.valueOf(meter) + "m." + ")"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
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
        pinGraphicToLocation.removeAll();
        if (toLocationCenterX != 0 && toLocationCenterY != 0) {
            pointToLocation = new Point(toLocationCenterX, toLocationCenterY);
            PictureMarkerSymbol pinMarkToLocation = new PictureMarkerSymbol(MainActivity.this,
                    getResources().getDrawable(R.drawable.flag));
            Graphic graphic = new Graphic(pointToLocation, pinMarkToLocation);
            pinGraphicToLocation.addGraphic(graphic);
            mapView.addLayer(pinGraphicToLocation);
        } else {
            Toast.makeText(MainActivity.this, "Select start.", Toast.LENGTH_SHORT).show();
        }

        pinGraphicFromLocation.removeAll();
        if (fromLocationCenterX != 0 && fromLocationCenterY != 0) {
            pointFromLocation = new Point(fromLocationCenterX, fromLocationCenterY);
            PictureMarkerSymbol pinMarkToLocation = new PictureMarkerSymbol(MainActivity.this,
                    getResources().getDrawable(R.drawable.flag_des));
            Graphic graphic = new Graphic(pointFromLocation, pinMarkToLocation);
            pinGraphicFromLocation.addGraphic(graphic);
            mapView.addLayer(pinGraphicFromLocation);
        } else {
            Toast.makeText(MainActivity.this, "Select destination.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStatusChanged(Object source, STATUS status) {
        if (source == mapView && status == OnStatusChangedListener.STATUS.INITIALIZED) {
            locationManager = mapView.getLocationDisplayManager();
            locationManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
            locationManager.setLocationListener(new LocationListener() {

                boolean locationChanged = false;

                // Zooms to the current location when first GPS fix arrives.
                @Override
                public void onLocationChanged(Location loc) {
                    if (!locationChanged) {
                        locationChanged = true;
                        double locY = loc.getLatitude();
                        double locX = loc.getLongitude();
                        Point wgsPoint = new Point(locX, locY);
                        mapPoint = (Point) GeometryEngine.project(wgsPoint,
                                        SpatialReference.create(4326),
                                        mapView.getSpatialReference());

                        Unit mapUnit = mapView.getSpatialReference().getUnit();
                        double zoomWidth = Unit.convertUnits(5,
                                Unit.create(LinearUnit.Code.MILE_US),
                                mapUnit);
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
            locationManager.start();
        }
    }
}