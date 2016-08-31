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
import android.widget.Toast;

import th.co.gissoft.nostrasdk.Result.NTAddressSearchResult;

public class ListResultsActivity extends Activity {
    private Parcelable[] results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_results);
        ListView lvAddress = (ListView) findViewById(R.id.lvAddress);

        //Get parameter From KeyWordActivity,AttributeActivity and set adapter in listview
        results = getIntent().getParcelableArrayExtra("addressSearchResults");

        String[] arrAddress = new String[results.length];
        if (results != null && results.length > 0) {
            for (int i = 0; i < results.length; i++) {
                NTAddressSearchResult result = (NTAddressSearchResult) results[i];
                StringBuilder sb = new StringBuilder();
                String houseNo = result.getHouseNo();
                String moo = result.getMoo();
                String soiL = result.getSoi_L();
                String roadL = result.getRoad_L();
                String adminLevel1L = result.getAdminLevel1_L();
                String adminLevel2L = result.getAdminLevel2_L();
                String adminLevel3L = result.getAdminLevel3_L();
                String postcode = result.getPostcode();

                if (!TextUtils.isEmpty(houseNo)) {
                    sb.append(houseNo);
                }
                if (!TextUtils.isEmpty(moo)) {
                    sb.append("หมู่ " + moo + " ");
                }
                if (!TextUtils.isEmpty(soiL)) {
                    sb.append("ซอย " + soiL + " ");
                }
                if (!TextUtils.isEmpty(roadL)) {
                    sb.append("ถนน " + roadL + " ");
                }
                if (!TextUtils.isEmpty(adminLevel3L)) {
                    sb.append("ตำบล " + adminLevel3L + " ");
                }
                if (!TextUtils.isEmpty(adminLevel2L)) {
                    sb.append("อำเภอ " + adminLevel2L + " ");
                }
                if (!TextUtils.isEmpty(adminLevel1L)) {
                    sb.append("จังหวัด " + adminLevel1L + " ");
                }
                if (!TextUtils.isEmpty(postcode)) {
                    sb.append("รหัสไปรษณี " + postcode);
                }
                arrAddress[i] = sb.toString();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Input Attribute", Toast.LENGTH_SHORT).show();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.row_results, R.id.txvAddress, arrAddress);
        lvAddress.setAdapter(adapter);

        //Set on click in listview and send parameter to Class MapActivity
        lvAddress.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), MapActivity.class);
                NTAddressSearchResult result = (NTAddressSearchResult) results[position];
                intent.putExtra("lat", result.getLat());
                intent.putExtra("lon", result.getLon());
                intent.putExtra("houseNo", result.getHouseNo());
                intent.putExtra("moo", result.getMoo());
                intent.putExtra("soiL", result.getSoi_L());
                intent.putExtra("roadL", result.getRoad_L());
                intent.putExtra("adminLevel1L", result.getAdminLevel1_L());
                intent.putExtra("adminLevel2L", result.getAdminLevel2_L());
                intent.putExtra("adminLevel3L", result.getAdminLevel3_L());
                intent.putExtra("postcode", result.getPostcode());
                startActivity(intent);
            }
        });

        ImageButton imbBack = (ImageButton) findViewById(R.id.imbBack);
        imbBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
                startActivity(intent);
            }
        });
    }
}