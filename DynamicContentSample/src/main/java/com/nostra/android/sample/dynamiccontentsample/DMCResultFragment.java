package com.nostra.android.sample.dynamiccontentsample;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import th.co.gissoft.nostrasdk.Base.IServiceRequestListener;
import th.co.gissoft.nostrasdk.Base.NTDynamicContentService;
import th.co.gissoft.nostrasdk.Parameter.Constant.NTDynamicContentSort;
import th.co.gissoft.nostrasdk.Parameter.NTDynamicContentParameter;
import th.co.gissoft.nostrasdk.Result.NTDynamicContentResult;
import th.co.gissoft.nostrasdk.Result.NTDynamicContentResultSet;

public class DMCResultFragment extends Fragment {
    private static final String LAYER_ID = "LAYER_ID";
    private static final String LATITUDE = "LATITUDE";
    private static final String LONGITUDE = "LONGITUDE";

    private RecyclerView rcvLayerResult;

    private String layerID;
    private double lat, lon;
    private NTDynamicContentResult[] dmcResults;

    public DMCResultFragment() {}

    static DMCResultFragment newInstance(String layerID, double lat, double lon) {
        DMCResultFragment fragment = new DMCResultFragment();
        Bundle args = new Bundle();
        args.putString(LAYER_ID, layerID);
        args.putDouble(LATITUDE, lat);
        args.putDouble(LONGITUDE, lon);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            layerID = getArguments().getString(LAYER_ID);
            lat = getArguments().getDouble(LATITUDE);
            lon = getArguments().getDouble(LONGITUDE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layoutView = inflater.inflate(R.layout.fragment_dmc_result, container, false);

        rcvLayerResult = (RecyclerView) layoutView.findViewById(R.id.rcvLayerResult);
        ImageView imvBack = (ImageView) layoutView.findViewById(R.id.imvBack);
        imvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        // Setting parameter
        final int pageNumber = 1;
        final int numberOfReturn = 5;
        NTDynamicContentParameter parameter = new NTDynamicContentParameter(layerID, lat, lon,
                NTDynamicContentSort.NAME_ASC, numberOfReturn, pageNumber);
        parameter.setRadius(2000);

        // Call service
        NTDynamicContentService.executeAsync(parameter, new IServiceRequestListener<NTDynamicContentResultSet>() {
            @Override
            public void onResponse(NTDynamicContentResultSet result, String responseCode) {
                dmcResults = result.getResults();
                DMCResultAdapter dmcResultAdapter = new DMCResultAdapter(dmcResults, new DMCResultAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(int position) {
                        // When click show the detail of the content
                        if (getActivity() instanceof DynamicContentActivity) {
                            ((DynamicContentActivity) getActivity()).showDetail(dmcResults[position]);
                        }
                    }
                });
                rcvLayerResult.setAdapter(dmcResultAdapter);
                rcvLayerResult.setLayoutManager(new LinearLayoutManager(getContext()));
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        return layoutView;
    }
}