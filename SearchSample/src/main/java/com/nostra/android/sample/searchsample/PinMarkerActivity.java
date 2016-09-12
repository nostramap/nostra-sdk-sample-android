package com.nostra.android.sample.searchsample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.CoordinateConversion;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;

import th.co.nostrasdk.Base.IServiceRequestListener;
import th.co.nostrasdk.Base.NTMapPermissionService;
import th.co.nostrasdk.Base.NTSDKEnvironment;
import th.co.nostrasdk.Result.NTMapPermissionResult;
import th.co.nostrasdk.Result.NTMapPermissionResultSet;

public class PinMarkerActivity extends AppCompatActivity {
    private MapView mapView;
    private Callout mapCallout;
    private NTMapPermissionResult[] ntMapResults;
    private double lat;
    private double lon;
    private Point point;
    private GraphicsLayer graphicsLayerPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_marker);

        // Setting SDK Environment (API KEY)
        NTSDKEnvironment.setEnvironment("API_KEY", this);
        // Setting Client ID
        ArcGISRuntime.setClientId("CLIENT_ID");

        mapView = (MapView) findViewById(R.id.mapView);
        graphicsLayerPin = new GraphicsLayer();
        initializeMap();

        final ImageView imvCurrentLocation = (ImageView) findViewById(R.id.imvCurrentLocation);
        imvCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.centerAt(point, true);
            }
        });
    }

    private void initializeMap() {
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

                    lat = getIntent().getExtras().getDouble("lat");
                    lon = getIntent().getExtras().getDouble("lon");
                    point = new Point(lon, lat);

                    String decimalDegrees = CoordinateConversion.pointToDecimalDegrees(point,
                            SpatialReference.create(SpatialReference.WKID_WGS84), 7);
                    point = CoordinateConversion.decimalDegreesToPoint(decimalDegrees,
                            SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE));
                    PictureMarkerSymbol markerSymbol = new PictureMarkerSymbol(PinMarkerActivity.this,
                            getResources().getDrawable(R.drawable.pin_markonmap));
                    Graphic graphicPin = new Graphic(point, markerSymbol);
                    graphicsLayerPin.addGraphic(graphicPin);
                    mapView.addLayer(graphicsLayerPin);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(PinMarkerActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        mapView.setOnStatusChangedListener(new OnStatusChangedListener() {
            @Override
            public void onStatusChanged(Object o, STATUS status) {
                if (status == STATUS.LAYER_LOADED) {
                    String results = getIntent().getExtras().getString("listResults");
                    mapCallout = mapView.getCallout();
                    mapCallout.setContent(loadView(results));
                    mapCallout.setOffsetDp(0, 25);
                    mapCallout.show(point);
                    mapView.centerAt(point, true);
                    mapView.zoomToScale(point, 10000);
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

    //Set content in callout
    private View loadView(String Name_L) {
        View view = LayoutInflater.from(PinMarkerActivity.this).inflate(R.layout.callout, null);

        final TextView txvNameL = (TextView) view.findViewById(R.id.txvNameL);
        txvNameL.setText(Name_L);

        final ImageView imvPin = (ImageView) view.findViewById(R.id.imvPin);
        imvPin.setImageDrawable(PinMarkerActivity.this.getResources().getDrawable(R.drawable.pin_markonmap));

        return view;
    }
}