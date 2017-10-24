package com.nostra.android.sample.multimodalsample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;

public class ResultsDirectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_direction);

        String[] directions;
        String[] type;
        String[] arrDistanceAndTime;
        //Get parameter and show information in listview
        Bundle data = getIntent().getExtras();
        directions = data.getStringArray("directions");
        type = data.getStringArray("type");
        arrDistanceAndTime = data.getStringArray("distance_time");

        ArrayList<String> arrDirectionResult;
        if (directions!=null){
            arrDirectionResult = new ArrayList<>(Arrays.asList(directions));
            arrDirectionResult.add(0,"เริ่มต้นเดินทาง");
            int size = arrDirectionResult.size();
            arrDirectionResult.add(size,"สิ้นสุดการเดินทาง");

            ListView lvDirection;
            lvDirection = (ListView)findViewById(R.id.lvDirections);
            ResultDirectionAdapter adapter = new ResultDirectionAdapter(this,
                    arrDirectionResult, type, arrDistanceAndTime);
            lvDirection.setAdapter(adapter);
        }

        ImageView imvBack;
        imvBack = (ImageView)findViewById(R.id.imbBack);
        imvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}