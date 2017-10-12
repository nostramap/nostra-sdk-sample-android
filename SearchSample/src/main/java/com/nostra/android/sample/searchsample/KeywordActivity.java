package com.nostra.android.sample.searchsample;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.common.NTCountry;
import th.co.nostrasdk.network.NTPoint;
import th.co.nostrasdk.search.autocomplete.NTAutoCompleteSearchParameter;
import th.co.nostrasdk.search.autocomplete.NTAutoCompleteSearchResultSet;
import th.co.nostrasdk.search.autocomplete.NTAutoCompleteSearchService;
import th.co.nostrasdk.search.location.NTLocationSearchParameter;
import th.co.nostrasdk.search.location.NTLocationSearchResult;
import th.co.nostrasdk.search.location.NTLocationSearchResultSet;
import th.co.nostrasdk.search.location.NTLocationSearchService;

public class KeywordActivity extends AppCompatActivity {
    private ListView lvAutoSearch;
    private EditText edtKeyword;
    private Button btnSearch;
    private ArrayAdapter<String> adapter;
    // TODO: 10/12/2017 recheck again.
    private String[] results;
    private String[] arrAutoSearch;
    private String nameLocation;
    private double lat;
    private double lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyword);
        lvAutoSearch = (ListView) findViewById(R.id.lvAutoSearch);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        edtKeyword = (EditText) findViewById(R.id.edtKeyWord);

        lat = getIntent().getExtras().getDouble("lat");
        lon = getIntent().getExtras().getDouble("lon");

        edtKeyword.addTextChangedListener(keywordWatcher);
        lvAutoSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                nameLocation = arrAutoSearch[position];
                edtKeyword.setText(nameLocation);
            }
        });

        // Call NTSearchService and put parameter to ListResultsActivity
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String search = edtKeyword.getText().toString();
                String[] categories = new String[] {};
                String[] localCategories = new String[] {};
                NTPoint point = new NTPoint(lon,lat);
                // TODO: 10/12/2017 recheck again.
                NTLocationSearchParameter param = new NTLocationSearchParameter(
                        search, categories, localCategories,"","");
                param.setPoint(point);
                param.setRadius(1000);
                param.setCountry(NTCountry.THAILAND);

                NTLocationSearchService.executeAsync(param,
                        new ServiceRequestListener<NTLocationSearchResultSet>() {
                    @Override
                    public void onResponse(NTLocationSearchResultSet result) {
                        NTLocationSearchResult[] results = result.getResults();
                        String[] list2 = new String[results.length];
                        // TODO: 10/12/2017 recheck again.
                        for (int i = 0; i < results.length; i++) {
                            list2[i] = results[i].getLocalName() + " "
                                    + results[i].getAdminLevel4().getLocalName() + " "
                                    + results[i].getAdminLevel3().getLocalName() + " "
                                    + results[i].getAdminLevel2().getLocalName() + " "
                                    + results[i].getAdminLevel1().getLocalName();
                        }
                        Intent intent = new Intent(KeywordActivity.this, ListResultsActivity.class);
                        intent.putExtra("autoCompleteSearchResults", list2);
                        intent.putExtra("lon", lon);
                        intent.putExtra("lat", lat);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(String errorMessage,int statusCode) {
                        Toast.makeText(KeywordActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    TextWatcher keywordWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(final Editable editText) {
            final String keyword = editText.toString();
            if (keyword.length() > 3) {
                new CountDownTimer(500, 500) {
                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        NTAutoCompleteSearchParameter autocompleteParameter =
                                new NTAutoCompleteSearchParameter(keyword);
                        NTAutoCompleteSearchService.executeAsync(autocompleteParameter,
                                new ServiceRequestListener<NTAutoCompleteSearchResultSet>() {

                            @Override
                            public void onResponse(NTAutoCompleteSearchResultSet result) {
                                results = result.getResults();
                                arrAutoSearch = new String[results.length];
                                for (int i = 0; i < results.length; i++) {
                                    String name = results[i];
                                    arrAutoSearch[i] = name;
                                }
                                adapter = new ArrayAdapter<>(KeywordActivity.this,
                                        R.layout.row_search, R.id.txvLocation, arrAutoSearch);
                                lvAutoSearch.setAdapter(adapter);
                            }

                            @Override
                            public void onError(String errorMessage,int statusCode) {
                                Toast.makeText(KeywordActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }.start();
            }
        }
    };
}