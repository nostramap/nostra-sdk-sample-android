package com.nostra.android.sample.dynamiccontentsample;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import th.co.nostrasdk.query.dynamic.NTDynamicContentListResult;

/**
 * Adapter for DMC layer list
 */
class DMCLayerAdapter extends RecyclerView.Adapter<DMCLayerAdapter.LayerViewHolder> {
    private NTDynamicContentListResult[] layerResults;
    private OnItemClickListener listener;

    DMCLayerAdapter(NTDynamicContentListResult[] layerResults, OnItemClickListener listener) {
        this.layerResults = layerResults;
        this.listener = listener;
    }

    // Interface for handle event onClick
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    static class LayerViewHolder extends RecyclerView.ViewHolder {
        TextView txvLayerName;

        LayerViewHolder(View itemView) {
            super(itemView);
            txvLayerName = (TextView) itemView.findViewById(R.id.txvLayerName);
        }
    }

    @Override
    public LayerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layer_list, parent, false);
        return new LayerViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final LayerViewHolder holder, int position) {
        NTDynamicContentListResult layer = layerResults[position];
        holder.txvLayerName.setText(layer.getLocalName());
        holder.txvLayerName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return layerResults.length;
    }
}