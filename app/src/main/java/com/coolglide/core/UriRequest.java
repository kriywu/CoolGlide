package com.coolglide.core;

import android.graphics.Bitmap;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.coolglide.core.LoadingListener;

import java.lang.ref.SoftReference;

/**
 * author : kriywu
 * date : 2019/9/15
 * description : this is base request
 */
public abstract class UriRequest implements SizeReadyCallback{
    public String mUri;
    public String mTag;
    public SoftReference<ImageView> mTargetView;
    public LoadingListener mListener;
    private static final String TAG = "UriRequest";

    public UriRequest(String uri){
        this.mUri = uri;
    }

    public abstract void placeholder();

    public abstract void loadBitmap();

    public void attach(Bitmap bitmap) {
        // 1. check bitmap object 
        if(bitmap == null) {
            mListener.onFail();
            Log.d(TAG, "attach: bitmap == null");
            return;
        }
        // 2. check target view
        final ImageView imageView = mTargetView.get();
        if(imageView == null  || imageView.getTag() != mTag) {
            mListener.onFail();
            Log.d(TAG, "attach: view == null");
            return;
        }
        // 3. attach bitmap in main thread
        if(Looper.getMainLooper() == Looper.myLooper()){
            imageView.setImageBitmap(bitmap);
        }else{
            imageView.post(() -> imageView.setImageBitmap(bitmap));
        }
        // 4. cache bitmap
        mListener.onSuccess(bitmap);
    }

    @Override
    public void onSizeReady(int width, int height) {

    }
}
