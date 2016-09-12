package com.nostra.android.sample.addresssearchsample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import th.co.nostrasdk.Base.IServiceRequestListener;
import th.co.nostrasdk.Base.NTAddressSearchService;
import th.co.nostrasdk.Parameter.NTAddressSearchParameter;
import th.co.nostrasdk.Result.NTAddressSearchResult;
import th.co.nostrasdk.Result.NTAddressSearchResultSet;

public class KeywordActivity extends Activity {
    private EditText edtKeyword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyword);
        edtKeyword = (EditText) findViewById(R.id.edtKeyword);

        Button btnSearch = (Button)findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(btnSearchClick);
    }

    View.OnClickListener btnSearchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Determine parameter
            NTAddressSearchParameter param = new NTAddressSearchParameter(edtKeyword.getText().toString());
            param.setNumReturn(5);

            // Call service NTAddressSearchService with parameter
            NTAddressSearchService.executeAsync(param, new IServiceRequestListener<NTAddressSearchResultSet>() {
                @Override
                public void onResponse(NTAddressSearchResultSet result, String responseCode) {
                    NTAddressSearchResult[] results = result.getResults();
                    if (results != null && results.length > 0) {
                        Intent intent = new Intent(KeywordActivity.this, ListResultsActivity.class);
                        intent.putExtra("results", results);
                        startActivity(intent);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(KeywordActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
}
