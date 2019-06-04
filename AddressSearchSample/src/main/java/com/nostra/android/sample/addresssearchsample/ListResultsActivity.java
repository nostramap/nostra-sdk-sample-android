package com.nostra.android.sample.addresssearchsample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import th.co.nostrasdk.common.NTPoint;
import th.co.nostrasdk.search.address.NTAddressSearchResult;

public class ListResultsActivity extends Activity {
    private List<NTAddressSearchResult> results;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_results);

        gson = new Gson();
        ListView lvAddress = (ListView) findViewById(R.id.lvAddress);

        //Get parameter From KeyWordActivity,AttributeActivity and set adapter in listview
        String json = getIntent().getStringExtra("results");
        Type type = new TypeToken<List<NTAddressSearchResult>>(){}.getType();
        results = gson.fromJson(json,type);
        //results = getIntent().getStringExtra("results");

        String[] arrAddress = new String[results.size()];
        if (results.size() > 0) {
            for (int i = 0; i < results.size(); i++) {
                NTAddressSearchResult result =  results.get(i);
                StringBuilder sb = new StringBuilder();
                String houseNo = result.getHouseNo();
                String moo = result.getMoo();
                String soiL = result.getLocalSoiName();
                String roadL = result.getAdminLevel4().getLocalName();
                String adminLevel1L = result.getAdminLevel1().getLocalName();
                String adminLevel2L = result.getAdminLevel2().getLocalName();
                String adminLevel3L = result.getAdminLevel3().getLocalName();
                String postcode = result.getPostcode();

                if (!TextUtils.isEmpty(houseNo)) {
                    sb.append(houseNo);
                }
                if (!TextUtils.isEmpty(moo)) {
                    sb.append("หมู่ ").append(moo).append(" ");
                }
                if (!TextUtils.isEmpty(soiL)) {
                    sb.append("ซอย ").append(soiL).append(" ");
                }
                if (!TextUtils.isEmpty(roadL)) {
                    sb.append("ถนน ").append(roadL).append(" ");
                }
                if (!TextUtils.isEmpty(adminLevel3L)) {
                    sb.append("ตำบล ").append(adminLevel3L).append(" ");
                }
                if (!TextUtils.isEmpty(adminLevel2L)) {
                    sb.append("อำเภอ ").append(adminLevel2L).append(" ");
                }
                if (!TextUtils.isEmpty(adminLevel1L)) {
                    sb.append("จังหวัด ").append(adminLevel1L).append(" ");
                }
                if (!TextUtils.isEmpty(postcode)) {
                    sb.append("รหัสไปรษณี ").append(postcode);
                }
                arrAddress[i] = sb.toString();
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.row_results, R.id.txvAddress, arrAddress);
        lvAddress.setAdapter(adapter);
        lvAddress.setOnItemClickListener(addressListItemClick);

        ImageButton imbBack = (ImageButton) findViewById(R.id.imbBack);
        imbBack.setOnClickListener(imbBackClick);
    }

    AdapterView.OnItemClickListener addressListItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            NTAddressSearchResult result =  results.get(position);
            NTPoint point = result.getPoint();
            if (point != null) {
                Intent intent = new Intent(ListResultsActivity.this, MapActivity.class)
                        .putExtra("lat", point.getY())
                        .putExtra("lon", point.getX())
                        .putExtra("houseNo", result.getHouseNo())
                        .putExtra("moo", result.getMoo())
                        .putExtra("soiL", result.getLocalSoiName())
                        .putExtra("roadL", result.getAdminLevel4().getLocalName())
                        .putExtra("adminLevel1L", result.getAdminLevel1().getLocalName())
                        .putExtra("adminLevel2L", result.getAdminLevel2().getLocalName())
                        .putExtra("adminLevel3L", result.getAdminLevel3().getLocalName())
                        .putExtra("postcode", result.getPostcode());
                startActivity(intent);
            }
        }
    };

    View.OnClickListener imbBackClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(ListResultsActivity.this, SearchActivity.class);
            startActivity(intent);
        }
    };
}