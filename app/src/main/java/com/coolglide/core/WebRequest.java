package com.coolglide.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import com.coolglide.core.UriRequest;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static com.coolglide.BitmapUtil.caculateInSampleSize;

/**
 * author : kriywu
 * date : 2019/9/15
 * description : this is request to load web photo
 */
public class WebRequest extends UriRequest {
    private static final String TAG = "WebRequest";

    public WebRequest(String uri) {
        super(uri);
    }

    @Override
    public void placeholder() {

    }

    @Override
    public void loadBitmap() {
        URL uri = null;
        try {
            // get bitmap form web 
            uri = new URL(mUri);
            InputStream in = uri.openConnection().getInputStream();
            Bitmap source = BitmapFactory.decodeStream(in);

            // compress it if needed
            if (mTargetView.get() == null) return;
            SizeDeterminer determiner = SizeDeterminer.Obtains(mTargetView.get(), (width, height) -> {
                // sample <= 1 , needn't compress
                // else compress
                // recycle source bitmap object
                float sample = caculateInSampleSize(source.getWidth(), source.getHeight(), width, height);
                if (sample <= 1) {
                    attach(source);
                    Log.d(TAG, "loadBitmap: compress");
                } else {
                    RequestManager.getInstance().commit(() -> {
                        Matrix matrix = new Matrix();
                        matrix.setScale(1 / sample, 1 / sample);
                        attach(Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true));
                        source.recycle();
                        Log.d(TAG, "loadBitmap: no compress");
                    });
                }
            });
            determiner.getSize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
