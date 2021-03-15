package com.nostra.android.sample.searchsample;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.map.NTMapPermissionResult;
import th.co.nostrasdk.map.NTMapPermissionResultSet;
import th.co.nostrasdk.map.NTMapPermissionService;
import th.co.nostrasdk.map.NTMapServiceInfo;

public class PinMarkerActivity extends AppCompatActivity {
    private MapView mapView;
    private Callout mapCallout;
    private NTMapPermissionResult[] ntMapResults;
    private double lat;
    private double lon;
    private Point point;
    private GraphicsOverlay graphicOverlaysPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_marker);

        mapView = (MapView) findViewById(R.id.mapView);
        graphicOverlaysPin = new GraphicsOverlay();
        initializeMap();

        final ImageView imvCurrentLocation = (ImageView) findViewById(R.id.imvCurrentLocation);
        imvCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.setViewpointCenterAsync(point);
            }
        });
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
    }

    private void initializeMap() {
        NTMapPermissionService.executeAsync(new ServiceRequestListener<NTMapPermissionResultSet>() {
            @Override
            public void onResponse(NTMapPermissionResultSet result) {
                ntMapResults = result.getResults();
                NTMapPermissionResult map = getThailandBasemap();
                if (map != null) {
                    NTMapServiceInfo info = map.getLocalService();
                    String url = info.getServiceUrl();
                    String token = info.getServiceToken();
                    String referrer = "REFERRER";    // TODO: Insert referrer

                    UserCredential credentials = UserCredential.createFromToken(token, referrer);

                    ArcGISTiledLayer layer = new ArcGISTiledLayer(url);
                    layer.setCredential(credentials);
                    Basemap  baseMap = new Basemap(layer);
                    ArcGISMap mMap = new ArcGISMap(baseMap);
                    mapView.setMap(mMap);
                    mMap.addDoneLoadingListener(onDoneLoadingListener);
                    lat = getIntent().getExtras().getDouble("lat");
                    lon = getIntent().getExtras().getDouble("lon");
                    point = new Point(lon, lat, SpatialReferences.getWgs84());

                    Drawable drawable = (BitmapDrawable) ContextCompat.getDrawable(getApplicationContext(), R.drawable.pin_markonmap);
                    ListenableFuture<PictureMarkerSymbol> symbol =
                            PictureMarkerSymbol.createAsync((BitmapDrawable) drawable);


                    try {
                        Graphic graphicPin = new Graphic(point, symbol.get());
                        graphicOverlaysPin.getGraphics().add(graphicPin);
                        mapView.getGraphicsOverlays().add(graphicOverlaysPin);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(PinMarkerActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private Runnable onDoneLoadingListener = new Runnable() {
        @Override
        public void run() {
            SearchResult results = getIntent().getParcelableExtra("result");
            mapCallout = mapView.getCallout();
            mapCallout.setContent(loadView(results.getLocalName(), lat, lon));
            mapCallout.setLocation(point);
            mapView.setViewpointCenterAsync(point, 10000);
        }
    };


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
    private View loadView(String Name_L, double lat, double lon) {
        View view = LayoutInflater.from(PinMarkerActivity.this).inflate(R.layout.callout, null);

        final TextView txvNameL = (TextView) view.findViewById(R.id.txvNameL);
        txvNameL.setText(Name_L);

        final TextView txvLocation = (TextView) view.findViewById(R.id.txvLocation);
        txvLocation.setText(String.format(Locale.ENGLISH, "%.6f", lat) +
                "  " + String.format(Locale.ENGLISH, "%.6f", lon));

        final ImageView imvPin = (ImageView) view.findViewById(R.id.imvPin);
        imvPin.setImageDrawable(PinMarkerActivity.this.getResources().getDrawable(R.drawable.pin_markonmap));

        return view;
    }
}