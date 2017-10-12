package com.nostra.android.sample.identifysample;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import th.co.nostrasdk.ServiceRequestListener;
import th.co.nostrasdk.query.extra.NTAttraction;
import th.co.nostrasdk.query.extra.NTEntertainmentService;
import th.co.nostrasdk.query.extra.NTExtraContentFood;
import th.co.nostrasdk.query.extra.NTExtraContentParameter;
import th.co.nostrasdk.query.extra.NTExtraContentResult;
import th.co.nostrasdk.query.extra.NTExtraContentService;
import th.co.nostrasdk.query.extra.NTExtraContentTravel;
import th.co.nostrasdk.query.extra.NTFoodType;

public class DetailActivity extends AppCompatActivity {

    private ImageButton imbBack;
    private ImageView imvLocation;
    private TextView txvNameL;
    private TextView txvInfo;
    private TextView txvDesc;

    private LoadPictureTask loadPictureTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        imbBack = (ImageButton) findViewById(R.id.imbBack);
        imvLocation = (ImageView) findViewById(R.id.imvLocation);
        txvNameL = (TextView) findViewById(R.id.txvNameL);
        txvInfo = (TextView) findViewById(R.id.txvInfo);
        txvDesc = (TextView) findViewById(R.id.txvDesc);

        imbBack.setOnClickListener(imbBackClick);

        String nostraId = getIntent().getStringExtra("nostraId");
        if (nostraId != null && nostraId.length() > 0) {
            NTExtraContentParameter param = new NTExtraContentParameter(nostraId);
            NTExtraContentService.executeAsync(param, new ServiceRequestListener<NTExtraContentResult>() {
                @Override
                public void onResponse(NTExtraContentResult result) {
                    if (result != null) {
                        NTExtraContentTravel travel = result.getTravel();
                        NTExtraContentFood food = result.getFood();
                        // TODO: 10/12/2017 please recheck again.
                        if (travel != null) {
                            txvNameL.setText(travel.getPlaceZone());
                            NTAttraction[] attractions = travel.getAttractions();
                            txvInfo.setText(attractions[0].getLocalAttraction());
                            txvDesc.setText(travel.getLocalHistory());
                            String[] picture = travel.getPictureUrls();
                            loadPicture(picture[0]);

                        } else if (food != null) {
                            txvNameL.setText(food.getPlaceZone());
                            NTFoodType[] type = food.getFoodTypes();
                            txvInfo.setText(type[0].getLocalType());
                            NTEntertainmentService entertainmentService = food.getEntertainmentService();
                            txvDesc.setText(entertainmentService.getLocalService());
                            String[] picture = food.getPictureUrls();
                            loadPicture(picture[0]);

                        } else {
                            Toast.makeText(DetailActivity.this, "No data for given nostraId.",
                                    Toast.LENGTH_SHORT).show();
                            onBackPressed();
                        }
                    }
                }

                @Override
                public void onError(String errorMessage, int statusCode) {
                    Toast.makeText(DetailActivity.this, "No data for given nostraId.",
                            Toast.LENGTH_SHORT).show();
                    onBackPressed();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (loadPictureTask != null && loadPictureTask.getStatus() == AsyncTask.Status.RUNNING) {
            loadPictureTask.cancel(true);
        }
        super.onDestroy();
    }

    private View.OnClickListener imbBackClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

    private void loadPicture(String pictureUrl) {
        if (pictureUrl != null && pictureUrl.length() > 0) {
            loadPictureTask = new LoadPictureTask(imvLocation);
            loadPictureTask.execute(pictureUrl);
        }
    }
}
