package com.nostra.android.sample.searchsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.common.NTAdministrative;
import th.co.nostrasdk.info.category.NTCategoryResult;
import th.co.nostrasdk.info.category.NTCategoryResultSet;
import th.co.nostrasdk.info.category.NTCategoryService;
import th.co.nostrasdk.network.NTPoint;
import th.co.nostrasdk.search.location.NTLocationSearchParameter;
import th.co.nostrasdk.search.location.NTLocationSearchResult;
import th.co.nostrasdk.search.location.NTLocationSearchResultSet;
import th.co.nostrasdk.search.location.NTLocationSearchService;

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
        lvLocation = (ListView) findViewById(R.id.lvLocation);
        lvLocation.setOnItemClickListener(this);

        lat = getIntent().getExtras().getDouble("lat");
        lon = getIntent().getExtras().getDouble("lon");

        // Call NTSearchService and put parameter to ListResultsActivity
        NTCategoryService.executeAsync(new ServiceRequestListener<NTCategoryResultSet>() {
            @Override
            public void onResponse(NTCategoryResultSet result) {
                ntCategoryResult = result.getResults();
                String[] arrLocation = new String[ntCategoryResult.length];
                for (int i = 0; i < ntCategoryResult.length; i++) {
                    arrLocation[i] = ntCategoryResult[i].getLocalName();
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        CategoriesActivity.this, R.layout.row_categories, R.id.txvCategories, arrLocation);
                lvLocation.setAdapter(adapter);
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(CategoriesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        String[] categories = new String[] { ntCategoryResult[position].getCategoryCode() };
        String[] categories = new String[]{""};
        String[] LocalCategories = new String[]{""};
        NTPoint point = new NTPoint(lon, lat);
        // TODO: 10/12/2017 recheck again.
        NTLocationSearchParameter param = new NTLocationSearchParameter("สาทร", categories, LocalCategories, "", "");
        param.setPoint(point);
        param.setNumberOfResult(5);

        NTLocationSearchService.executeAsync(param, new ServiceRequestListener<NTLocationSearchResultSet>() {
            @Override
            public void onResponse(NTLocationSearchResultSet result) {
                NTLocationSearchResult[] results = result.getResults();
                String[] arrCategories = new String[results.length];
                // TODO: 10/12/2017 recheck again.
                for (int i = 0; i < results.length; i++) {
                    nameL = results[i].getLocalName();
                    NTAdministrative admin = results[i].getAdminLevel1();
                    adminLevel1L = admin.getLocalName();
                    admin = results[i].getAdminLevel2();
                    adminLevel2L = admin.getLocalName();
                    admin = results[i].getAdminLevel3();
                    adminLevel3L = admin.getLocalName();
                    admin = results[i].getAdminLevel4();
                    adminLevel4L = admin.getLocalName();
                    NTPoint latLon = results[i].getLocationPoint();
                    getLat = latLon.getY();
                    getLon = latLon.getX();
                    arrCategories[i] = nameL + " "
                            + adminLevel4L + " "
                            + adminLevel3L + " "
                            + adminLevel2L + " "
                            + adminLevel1L;
                }
                Intent intent = new Intent(CategoriesActivity.this, ListResultsActivity.class);
                intent.putExtra("addressSearchResults", arrCategories);
                intent.putExtra("lon", getLon);
                intent.putExtra("lat", getLat);
                startActivity(intent);
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(CategoriesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}