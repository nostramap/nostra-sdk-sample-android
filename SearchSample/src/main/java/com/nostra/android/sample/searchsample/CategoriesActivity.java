package com.nostra.android.sample.searchsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import th.co.gissoft.nostrasdk.Base.IServiceRequestListener;
import th.co.gissoft.nostrasdk.Base.NTCategoryService;
import th.co.gissoft.nostrasdk.Base.NTLocationSearchService;
import th.co.gissoft.nostrasdk.Parameter.NTLocationSearchParameter;
import th.co.gissoft.nostrasdk.Result.NTCategoryResult;
import th.co.gissoft.nostrasdk.Result.NTCategoryResultSet;
import th.co.gissoft.nostrasdk.Result.NTLocationSearchResult;
import th.co.gissoft.nostrasdk.Result.NTLocationSearchResultSet;

public class CategoriesActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private ListView lvLocation;

    private double lat;
    private double lon;
    private double getLat;
    private double getLon;
    private String nameL;
    private String adminLevel1L;
    private String adminLevel2L;
    private String adminLevel3L;
    private String adminLevel4L;
    private NTCategoryResult[] ntCategoryResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);
        lvLocation = (ListView)findViewById(R.id.lvLocation);
        lvLocation.setOnItemClickListener(this);

        lat = getIntent().getExtras().getDouble("lat");
        lon = getIntent().getExtras().getDouble("lon");

        // Call NTSearchService and put parameter to ListResultsActivity
        NTCategoryService.executeAsync(new IServiceRequestListener<NTCategoryResultSet>() {
            @Override
            public void onResponse(NTCategoryResultSet result, String responseCode) {
                ntCategoryResult = result.getResults();
                String[] arrLocation = new String[ntCategoryResult.length];
                for (int i = 0; i < ntCategoryResult.length; i++) {
                    arrLocation[i] = ntCategoryResult[i].getName_L();
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        CategoriesActivity.this, R.layout.row_categories, R.id.txvCategories, arrLocation);
                lvLocation.setAdapter(adapter);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String[] categories = new String[] { ntCategoryResult[position].getCategoryCode() };
        NTLocationSearchParameter param = new NTLocationSearchParameter("");
        param.setCategory(categories);
        param.setLat(lat);
        param.setLon(lon);
        param.setNumReturn(5);

        NTLocationSearchService.executeAsync(param, new IServiceRequestListener<NTLocationSearchResultSet>() {
            @Override
            public void onResponse(NTLocationSearchResultSet result, String s) {
                NTLocationSearchResult[] results = result.getResults();
                String[] arrCategories = new String[results.length];
                for (int i = 0; i < results.length; i++) {
                    nameL = results[i].getName_L();
                    adminLevel1L = results[i].getAdminLevel1_L();
                    adminLevel2L = results[i].getAdminLevel2_L();
                    adminLevel3L = results[i].getAdminLevel3_L();
                    adminLevel4L = results[i].getAdminLevel4_L();
                    getLat = results[i].getLat();
                    getLon = results[i].getLon();
                    arrCategories[i] = nameL + " "
                            + adminLevel4L + " "
                            + adminLevel3L + " "
                            + adminLevel2L + " "
                            + adminLevel1L;
                }
                Intent intent = new Intent(CategoriesActivity.this, ListResultsActivity.class);
                intent.putExtra("addressSearchResults", arrCategories);
                intent.putExtra("lon",getLon);
                intent.putExtra("lat",getLat);
                startActivity(intent);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(CategoriesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}