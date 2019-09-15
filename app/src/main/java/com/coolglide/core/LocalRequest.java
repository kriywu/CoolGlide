package com.coolglide.core;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

import com.coolglide.BitmapUtil;

/**
 * author : kriywu
 * date : 2019/9/15
 * description : this is local request.
 */
public class LocalRequest extends UriRequest {
    private static final String TAG = "LocalRequest";

    public LocalRequest(String uri) {
        super(uri);
    }

    @Override
    public void placeholder() {

    }

    @Override
    public void loadBitmap() {
        View view = mTargetView.get();
        if (view == null) {
            Log.d(TAG, "loadBitmap: view == null");
            return;
        }

        // get w and h before decode bitmap
        SizeDeterminer sizeDeterminer = SizeDeterminer.Obtains(view, (width, height) -> {
            Log.d(TAG, "loadBitmap: width = " + width + " height = " + height);
            Bitmap fit = BitmapUtil.decodeBitmapFromFile(mUri, width, height);
            // attach bitmap
            attach(fit);
        });
        sizeDeterminer.getSize();
    }
}
