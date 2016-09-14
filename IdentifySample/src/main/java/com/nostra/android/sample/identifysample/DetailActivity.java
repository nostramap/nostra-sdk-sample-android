package com.nostra.android.sample.identifysample;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import th.co.nostrasdk.Base.IServiceRequestListener;
import th.co.nostrasdk.Base.NTExtraContentService;
import th.co.nostrasdk.Parameter.Class.NTExtraContentFood;
import th.co.nostrasdk.Parameter.Class.NTExtraContentTravel;
import th.co.nostrasdk.Parameter.NTExtraContentParameter;
import th.co.nostrasdk.Result.NTExtraContentResult;

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
            NTExtraContentService.executeAsync(param, new IServiceRequestListener<NTExtraContentResult>() {
                @Override
                public void onResponse(NTExtraContentResult result, String responseCode) {
                    if (result != null) {
                        NTExtraContentTravel travel = result.getTravel();
                        NTExtraContentFood food = result.getFood();

                        if (travel != null) {
                            txvNameL.setText(travel.getPlaceName_L());
                            txvInfo.setText(travel.getAttraction1_L());
                            txvDesc.setText(travel.getHistory_L());
                            loadPicture(travel.getPicture1());

                        } else if (food != null) {
                            txvNameL.setText(food.getPlaceName_L());
                            txvInfo.setText(food.getFoodType1_L());
                            txvDesc.setText(food.getEntertainService_L());
                            loadPicture(food.getPicture1());

                        } else {
                            Toast.makeText(DetailActivity.this, "No data for given nostraId.",
                                    Toast.LENGTH_SHORT).show();
                            onBackPressed();
                        }
                    }
                }

                @Override
                public void onError(String errorMessage) {
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
