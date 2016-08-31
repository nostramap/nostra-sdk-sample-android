package com.nostra.android.sample.multimodalsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

public class TravelByActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ListView lvTravelBy;

    private TravelAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel_by);
        setInIt();
        String[] Travel;
        Travel = new String[]
                {"AIRPLANE", "BUS", "MRT" , "BTS" , "BRT" , "AIRPORT RAIL LINK" , "RAIL" , "BOAT" , "BMTA"};
        adapter = new TravelAdapter(getApplicationContext(),Travel);
        lvTravelBy.setAdapter(adapter);

        //Send parameter to MainActivity
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putStringArrayListExtra("resultTravel",adapter.getCheck());
                setResult(RESULT_OK,intent);
                onBackPressed();
            }
        });
    }
    private void setInIt(){
        lvTravelBy = (ListView)findViewById(R.id.lvTravelBy);
        btnBack = (ImageView) findViewById(R.id.btnBack);
    }
}