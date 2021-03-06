package com.nostra.android.sample.identifysample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class LoadPictureTask extends AsyncTask<String, Void, Bitmap> {
    private ImageView imageView;

    // Load picture from URL
    LoadPictureTask(ImageView imageView) {
        this.imageView = imageView;
    }

    @Override
    protected Bitmap doInBackground(String... urlIcon) {
        String urlIcon1 = urlIcon[0];
        return downloadImage(urlIcon1);
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (!isCancelled()) {
            imageView.setImageBitmap(result);
        }
    }

    private Bitmap downloadImage(String url) {
        Bitmap bitmap = null;
        try {
            URL newURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) newURL.openConnection();
            InputStream is = con.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}
