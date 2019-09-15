package com.coolglide.core;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * author : kriywu
 * date : 2019/9/15
 * description : this is core manager, contains all apis for users
 */
public class RequestManager {
    private static final String TAG = "RequestManager";
    private static RequestManager manager = null;
    private RequestDispatcher[] dispatchers;
    private LinkedBlockingQueue<UriRequest> requests;
    private int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024);

    private ExecutorService executor;
    
    private LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(cacheSize) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };

    private RequestManager() {
        int count = Runtime.getRuntime().availableProcessors();
        dispatchers = new RequestDispatcher[count];
        requests = new LinkedBlockingQueue<>();
    }

    public void commit(Runnable runnable){
        executor.submit(runnable);
    }

    public static RequestManager getInstance() {
        if (manager == null) {
            synchronized (RequestManager.class) {
                if (manager == null) manager = new RequestManager();
            }
        }
        return manager;
    }

    public void offer(UriRequest request) {
        // 1. set tag
        ImageView imageView = request.mTargetView.get();
        if(request.mTag != null && imageView.getTag() != null && request.mTag != imageView.getTag()) imageView.setImageBitmap(null);
        request.mTag = request.mUri;
        if (imageView != null) imageView.setTag(request.mTag);
        Log.d(TAG, "offer: set tag ok " + request.mUri);

        // 2. if cache has it
        Bitmap bitmap;
        if ((bitmap = cache.get(request.mUri)) != null) {
            request.attach(bitmap);
            Log.d(TAG, "offer: hit cache " + request.mUri);
        }

        // 3. need to load it
        request.mListener = new LoadingListenerProxy(request.mListener) {
            @Override
            public void onSuccess(Bitmap bitmap) {
                cache.put(request.mUri, bitmap);
                super.onSuccess(bitmap);
            }

            @Override
            public void onFail() {
                super.onFail();
            }
        };
        requests.offer(request);
        Log.d(TAG, "offer: loading" + request.mUri);
    }

    /**
     * start all thread
     */
    public void start() {
        for (int i = 0; i < dispatchers.length; i++) {
            dispatchers[i] = new RequestDispatcher(requests);
            dispatchers[i].start();
        }
        executor = Executors.newFixedThreadPool(2);
    }

    /**
     * stop all thread 
     */
    public void stop() {
        for (RequestDispatcher dispatcher : dispatchers) {
            dispatcher.interrupt();
        }
        requests.clear();
        executor.shutdownNow();
    }

    /**
     * proxy Loading listener
     */
    class LoadingListenerProxy implements LoadingListener {
        private LoadingListener listener;

        public LoadingListenerProxy(LoadingListener listener) {
            this.listener = listener;
        }

        @Override
        public void onSuccess(Bitmap bitmap) {
            if (listener != null) listener.onSuccess(bitmap);
        }

        @Override
        public void onFail() {
            if (listener != null) listener.onFail();
        }
    }


}
