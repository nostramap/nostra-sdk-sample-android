package com.nostra.android.sample.identifysample;

import android.os.AsyncTask;
import androidx.appcompat.app.AppCompatActivity;
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
import th.co.nostrasdk.query.extra.NTExtraContentHotel;
import th.co.nostrasdk.query.extra.NTExtraContentParameter;
import th.co.nostrasdk.query.extra.NTExtraContentResult;
import th.co.nostrasdk.query.extra.NTExtraContentService;
import th.co.nostrasdk.query.extra.NTExtraContentTravel;
import th.co.nostrasdk.query.extra.NTFoodType;

public class DetailActivity extends AppCompatActivity {

    private ImageView imvLocation;
    private TextView txvNameL;
    private TextView txvInfo;
    private TextView txvDesc;

    private LoadPictureTask loadPictureTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ImageButton imbBack = (ImageButton) findViewById(R.id.imbBack);
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
                        NTExtraContentHotel hotel = result.getHotel();
                        if (travel != null) {
                            String[] picture = travel.getPictureUrls();
                            NTAttraction[] attractions = travel.getAttractions();

                            txvNameL.setText(travel.getPlaceZone());
                            txvInfo.setText(attractions[0].getLocalAttraction());
                            txvDesc.setText(travel.getLocalHistory());
                            if (picture.length > 0)
                                loadPicture(picture[0]);

                        } else if (food != null) {
                            NTFoodType[] type = food.getFoodTypes();
                            NTEntertainmentService entertainmentService = food.getEntertainmentService();
                            String[] picture = food.getPictureUrls();

                            txvNameL.setText(food.getPlaceZone());
                            txvInfo.setText(type[0].getLocalType());
                            txvDesc.setText(entertainmentService.getLocalService());
                            if (picture.length > 0)
                                loadPicture(picture[0]);

                        } else if (hotel != null) {
                            String[] sportsAndRecreation = hotel.getSportsAndRecreations();
                            String[] picture = hotel.getPictureUrls();

                            txvNameL.setText(hotel.getName());
                            txvInfo.setText(hotel.getHotelType());
                            txvDesc.setText(sportsAndRecreation[0]);
                            if (picture.length > 0)
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
