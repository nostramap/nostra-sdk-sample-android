package com.nostra.android.sample.fuelsample;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class PriceFuelActivity extends AppCompatActivity {
    private TextView txvPriceDiesel;
    private TextView txvPriceDieselPremium;
    private TextView txvPriceGasohol91;
    private TextView txvPriceGasohol95;
    private TextView txvPriceGasoholE20;
    private TextView txvPriceGasoholE85;
    private TextView txvPriceGasoline91;
    private TextView txvPriceGasoline95;
    private TextView txvPriceNgv;
    private TextView txvNameBrandL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_price_fuel);

        init();
        displayPrice();
    }

    //Get parameter from Class ListResultsActivity
    private void displayPrice() {
        Bundle data = getIntent().getExtras();
        String nameBrand = data.getString("nameBrandL", "");
        double diesel = data.getDouble("diesel", 0D);
        double dieselPremium = data.getDouble("dieselPremium", 0D);
        double gasohol91 = data.getDouble("gasohol91", 0D);
        double gasohol95 = data.getDouble("gasohol95", 0D);
        double gasoholE20 = data.getDouble("gasoholE20", 0D);
        double gasoholE85 = data.getDouble("gasoholE85", 0D);
        double gasoline91 = data.getDouble("gasoline91", 0D);
        double gasoline95 = data.getDouble("gasoline95", 0D);
        double ngv = data.getDouble("ngv", 0D);

        txvNameBrandL.setText(nameBrand);
        if (diesel > 0 && (!Double.isNaN(diesel))) {
            txvPriceDiesel.setText(String.valueOf(diesel));
        } else {
            txvPriceDiesel.setText("-");
        }

        if (dieselPremium > 0 && (!Double.isNaN(dieselPremium))) {
            txvPriceDieselPremium.setText(String.valueOf(dieselPremium));
        } else {
            txvPriceDieselPremium.setText("-");
        }

        if (gasohol91 > 0 && (!Double.isNaN(gasohol91))) {
            txvPriceGasohol91.setText(String.valueOf(gasohol91));
        } else {
            txvPriceGasohol91.setText("-");
        }

        if (gasohol95 > 0 && (!Double.isNaN(gasohol95))) {
            txvPriceGasohol95.setText(String.valueOf(gasohol95));
        } else {
            txvPriceGasohol95.setText("-");
        }

        if (gasoholE20 > 0 && (!Double.isNaN(gasoholE20))) {
            txvPriceGasoholE20.setText(String.valueOf(gasoholE20));
        } else {
            txvPriceGasoholE20.setText("-");
        }

        if (gasoholE85 > 0 && (!Double.isNaN(gasoholE85))) {
            txvPriceGasoholE85.setText(String.valueOf(gasoholE85));
        } else {
            txvPriceGasoholE85.setText("-");
        }

        if (gasoline91 > 0 && (!Double.isNaN(gasoline91))) {
            txvPriceGasoline91.setText(String.valueOf(gasoline91));
        } else {
            txvPriceGasoline91.setText("-");
        }

        if (gasoline95 > 0 && (!Double.isNaN(gasoline95))) {
            txvPriceGasoline95.setText(String.valueOf(gasoline95));
        } else {
            txvPriceGasoline95.setText("-");
        }

        if (ngv > 0 && (!Double.isNaN(ngv))) {
            txvPriceNgv.setText(String.valueOf(ngv));
        } else {
            txvPriceNgv.setText("-");
        }
    }

    private void init() {
        txvPriceDiesel = (TextView) findViewById(R.id.txvPriceDiesel);
        txvPriceDieselPremium = (TextView) findViewById(R.id.txvPriceDieselPremium);
        txvPriceGasohol91 = (TextView) findViewById(R.id.txvPriceGasohol91);
        txvPriceGasohol95 = (TextView) findViewById(R.id.txvPriceGasohol95);
        txvPriceGasoholE20 = (TextView) findViewById(R.id.txvPriceGasoholE20);
        txvPriceGasoholE85 = (TextView) findViewById(R.id.txvPriceGasoholE85);
        txvPriceGasoline91 = (TextView) findViewById(R.id.txvPriceGasoline91);
        txvPriceGasoline95 = (TextView) findViewById(R.id.txvPriceGasoline95);
        txvPriceNgv = (TextView) findViewById(R.id.txvPriceNgv);
        txvNameBrandL = (TextView) findViewById(R.id.txvNameBrandL);
        ImageButton imbBack = (ImageButton) findViewById(R.id.imbBack);
        imbBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }
}