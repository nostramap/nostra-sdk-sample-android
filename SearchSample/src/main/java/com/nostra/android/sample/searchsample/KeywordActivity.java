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

import th.co.gissoft.nostrasdk.Base.IServiceRequestListener;
import th.co.gissoft.nostrasdk.Base.NTAutocompleteService;
import th.co.gissoft.nostrasdk.Base.NTLocationSearchService;
import th.co.gissoft.nostrasdk.Parameter.Constant.NTCountry;
import th.co.gissoft.nostrasdk.Parameter.NTAutocompleteParameter;
import th.co.gissoft.nostrasdk.Parameter.NTLocationSearchParameter;
import th.co.gissoft.nostrasdk.Result.NTAutocompleteResult;
import th.co.gissoft.nostrasdk.Result.NTAutocompleteResultSet;
import th.co.gissoft.nostrasdk.Result.NTLocationSearchResult;
import th.co.gissoft.nostrasdk.Result.NTLocationSearchResultSet;

public class KeywordActivity extends AppCompatActivity {
    private ListView lvAutoSearch;
    private EditText edtKeyword;
    private Button btnSearch;
    private ArrayAdapter<String> adapter;

    private NTAutocompleteResult[] results;
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
                NTLocationSearchParameter param = new NTLocationSearchParameter(
                        search, categories, localCategories);
                param.setLat(lat);
                param.setLon(lon);
                param.setRadius(1000);
                param.setCountry(NTCountry.THAILAND);

                NTLocationSearchService.executeAsync(param,
                        new IServiceRequestListener<NTLocationSearchResultSet>() {
                    @Override
                    public void onResponse(NTLocationSearchResultSet result, String responseCode) {
                        NTLocationSearchResult[] results = result.getResults();
                        String[] list2 = new String[results.length];

                        for (int i = 0; i < results.length; i++) {
                            list2[i] = results[i].getName_L() + " "
                                    + results[i].getAdminLevel4_L() + " "
                                    + results[i].getAdminLevel3_L() + " "
                                    + results[i].getAdminLevel2_L() + " "
                                    + results[i].getAdminLevel1_L();
                        }
                        Intent intent = new Intent(getApplicationContext(), ListResultsActivity.class);
                        intent.putExtra("autoCompleteSearchResults", list2);
                        intent.putExtra("lon", lon);
                        intent.putExtra("lat", lat);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(getApplicationContext(),errorMessage,Toast.LENGTH_SHORT).show();
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
                        NTAutocompleteParameter autocompleteParameter =
                                new NTAutocompleteParameter(keyword);
                        NTAutocompleteService.executeAsync(autocompleteParameter,
                                new IServiceRequestListener<NTAutocompleteResultSet>() {

                            @Override
                            public void onResponse(NTAutocompleteResultSet result, String responseCode) {
                                results = result.getResults();
                                arrAutoSearch = new String[results.length];
                                for (int i = 0; i < results.length; i++) {
                                    String name = results[i].getName();
                                    arrAutoSearch[i] = name;
                                }
                                adapter = new ArrayAdapter<>(KeywordActivity.this,
                                        R.layout.row_search, R.id.txvLocation, arrAutoSearch);
                                lvAutoSearch.setAdapter(adapter);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }.start();
            }
        }
    };
}