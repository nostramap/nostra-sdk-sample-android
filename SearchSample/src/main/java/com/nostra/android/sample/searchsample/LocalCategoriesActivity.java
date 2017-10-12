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
import th.co.nostrasdk.common.NTCountry;
import th.co.nostrasdk.info.category.NTLocalCategoryParameter;
import th.co.nostrasdk.info.category.NTLocalCategoryResult;
import th.co.nostrasdk.info.category.NTLocalCategoryResultSet;
import th.co.nostrasdk.info.category.NTLocalCategoryService;
import th.co.nostrasdk.network.NTPoint;
import th.co.nostrasdk.search.location.NTLocationSearchParameter;
import th.co.nostrasdk.search.location.NTLocationSearchResult;
import th.co.nostrasdk.search.location.NTLocationSearchResultSet;
import th.co.nostrasdk.search.location.NTLocationSearchService;

public class LocalCategoriesActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
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
    private NTLocalCategoryResult[] ntCategoryNameResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);
        lvLocation = (ListView) findViewById(R.id.lvLocation);
        lvLocation.setOnItemClickListener(this);

        lat = getIntent().getExtras().getDouble("lat");
        lon = getIntent().getExtras().getDouble("lon");

        // Call NTSearchService and put parameter to ListResultsActivity
        NTLocalCategoryParameter param = new NTLocalCategoryParameter();
        param.setCountry(NTCountry.THAILAND);
        NTLocalCategoryService.executeAsync(param, new ServiceRequestListener<NTLocalCategoryResultSet>() {
            @Override
            public void onResponse(NTLocalCategoryResultSet result) {
                ntCategoryNameResult = result.getResults();
                String[] arrLocation = new String[ntCategoryNameResult.length];
                for (int i = 0; i < ntCategoryNameResult.length; i++) {
                    arrLocation[i] = ntCategoryNameResult[i].getLocalName();
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        LocalCategoriesActivity.this, R.layout.row_categories, R.id.txvCategories, arrLocation);
                lvLocation.setAdapter(adapter);
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(LocalCategoriesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        String[] categories = new String[]{ntCategoryNameResult[position].getLocalCategoryCode()};
        String[] categories = new String[]{""};
        String[] localCategories = new String[]{""};
        NTPoint point = new NTPoint(lon,lat);
        // TODO: 10/12/2017 recheck again.
        NTLocationSearchParameter param = new NTLocationSearchParameter("สาทร",categories,localCategories,"","");
        param.setPoint(point);
        param.setNumberOfResult(5);

        NTLocationSearchService.executeAsync(param, new ServiceRequestListener<NTLocationSearchResultSet>() {
            @Override
            public void onResponse(NTLocationSearchResultSet result) {
                // TODO: 10/12/2017 recheck again. 
                NTLocationSearchResult[] results = result.getResults();
                String[] arrCategories = new String[results.length];
                for (int i = 0; i < results.length; i++) {
                    nameL = results[i].getLocalName();
                    adminLevel1L = results[i].getAdminLevel1().getLocalName();
                    adminLevel2L = results[i].getAdminLevel2().getLocalName();
                    adminLevel3L = results[i].getAdminLevel3().getLocalName();
                    adminLevel4L = results[i].getAdminLevel4().getLocalName();
                    getLat = results[i].getLocationPoint().getY();
                    getLon = results[i].getLocationPoint().getX();
                    arrCategories[i] = nameL + " "
                            + adminLevel4L + " "
                            + adminLevel3L + " "
                            + adminLevel2L + " "
                            + adminLevel1L;
                }
                Intent intent = new Intent(LocalCategoriesActivity.this, ListResultsActivity.class);
                intent.putExtra("addressSearchResults", arrCategories);
                intent.putExtra("lon", getLon);
                intent.putExtra("lat", getLat);
                startActivity(intent);
            }

            @Override
            public void onError(String errorMessage,int statusCode) {
                Toast.makeText(LocalCategoriesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}