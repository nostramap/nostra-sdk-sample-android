package com.nostra.android.sample.addresssearchsample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import th.co.gissoft.nostrasdk.Base.IServiceRequestListener;
import th.co.gissoft.nostrasdk.Base.NTAddressSearchService;
import th.co.gissoft.nostrasdk.Parameter.NTAddressSearchParameter;
import th.co.gissoft.nostrasdk.Result.NTAddressSearchResult;
import th.co.gissoft.nostrasdk.Result.NTAddressSearchResultSet;

public class AttributeActivity extends Activity {
    private EditText edtHouseNo;
    private EditText edtMoo;
    private EditText edtSoiL;
    private EditText edtRoadL;
    private EditText edtAdminLevel1;
    private EditText edtAdminLevel2;
    private EditText edtAdminLevel3;
    private EditText edtPostcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attribute);

        edtHouseNo = (EditText)findViewById(R.id.edtHouseNo);
        edtMoo = (EditText)findViewById(R.id.edtMoo);
        edtSoiL = (EditText)findViewById(R.id.edtSoiL);
        edtRoadL = (EditText)findViewById(R.id.edtRoadL);
        edtAdminLevel1 = (EditText)findViewById(R.id.edtAdminLevel1);
        edtAdminLevel2 = (EditText)findViewById(R.id.edtAdminLevel2);
        edtAdminLevel3 = (EditText)findViewById(R.id.edtAdminLevel3);
        edtPostcode = (EditText)findViewById(R.id.edtPostcode);

        Button btnSearch = (Button)findViewById(R.id.button_Search2);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Determine parameter
                NTAddressSearchParameter param = new NTAddressSearchParameter();
                param.setHouseNo(edtHouseNo.getText().toString());
                param.setMoo(edtMoo.getText().toString());
                param.setSoi(edtSoiL.getText().toString());
                param.setRoad(edtRoadL.getText().toString());
                param.setAdminLevel1(edtAdminLevel1.getText().toString());
                param.setAdminLevel2(edtAdminLevel2.getText().toString());
                param.setAdminLevel3(edtAdminLevel3.getText().toString());
                param.setPostcode(edtPostcode.getText().toString());
                param.setNumReturn(5);

                // Call service NTAddressSearchService with parameter
                NTAddressSearchService.executeAsync(param, new IServiceRequestListener<NTAddressSearchResultSet>() {
                    @Override
                    public void onResponse(NTAddressSearchResultSet result, String responseCode) {
                        NTAddressSearchResult[] results = result.getResults();
                        if (results != null && results.length > 0) {
                            Intent intent = new Intent(getApplicationContext(), ListResultsActivity.class);
                            intent.putExtra("addressSearchResults", results);
                            startActivity(intent);
                        } else {
                            Toast.makeText(getApplicationContext(), "No Results", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
