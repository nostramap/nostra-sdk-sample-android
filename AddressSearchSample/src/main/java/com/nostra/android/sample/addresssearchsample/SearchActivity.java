package com.nostra.android.sample.addresssearchsample;

import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TabHost;

public class SearchActivity extends AppCompatActivity {
    private LocalActivityManager localActivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        TabHost tabHostSearch;
        tabHostSearch = (TabHost)findViewById(R.id.tabhostSearch);
        localActivityManager = new LocalActivityManager(this, false);
        tabHostSearch.setup(localActivityManager);
        localActivityManager.dispatchCreate(savedInstanceState);

        TabHost.TabSpec spec;
        Intent activity_tab1 = new Intent().setClass(this, KeywordActivity.class);
        spec = tabHostSearch.newTabSpec("Tab1")
                .setIndicator("Keyword")
                .setContent(activity_tab1);
        tabHostSearch.addTab(spec);

        Intent activity_tab2 = new Intent().setClass(this, AttributeActivity.class);
        spec = tabHostSearch.newTabSpec("Tab2")
                .setIndicator("Attribute")
                .setContent(activity_tab2);
        tabHostSearch.addTab(spec);
    }

    @Override
    protected void onPause() {
        super.onPause();
        localActivityManager.dispatchPause(isFinishing());
    }

    @Override
    protected void onResume(){
        super.onResume();
        localActivityManager.dispatchResume();
    }
}
