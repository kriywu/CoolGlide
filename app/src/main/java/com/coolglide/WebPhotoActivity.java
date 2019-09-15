package com.coolglide;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coolglide.core.LoadingListener;
import com.coolglide.core.RequestManager;
import com.coolglide.core.WebRequest;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

/**
 * author : kriywu
 * date : 2019/9/15
 * description : this a demo to test web photo
 */
public class WebPhotoActivity extends AppCompatActivity {
    private static final String TAG = "WebPhotoActivity";
    RequestManager glide;
    ArrayList<String> urls = new ArrayList<>();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_photo);

        urls.add("https://img.mp.itc.cn/upload/20161204/a279db0f7abe44e5b5ed623182136dc8_th.jpg");
        urls.add("https://img.mp.itc.cn/upload/20161204/4266281024384fa388f5e78be4124d32_th.jpg");
        urls.add("https://img.mp.itc.cn/upload/20161204/b3027a404bdd4866a95f9e9db8d4d5d2_th.jpg");
        urls.add("https://img.mp.itc.cn/upload/20161204/1b49d719f76641789f4ef84c5935b491_th.jpg");
        urls.add("https://img.mp.itc.cn/upload/20161204/a0e4b39fff1f4b7fb5f8b26bf779a2cb_th.jpg");
        urls.add("https://img.mp.itc.cn/upload/20161204/89339456292b4a8a8fe1e35455708237_th.jpg");
        urls.add("https://img.mp.itc.cn/upload/20161204/3a0d9f6a98d744998689afb07390c8e6_th.jpg");
        urls.add("https://img.mp.itc.cn/upload/20161204/886816666887442ab54fb44667d20cfa_th.jpg");
        urls.add("https://img.mp.itc.cn/upload/20161204/fddd5710b7214a4a85b458a80d95c349_th.jpg");
        urls.add("https://img.mp.itc.cn/upload/20161204/5f6a9a67a2734f7a823d564e4cfc2ce0_th.jpg");

        glide = RequestManager.getInstance();
        RecyclerView rv = findViewById(R.id.rv);
        rv.setLayoutManager(new GridLayoutManager(this, 1));
        rv.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                //View view = View.inflate(WebPhotoActivity.this, R.layout.item_image, parent);
                View view = getLayoutInflater().inflate(R.layout.item_image,parent, false);
                return new ViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                ImageView iv = holder.itemView.findViewById(R.id.iv);
                WebRequest request = new WebRequest(urls.get(position));
                request.mTargetView = new SoftReference<>(iv);
                request.mListener = new LoadingListener() {
                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        Log.d(TAG, "onSuccess: success , position = " + position);

                    }

                    @Override
                    public void onFail() {
                        Log.d(TAG, "onFail: , position = " + position);
                    }
                };
                glide.offer(request);
            }

            @Override
            public int getItemCount() {
                return urls.size();
            }
        });
        glide.start();
    }
    static class ViewHolder extends RecyclerView.ViewHolder{

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        glide.stop();
    }
}
