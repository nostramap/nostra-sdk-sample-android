package com.nostra.android.sample.dynamiccontentsample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
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
    private static final String DMC_RESULT = "DMC_RESULT";

    private NTDynamicContentResult dmcResult;

    public DMCDetailFragment() {}

    static DMCDetailFragment newInstance(NTDynamicContentResult dmcResult) {
        DMCDetailFragment fragment = new DMCDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(DMC_RESULT, dmcResult);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            dmcResult = getArguments().getParcelable(DMC_RESULT);
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
        if (dmcResult.getMediaThumbnailUrl() != null) {
            new LoadImageTask(imvPicture).execute(dmcResult.getMediaThumbnailUrl());
        }

        TextView txvName = (TextView) layoutView.findViewById(R.id.txvName);
        txvName.setText(dmcResult.getLocalName());

        TextView txvAddress = (TextView) layoutView.findViewById(R.id.txvAddress);
        txvAddress.setText(dmcResult.getLocalAddress());

        RelativeLayout rllDetail = (RelativeLayout) layoutView.findViewById(R.id.rllDetail);
        if (dmcResult.getLocalDetail() != null) {
            rllDetail.setVisibility(View.VISIBLE);
            TextView txvDetail = (TextView) layoutView.findViewById(R.id.txvDetail);
            txvDetail.setText(dmcResult.getLocalDetail());
        }

        RelativeLayout rllInfo = (RelativeLayout) layoutView.findViewById(R.id.rllInfo);
        if (dmcResult.getLocalName() != null) {
            rllInfo.setVisibility(View.VISIBLE);
            TextView txvName2 = (TextView) layoutView.findViewById(R.id.txvName2);
            txvName2.setText(dmcResult.getLocalName());
        }

        RelativeLayout rllTel = (RelativeLayout) layoutView.findViewById(R.id.rllTel);
        if (dmcResult.getTelephoneNumber() != null) {
            rllTel.setVisibility(View.VISIBLE);
            TextView txvTel = (TextView) layoutView.findViewById(R.id.txvTel);
            txvTel.setText(dmcResult.getTelephoneNumber());
        }

        RelativeLayout rllWeb = (RelativeLayout) layoutView.findViewById(R.id.rllWeb);
        if (dmcResult.getWebsite() != null) {
            rllWeb.setVisibility(View.VISIBLE);
            TextView txvWeb = (TextView) layoutView.findViewById(R.id.txvWeb);
            txvWeb.setText(dmcResult.getWebsite());
        }

        FloatingActionButton fabShare = (FloatingActionButton) layoutView.findViewById(R.id.fabShare);
        fabShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show share url
                if (getActivity() instanceof DynamicContentActivity) {
                    ((DynamicContentActivity) getActivity()).createShareUrl(dmcResult);
                }
            }
        });

        ImageView imvShowOnMap = (ImageView) layoutView.findViewById(R.id.imvShowOnMap);
        imvShowOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show location on map
                if (getActivity() instanceof DynamicContentActivity) {
                    ((DynamicContentActivity) getActivity()).showOnMap(dmcResult);
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