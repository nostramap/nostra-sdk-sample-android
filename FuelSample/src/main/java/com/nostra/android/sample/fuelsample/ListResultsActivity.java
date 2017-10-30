package com.nostra.android.sample.fuelsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.common.NTPoint;
import th.co.nostrasdk.info.fuel.NTFuelParameter;
import th.co.nostrasdk.info.fuel.NTFuelResult;
import th.co.nostrasdk.info.fuel.NTFuelResultSet;
import th.co.nostrasdk.info.fuel.NTFuelService;

public class ListResultsActivity extends AppCompatActivity {
    private double getX = 0;
    private double getY = 0;
    private String codeProvince;
    private String codeDistrict;
    private NTFuelResult[] results;

    private ListView lvFuel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview_fuel);

        lvFuel = (ListView) findViewById(R.id.lvFuel);
        ImageButton btnBack = (ImageButton) findViewById(R.id.imbBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Gather x, y from previous activity.
        Bundle data = getIntent().getExtras();
        getX = data.getDouble("x", 0D);
        getY = data.getDouble("y", 0D);
        codeDistrict = data.getString("codeDistrict", "");
        codeProvince = data.getString("codeProvince", "");

        if (getX != 0 && getY != 0) {
            displayFuelByCoordinate();
        } else {
            displayFuelByAdminCode();
        }
    }

    private void displayFuelByCoordinate() {
        NTPoint point = new NTPoint(getX,getY);
        NTFuelParameter fuelParameter = new NTFuelParameter(point);
        NTFuelService.executeAsync(fuelParameter, new ServiceRequestListener<NTFuelResultSet>() {
            @Override
            public void onResponse(final NTFuelResultSet result) {
                results = result.getResults();
                final String[] fuelResults = new String[results.length];
                for (int i = 0; i < results.length; i++) {
                    fuelResults[i] = results[i].getLocalBrandName();
                }
                bindFuelResult(fuelResults);
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(ListResultsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayFuelByAdminCode() {
        NTFuelParameter fuelParameter = new NTFuelParameter(codeProvince.trim(), codeDistrict.trim());
        NTFuelService.executeAsync(fuelParameter, new ServiceRequestListener<NTFuelResultSet>() {
            @Override
            public void onResponse(final NTFuelResultSet result) {
                results = result.getResults();
                final String[] fuelResults = new String[results.length];
                for (int i = 0; i < results.length; i++) {
                    fuelResults[i] = results[i].getLocalBrandName();
                }
                bindFuelResult(fuelResults);
            }

            @Override
            public void onError(String errorMessage,int statusCode) {
                Toast.makeText(ListResultsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindFuelResult(String[] fuelResults) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ListResultsActivity.this,
                R.layout.row_fuel, R.id.txv_results, fuelResults);
        lvFuel.setAdapter(adapter);
        lvFuel.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                for (int i = 0; i < results.length; i++) {
                    if (position == i) {
                        Intent intent = new Intent(ListResultsActivity.this, PriceFuelActivity.class);
                        intent.putExtra("nameBrandL", results[i].getLocalBrandName());
                        intent.putExtra("diesel", results[i].getDiesel());
                        intent.putExtra("dieselPremium", results[i].getDieselPremium());
                        intent.putExtra("gasohol91", results[i].getGasohol91());
                        intent.putExtra("gasohol95", results[i].getGasohol95());
                        intent.putExtra("gasoholE20", results[i].getGasoholE20());
                        intent.putExtra("gasoholE85", results[i].getGasoholE85());
                        intent.putExtra("gasoline91", results[i].getGasoline91());
                        intent.putExtra("gasoline95", results[i].getGasoline95());
                        intent.putExtra("ngv", results[i].getNgv());
                        startActivity(intent);
                    }
                }
            }
        });
    }
}