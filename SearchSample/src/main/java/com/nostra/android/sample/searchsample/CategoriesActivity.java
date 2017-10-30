package com.nostra.android.sample.searchsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.info.category.NTCategoryResult;
import th.co.nostrasdk.info.category.NTCategoryResultSet;
import th.co.nostrasdk.info.category.NTCategoryService;
import th.co.nostrasdk.common.NTPoint;
import th.co.nostrasdk.search.location.NTLocationSearchParameter;
import th.co.nostrasdk.search.location.NTLocationSearchResult;
import th.co.nostrasdk.search.location.NTLocationSearchResultSet;
import th.co.nostrasdk.search.location.NTLocationSearchService;

public class CategoriesActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private ListView lvLocation;

    private double lat;
    private double lon;
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
        String[] categories = new String[]{ntCategoryResult[position].getCategoryCode()};
        NTPoint point = new NTPoint(lon, lat);
        NTLocationSearchParameter param = new NTLocationSearchParameter("สาทร", categories);
        param.setPoint(point);
        param.setNumberOfResult(5);

        NTLocationSearchService.executeAsync(param, new ServiceRequestListener<NTLocationSearchResultSet>() {
            @Override
            public void onResponse(NTLocationSearchResultSet result) {
                NTLocationSearchResult[] results = result.getResults();
                ArrayList<SearchResult> resultList = new ArrayList<>();
                for (NTLocationSearchResult data : results) {
                    SearchResult searchResult = new SearchResult(data.getLocalName(),
                            data.getAdminLevel1().getLocalName(),
                            data.getAdminLevel2().getLocalName(),
                            data.getAdminLevel3().getLocalName(),
                            data.getAdminLevel3().getLocalName(),
                            data.getLocationPoint());
                    resultList.add(searchResult);
                }
                Intent intent = new Intent(CategoriesActivity.this, ListResultsActivityNew.class);
                intent.putParcelableArrayListExtra("results", resultList);
                startActivity(intent);
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(CategoriesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}