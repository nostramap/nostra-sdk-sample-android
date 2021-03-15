package com.nostra.android.sample.routesample;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;

public class DirectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direction);

        //get parameter from RouteActivity
        String[] Results_RouteDirections = getIntent().getStringArrayExtra("Results_RouteDirections");
        int[] resultsLength = getIntent().getIntArrayExtra("ResultLength");
        int[] resultLength = new int[resultsLength.length];
        String[] resultsDirections = new String[Results_RouteDirections.length];
        ListView lvDirection = (ListView) findViewById(R.id.lvDirection);
        for (int i = 0; i < Results_RouteDirections.length; i++) {
            resultsDirections[i] = Results_RouteDirections[i];
            resultLength[i] = resultsLength[i];
        }
        DirectionAdapter adapter = new DirectionAdapter(DirectionActivity.this, resultsDirections, resultLength);
        lvDirection.setAdapter(adapter);

        ImageButton imbBack = (ImageButton) findViewById(R.id.imbBack);
        imbBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}