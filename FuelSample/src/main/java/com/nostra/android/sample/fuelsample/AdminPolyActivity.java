package com.nostra.android.sample.fuelsample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.common.NTAdministrative;
import th.co.nostrasdk.common.NTCountry;
import th.co.nostrasdk.info.administrative.NTAdministrativeParameter;
import th.co.nostrasdk.info.administrative.NTAdministrativeResult;
import th.co.nostrasdk.info.administrative.NTAdministrativeResultSet;
import th.co.nostrasdk.info.administrative.NTAdministrativeService;
import th.co.nostrasdk.info.administrative.NTAdministrativeSorting;

public class AdminPolyActivity extends AppCompatActivity {
    private Spinner spnProvince;
    private Spinner spnDistrict;
    private Button btnSearch;

    private String codeProvince;
    private String codeDistrict;
    private String[] arrCodeProvince;
    private String[] arrNameProvince;
    private String[] arrNameDistrict;
    private String[] arrCodeDistrict;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adminpoly);

        spnProvince = (Spinner) findViewById(R.id.spnProvince);
        spnDistrict = (Spinner) findViewById(R.id.spnDistrict);
        btnSearch = (Button) findViewById(R.id.btnSearch);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (arrNameDistrict != null) {
                    Intent intent = new Intent(AdminPolyActivity.this, ListResultsActivity.class);
                    intent.putExtra("codeDistrict", codeDistrict);
                    intent.putExtra("codeProvince", codeProvince);
                    startActivity(intent);
                } else {
                    Toast.makeText(AdminPolyActivity.this, "Please wait", Toast.LENGTH_SHORT).show();
                }
            }
        });
        spnProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                codeProvince = arrCodeProvince[position];
                if (!TextUtils.isEmpty(codeProvince)) {
                    displayDistrict(codeProvince);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        spnDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                codeDistrict = arrCodeDistrict[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        displayProvince();
    }

    private void displayProvince() {
        // Setting parameter for all province
        NTAdministrative administrative = new NTAdministrative("");
        NTAdministrativeParameter param = new NTAdministrativeParameter();
        param.setAdminLevel1(administrative);
        param.setAdminLevel2(administrative);
        param.setCountry(NTCountry.THAILAND);
        param.setSortBy(NTAdministrativeSorting.SORT_BY_CODE);

        // Call NTAdministrativeService and show province
        NTAdministrativeService.executeAsync(param, new ServiceRequestListener<NTAdministrativeResultSet>() {
            @Override
            public void onResponse(NTAdministrativeResultSet result) {
                NTAdministrativeResult[] results = result.getResults();
                if (results != null && results.length > 0) {
                    arrNameProvince = new String[results.length];
                    arrCodeProvince = new String[results.length];
                    for (int i = 0; i < results.length; i++) {
                        arrNameProvince[i] = results[i].getLocalName();
                        arrCodeProvince[i] = results[i].getCode();
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminPolyActivity.this,
                            R.layout.row_province, R.id.txv_province, arrNameProvince);
                    spnProvince.setAdapter(adapter);
                }
            }

            @Override
            public void onError(String errorMessage,int statusCode) {
                Toast.makeText(AdminPolyActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Setting parameter to find all district of given province
    private void displayDistrict(String province) {
        NTAdministrative administrative = new NTAdministrative(province.trim());
        NTAdministrativeParameter adminParam = new NTAdministrativeParameter();
        adminParam.setAdminLevel1(administrative);
        adminParam.setCountry(NTCountry.THAILAND);
        adminParam.setSortBy(NTAdministrativeSorting.SORT_BY_CODE);

        // Call NTAdministrativeService and set adapter in spinner
        NTAdministrativeService.executeAsync(adminParam, new ServiceRequestListener<NTAdministrativeResultSet>() {
            @Override
            public void onResponse(NTAdministrativeResultSet result) {
                NTAdministrativeResult[] results = result.getResults();
                arrNameDistrict = new String[results.length];
                arrCodeDistrict = new String[results.length];
                for (int i = 0; i < results.length; i++) {
                    arrNameDistrict[i] = results[i].getLocalName();
                    arrCodeDistrict[i] = results[i].getCode();
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminPolyActivity.this,
                        R.layout.row_amphoe, R.id.txv_amphoe, arrNameDistrict);
                spnDistrict.setAdapter(adapter);
            }

            @Override
            public void onError(String errorMessage,int statusCode) {
                Toast.makeText(AdminPolyActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
