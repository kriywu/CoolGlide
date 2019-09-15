package com.coolglide.core;

import android.graphics.Bitmap;

import com.coolglide.core.UriRequest;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * author : kriywu
 * date : 2019/9/15
 * description : this is a thread to loading bitmap , decoding and so on ...
 */
public class RequestDispatcher extends Thread {
    private static final String TAG = "RequestDispatcher";
    private LinkedBlockingQueue<UriRequest> requests = null;

    public RequestDispatcher(LinkedBlockingQueue<UriRequest> requests){
        this.requests = requests;
    }

    @Override
    public void run() {
        while (true){
            if(interrupted()) return;
            UriRequest request = null;

            try {
                request = requests.take();
                request.placeholder();
                request.loadBitmap();
                
            } catch (Exception e) {
                e.printStackTrace();
                if(request != null) request.mListener.onFail();
            }

        }
    }

}
