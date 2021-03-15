package com.nostra.android.sample.dynamiccontentsample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.net.URL;

import th.co.nostrasdk.query.dynamic.NTDynamicContentResult;

public class DMCDetailFragment extends Fragment {
    private static final String RESULT = "DMC_RESULT";

    private PoiItem poiItem;

    public DMCDetailFragment() {
    }

    static DMCDetailFragment newInstance(NTDynamicContentResult dmcResult) {
        PoiItem item = new PoiItem();
        item.setLocalName(dmcResult.getLocalName());
        item.setLocalAddress(dmcResult.getLocalAddress());
        item.setLocalDetail(dmcResult.getLocalDetail());
        item.setTelephone(dmcResult.getTelephoneNumber());
        item.setWebsite(dmcResult.getWebsite());
        item.setMediaUrl(dmcResult.getMediaThumbnailUrl());
        item.setLatitude(dmcResult.getPoint().getY());
        item.setLongitude(dmcResult.getPoint().getX());

        DMCDetailFragment fragment = new DMCDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(RESULT, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            poiItem = getArguments().getParcelable(RESULT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layoutView = inflater.inflate(R.layout.fragment_dmc_detail, container, false);

        LinearLayout lnlDetail = (LinearLayout) layoutView.findViewById(R.id.lnlDetail);
        lnlDetail.setOnClickListener(null);

        ImageView imvBack = (ImageView) layoutView.findViewById(R.id.imvBack);
        imvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        ImageView imvPicture = (ImageView) layoutView.findViewById(R.id.imvPicture);
        if (poiItem.getMediaUrl() != null) {
            new LoadImageTask(imvPicture).execute(poiItem.getMediaUrl());
        }

        TextView txvName = (TextView) layoutView.findViewById(R.id.txvName);
        txvName.setText(poiItem.getLocalName());

        TextView txvAddress = (TextView) layoutView.findViewById(R.id.txvAddress);
        txvAddress.setText(poiItem.getLocalAddress());

        RelativeLayout rllDetail = (RelativeLayout) layoutView.findViewById(R.id.rllDetail);
        if (poiItem.getLocalDetail() != null) {
            rllDetail.setVisibility(View.VISIBLE);
            TextView txvDetail = (TextView) layoutView.findViewById(R.id.txvDetail);
            txvDetail.setText(poiItem.getLocalDetail());
        }

        RelativeLayout rllInfo = (RelativeLayout) layoutView.findViewById(R.id.rllInfo);
        if (poiItem.getLocalName() != null) {
            rllInfo.setVisibility(View.VISIBLE);
            TextView txvName2 = (TextView) layoutView.findViewById(R.id.txvName2);
            txvName2.setText(poiItem.getLocalName());
        }

        RelativeLayout rllTel = (RelativeLayout) layoutView.findViewById(R.id.rllTel);
        if (poiItem.getTelephone() != null) {
            rllTel.setVisibility(View.VISIBLE);
            TextView txvTel = (TextView) layoutView.findViewById(R.id.txvTel);
            txvTel.setText(poiItem.getTelephone());
        }

        RelativeLayout rllWeb = (RelativeLayout) layoutView.findViewById(R.id.rllWeb);
        if (poiItem.getWebsite() != null) {
            rllWeb.setVisibility(View.VISIBLE);
            TextView txvWeb = (TextView) layoutView.findViewById(R.id.txvWeb);
            txvWeb.setText(poiItem.getWebsite());
        }

        FloatingActionButton fabShare = (FloatingActionButton) layoutView.findViewById(R.id.fabShare);
        fabShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show share url
                if (getActivity() instanceof DynamicContentActivity) {
                    ((DynamicContentActivity) getActivity()).createShareUrl(poiItem);
                }
            }
        });

        ImageView imvShowOnMap = (ImageView) layoutView.findViewById(R.id.imvShowOnMap);
        imvShowOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show location on map
                if (getActivity() instanceof DynamicContentActivity) {
                    ((DynamicContentActivity) getActivity()).showOnMap(poiItem);
                }
            }
        });
        return layoutView;
    }

    // Async Class to load image from url and put in ImageView
    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imvLogo;

        LoadImageTask(ImageView imvLogo) {
            this.imvLogo = imvLogo;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String urlStr = params[0];
            Bitmap logo = null;
            try {
                URL url = new URL(urlStr);
                logo = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }

            return logo;
        }

        @Override
        protected void onPostExecute(Bitmap logo) {
            if (logo != null) {
                imvLogo.setColorFilter(Color.TRANSPARENT);
                imvLogo.setImageBitmap(logo);
            }
        }
    }
}