package com.nostra.android.sample.dynamiccontentsample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.net.URL;

import th.co.nostrasdk.query.dynamic.NTDynamicContentResult;
import th.co.nostrasdk.query.dynamic.NTPoiInfo;

class DMCResultAdapter extends RecyclerView.Adapter<DMCResultAdapter.ResultViewHolder> {
    private NTDynamicContentResult[] dmcResults;
    private OnItemClickListener listener;

    DMCResultAdapter(NTDynamicContentResult[] dmcResults, OnItemClickListener listener) {
        this.dmcResults = dmcResults;
        this.listener = listener;
    }

    // Interface for handle event onClick
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout rllResult;
        TextView txvName, txvAddress, txvAddInfo;
        ImageView imvLogo;

        ResultViewHolder(View itemView) {
            super(itemView);
            rllResult = (RelativeLayout) itemView.findViewById(R.id.rllResult);
            txvName = (TextView) itemView.findViewById(R.id.txvName);
            txvAddress = (TextView) itemView.findViewById(R.id.txvAddress);
            txvAddInfo = (TextView) itemView.findViewById(R.id.txvAddInfo);
            imvLogo = (ImageView) itemView.findViewById(R.id.imvLogo);
        }
    }

    @Override
    public ResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result_list, parent, false);
        return new ResultViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final ResultViewHolder holder, int position) {
        NTDynamicContentResult result = dmcResults[position];
        holder.txvName.setText(result.getLocalName());
        holder.txvAddress.setText(result.getLocalAddress());
        NTPoiInfo additionalInfo = result.getAdditionalInfo();
        if (additionalInfo != null) {
            holder.txvAddInfo.setText(additionalInfo.getLocalInfo());
        }
        new LoadImageTask(holder.imvLogo).execute(result.getIcon());

        holder.rllResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return dmcResults.length;
    }

    // Async Class to load image from url and put in ImageView
    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imvLogo;

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
                imvLogo.setImageBitmap(logo);
            }
        }
    }
}