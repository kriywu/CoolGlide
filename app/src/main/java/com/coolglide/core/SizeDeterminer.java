package com.coolglide.core;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Point;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.core.util.Preconditions;

import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * author : kriywu
 * date : 2019/9/15
 * description : to get normal size of view, protecting form OOM
 */
public class SizeDeterminer {
    private static final String TAG = "SizeDeterminer";
    private final static int PENDING_SIZE = 0;
    private static int MAX_SIZE = -1;
    private static int MAX_CACHE = Runtime.getRuntime().availableProcessors() + 1; // process num + 1
    private final static int SIZE_ORIGINAL = Integer.MIN_VALUE;
    private View mView;
    private SizeReadyCallback callback;
    private volatile static BlockingQueue<SizeDeterminer> determiners = new LinkedBlockingQueue<>(MAX_CACHE);

    // 享元模式
    public static SizeDeterminer Obtains(View view, SizeReadyCallback callback) {
        if (determiners.size() > 0) {
            try {
                SizeDeterminer sizeDeterminer = determiners.take();
                sizeDeterminer.mView = view;
                sizeDeterminer.callback = callback;
                Log.d(TAG, "Obtains: obtains !");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.d(TAG, "Obtains: InterruptedException");
                return null;
            }
        }
        return new SizeDeterminer(view, callback);
    }

    public SizeDeterminer(View view, SizeReadyCallback callback) {
        this.mView = view;
        this.callback = callback;
    }


    public void getSize() {
        int currentWidth = getTargetWidth();
        int currentHeight = getTargetHeight();

        // 1. is valid  ( when w and h > 0)
        if (isViewStateAndSizeValid(currentWidth, currentHeight)) {
            callback.onSizeReady(currentWidth, currentHeight);
            Log.d(TAG, "getSize: valid");
            return;
        }

        // 2. invalid ( w and h <= 0)
        ViewTreeObserver observer = mView.getViewTreeObserver();
        Log.d(TAG, "getSize: invalid");
        // before drawing view, call SizeReady listener
        observer.addOnPreDrawListener(new SizeDeterminerLayoutListener(this));

    }

    private boolean isViewStateAndSizeValid(int currentWidth, int currentHeight) {
        return (currentWidth > 0 || currentWidth == SIZE_ORIGINAL) && (currentHeight > 0 || currentHeight == SIZE_ORIGINAL);
    }

    private int getTargetHeight() {
        int padding = mView.getPaddingTop() + mView.getPaddingBottom();
        ViewGroup.LayoutParams params = mView.getLayoutParams();
        int paramsSize = params == null ? PENDING_SIZE : params.height;
        return getTargetDimen(mView.getHeight(), paramsSize, padding);
    }

    private int getTargetWidth() {
        int padding = mView.getPaddingLeft() + mView.getPaddingRight();
        ViewGroup.LayoutParams params = mView.getLayoutParams();
        int paramsSize = params == null ? PENDING_SIZE : params.width;
        return getTargetDimen(mView.getWidth(), paramsSize, padding);
    }

    private int getTargetDimen(int viewSize, int paramsSize, int padding) {
        int adjustedParamsSize = paramsSize - padding;
        if (adjustedParamsSize > 0) return adjustedParamsSize;

        // view 还在measure, 此时viewSize 和 paramsSize 都不可信
        if (mView.isLayoutRequested()) {
            return PENDING_SIZE;
        }

        // 通过viewSize 来计算区域尺寸
        int adjustedViewSize = viewSize - padding;
        if (adjustedViewSize > 0) return adjustedViewSize;

        if (!mView.isLayoutRequested() && paramsSize == ActionBar.LayoutParams.WRAP_CONTENT)
            return getMaxDisplayLength(mView.getContext());

        return PENDING_SIZE;
    }

    // calculate the display size
    private static int getMaxDisplayLength(Context context) {
        if (MAX_SIZE > 0) return MAX_SIZE;
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = Preconditions.checkNotNull(windowManager).getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        MAX_SIZE = Math.max(point.x, point.y);
        return MAX_SIZE;
    }

    private static final class SizeDeterminerLayoutListener implements ViewTreeObserver.OnPreDrawListener {
        private final WeakReference<SizeDeterminer> sizeDeterminerRef;

        SizeDeterminerLayoutListener(SizeDeterminer sizeDeterminer) {
            sizeDeterminerRef = new WeakReference<>(sizeDeterminer);
        }

        @Override
        public boolean onPreDraw() {
            SizeDeterminer sizeDeterminer = sizeDeterminerRef.get();
            if (sizeDeterminer != null) {
                sizeDeterminer.checkCurrentDimens(this);
            }

            return true;
        }

    }

    private void checkCurrentDimens(SizeDeterminerLayoutListener self) {
        int currentWidth = getTargetWidth();
        int currentHeight = getTargetHeight();

        if (!isViewStateAndSizeValid(currentWidth, currentHeight)) {
            Log.d(TAG, "checkCurrentDimens: size is invalid");
            return;
        }
        ViewTreeObserver observer = mView.getViewTreeObserver();
        observer.removeOnPreDrawListener(self);
        free();

        RequestManager.getInstance().commit(() -> {
            callback.onSizeReady(currentWidth, currentHeight);

            Log.d(TAG, "checkCurrentDimens: " + (Looper.getMainLooper() == Looper.myLooper()));

        });
    }

    private void free() {
        Log.d(TAG, "free: size = " + determiners.size());
        if (determiners.size() <= MAX_CACHE) determiners.offer(this);
    }
}
