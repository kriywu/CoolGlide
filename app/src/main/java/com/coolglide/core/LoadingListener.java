package com.coolglide.core;

import android.graphics.Bitmap;

/**
 * author : kriywu
 * date : 2019/9/15
 * description : this listener is called while bitmap object created. to cache bitmap
 */
public interface LoadingListener {
    void onSuccess(Bitmap bitmap);
    void onFail();
}
