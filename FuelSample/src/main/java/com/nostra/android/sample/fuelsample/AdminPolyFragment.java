package com.nostra.android.sample.fuelsample;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class AdminPolyFragment extends Fragment {
    private Spinner spnProvince;
    private Spinner spnDistrict;
    private Button btnSearch;

    private String codeProvince;
    private String codeDistrict;
    private String[] arrCodeProvince;
    private String[] arrNameProvince;
    private String[] arrNameDistrict;
    private String[] arrCodeDistrict;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_adminpoly, container, false);

        spnProvince = (Spinner) view.findViewById(R.id.spnProvince);
        spnDistrict = (Spinner) view.findViewById(R.id.spnDistrict);
        btnSearch = (Button) view.findViewById(R.id.btnSearch);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (arrNameDistrict != null) {
                    Intent intent = new Intent(getActivity(), ListResultsActivity.class);
                    intent.putExtra("codeDistrict", codeDistrict);
                    intent.putExtra("codeProvince", codeProvince);
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), "Please wait", Toast.LENGTH_SHORT).show();
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spnDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                codeDistrict = arrCodeDistrict[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        displayProvince();
        return view;
    }

    private void displayProvince() {
        // Setting parameter for all province
        NTAdministrativeParameter param = new NTAdministrativeParameter();
        param.setCountry(NTCountry.THAILAND);
        param.setSortBy(NTAdministrativeSorting.SORT_BY_CODE);

        // Call NTAdministrativeService and show province
        NTAdministrativeService.executeAsync(param, new ServiceRequestListener<NTAdministrativeResultSet>() {
            @Override
            public void onResponse(NTAdministrativeResultSet result) {
                NTAdministrativeResult[] results = result.getResults();
                if (results.length > 0) {
                    arrNameProvince = new String[results.length];
                    arrCodeProvince = new String[results.length];
                    for (int i = 0; i < results.length; i++) {
                        arrNameProvince[i] = results[i].getLocalName();
                        arrCodeProvince[i] = results[i].getCode();
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                            R.layout.row_province, R.id.txv_province, arrNameProvince);
                    spnProvince.setAdapter(adapter);
                }
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
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
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                        R.layout.row_amphoe, R.id.txv_amphoe, arrNameDistrict);
                spnDistrict.setAdapter(adapter);
            }

            @Override
            public void onError(String errorMessage, int statusCode) {
                Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
