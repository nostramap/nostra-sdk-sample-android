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

import th.co.gissoft.nostrasdk.Base.IServiceRequestListener;
import th.co.gissoft.nostrasdk.Base.NTFuelService;
import th.co.gissoft.nostrasdk.Parameter.NTFuelParameter;
import th.co.gissoft.nostrasdk.Result.NTFuelResult;
import th.co.gissoft.nostrasdk.Result.NTFuelResultSet;

public class ListResultsActivity extends AppCompatActivity {
    private double getX = 0;
    private double getY = 0;
    private String codeProvince;
    private String codeDistrict;

    private ImageButton imbBack;
    private ListView lvFuel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview_fuel);

        lvFuel = (ListView) findViewById(R.id.lvFuel);
        imbBack = (ImageButton) findViewById(R.id.imbBack);
        imbBack.setOnClickListener(new View.OnClickListener() {
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

        // Display fuel price
        displayFuelTable();
    }

    //Call NTFuelService and set adapter in listview
    private void displayFuelTable() {
        if (getX != 0 && getY != 0) {
            NTFuelParameter fuelParameter = new NTFuelParameter(getY, getX);
            NTFuelService.executeAsync(fuelParameter, new IServiceRequestListener<NTFuelResultSet>() {
                @Override
                public void onResponse(final NTFuelResultSet result, String responseCode) {
                    NTFuelResult[] results = result.getResults();
                    final String[] fuelResults = new String[results.length];
                    for (int i = 0; i < results.length; i++) {
                        fuelResults[i] = results[i].getBrandName_L();
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ListResultsActivity.this,
                            R.layout.row_fuel, R.id.txv_results, fuelResults);
                    lvFuel.setAdapter(adapter);
                    lvFuel.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            NTFuelResult[] results = result.getResults();
                            for (int i = 0; i < results.length; i++) {
                                if (position == i) {
                                    Intent intent = new Intent(getApplicationContext(), PriceFuelActivity.class);
                                    intent.putExtra("nameBrandL", results[i].getBrandName_L());
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

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            NTFuelParameter fuelParameter = new NTFuelParameter(codeProvince.trim(), codeDistrict.trim());
            NTFuelService.executeAsync(fuelParameter, new IServiceRequestListener<NTFuelResultSet>() {
                @Override
                public void onResponse(final NTFuelResultSet result, String responseCode) {
                    NTFuelResult[] results = result.getResults();
                    String[] fuelResults = new String[results.length];
                    for (int i = 0; i < results.length; i++) {
                        fuelResults[i] = results[i].getBrandName_L();
                        String brandNameL = results[i].getBrandName_L();
                        fuelResults[i] = brandNameL;
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ListResultsActivity.this,
                            R.layout.row_fuel, R.id.txv_results, fuelResults);
                    lvFuel.setAdapter(adapter);
                    lvFuel.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            NTFuelResult[] results = result.getResults();
                            for (int i = 0; i < results.length; i++) {
                                if (position == i) {
                                    Intent intent = new Intent(getApplicationContext(), PriceFuelActivity.class);
                                    intent.putExtra("nameBrandL", results[i].getBrandName_L());
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

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}