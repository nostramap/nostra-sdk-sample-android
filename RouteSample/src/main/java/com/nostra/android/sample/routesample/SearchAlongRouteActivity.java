package com.nostra.android.sample.routesample;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.common.NTPoint;
import th.co.nostrasdk.search.route.NTSearchAlongRouteParameter;
import th.co.nostrasdk.search.route.NTSearchAlongRouteResult;
import th.co.nostrasdk.search.route.NTSearchAlongRouteResultSet;
import th.co.nostrasdk.search.route.NTSearchAlongRouteService;
import th.co.nostrasdk.search.route.NTSearchType;

public class SearchAlongRouteActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText mTxtKeyword;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_along_route);

        ImageButton mBtnBack = (ImageButton) findViewById(R.id.imbBack);
        Button mBtnSearch = (Button) findViewById(R.id.buttonSearch);
        mTxtKeyword = (EditText) findViewById(R.id.textKeyword);
        mListView = (ListView) findViewById(R.id.listResult);

        mBtnBack.setOnClickListener(this);
        mBtnSearch.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSearch:
                Bundle bundle = getIntent().getExtras();
                if (bundle != null) {
                    double[] pointX = bundle.getDoubleArray("pointX");
                    double[] pointY = bundle.getDoubleArray("pointY");
                    if (pointX != null && pointY != null) {
                        NTPoint[] points = new NTPoint[pointX.length];
                        for (int i = 0; i < pointX.length; i++) {
                            points[i] = new NTPoint(pointX[i], pointY[i]);
                        }
                        String keyword = mTxtKeyword.getText().toString();
                        // Perform search
                        performSearch(points, keyword);
                    }
                }
                break;
            case R.id.imbBack:
                onBackPressed();
                break;
            default:
                break;
        }
    }

    private void performSearch(final NTPoint[] points, final String keyword) {
        NTSearchAlongRouteParameter param = new NTSearchAlongRouteParameter(
                points, keyword, NTSearchType.NEARBY);
        NTSearchAlongRouteService.executeAsync(param, new ServiceRequestListener<NTSearchAlongRouteResultSet>() {
            @Override
            public void onResponse(NTSearchAlongRouteResultSet resultSet) {
                NTSearchAlongRouteResult[] results = resultSet.getResults();

                String[] displayStrings = new String[results.length];
                for (int i = 0; i < results.length; i++) {
                    displayStrings[i] = results[i].getLocalName() + " " +
                            results[i].getHouseNumber() + " " +
                            results[i].getAdminLevel4().getLocalName() + " " +
                            results[i].getAdminLevel3().getLocalName() + " " +
                            results[i].getAdminLevel2().getLocalName() + " " +
                            results[i].getAdminLevel1().getLocalName();
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(SearchAlongRouteActivity.this,
                        android.R.layout.simple_list_item_1, displayStrings);
                mListView.setAdapter(adapter);
            }

            @Override
            public void onError(String s, int i) {
            }
        });
    }
}
