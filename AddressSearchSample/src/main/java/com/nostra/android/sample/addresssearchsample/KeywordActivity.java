package com.nostra.android.sample.addresssearchsample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import th.co.nostrasdk.NTSDKEnvironment;
import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.search.address.NTAddressSearchParameter;
import th.co.nostrasdk.search.address.NTAddressSearchResult;
import th.co.nostrasdk.search.address.NTAddressSearchResultSet;
import th.co.nostrasdk.search.address.NTAddressSearchService;

public class KeywordActivity extends Activity {
    private EditText edtKeyword;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyword);
        edtKeyword = (EditText) findViewById(R.id.edtKeyword);
        gson = new Gson();
        Button btnSearch = (Button) findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(btnSearchClick);
    }

    View.OnClickListener btnSearchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Determine parameter
            NTAddressSearchParameter param = new NTAddressSearchParameter(edtKeyword.getText().toString());
            param.setNumberOfResult(5);

            // Call service NTAddressSearchService with parameter
            NTAddressSearchService.executeAsync(param, new ServiceRequestListener<NTAddressSearchResultSet>() {
                @Override
                public void onResponse(NTAddressSearchResultSet resultSet) {

                    NTAddressSearchResult[] results = resultSet.getResults();
                    if (results.length > 0) {
                        String json =  gson.toJson(results);
                        Intent intent = new Intent(KeywordActivity.this, ListResultsActivity.class);
                        intent.putExtra("results", json);
                        startActivity(intent);
                    } else {
                        Toast.makeText(KeywordActivity.this, "No Results", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String errorMessage, int statusCode) {
                    Toast.makeText(KeywordActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
}
