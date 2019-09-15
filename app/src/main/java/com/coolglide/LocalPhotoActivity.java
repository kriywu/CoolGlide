package com.coolglide;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.coolglide.core.LoadingListener;
import com.coolglide.core.LocalRequest;
import com.coolglide.core.RequestManager;
import com.coolglide.core.UriRequest;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * author : kriywu
 * date : 2019/9/15
 * description : this is a demo to test local photo file
 */
public class LocalPhotoActivity extends AppCompatActivity {

    private static final String TAG = "LocalPhotoActivity";
    RequestManager glide;
    List<String> list = new ArrayList<>();
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_photo);

        glide = RequestManager.getInstance();
        recyclerView = findViewById(R.id.rv);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = getLayoutInflater().inflate(R.layout.item_image, parent, false);
                return new WebPhotoActivity.ViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                ImageView iv = holder.itemView.findViewById(R.id.iv);
                UriRequest request = new LocalRequest(list.get(position)); // url
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
            public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
                ImageView iv = holder.itemView.findViewById(R.id.iv);
                iv.setImageBitmap(null);
            }

            @Override
            public int getItemCount() {
                Log.d(TAG, "getItemCount: " + list.size());
                return list.size();
            }
        });
        glide.start();
        checkPermissionForReadStorage();

    }

    class QueryDBTask extends AsyncTask<Void, Void, Void> {
        ContentResolver contentResolver;
        List<String> list = null;

        QueryDBTask(ContentResolver resolver, List<String> list) {
            contentResolver = resolver;
            this.list = list;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Cursor cursor = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Images.Media.DATE_MODIFIED + " desc"
            );
            if (cursor == null) return null;

            int index = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
            while (cursor.moveToNext()) list.add(cursor.getString(index));
            cursor.close();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
        }
    }

    public void checkPermissionForReadStorage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE}, 1);
        } else {
            new QueryDBTask(getContentResolver(), list).execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    new QueryDBTask(getContentResolver(), list).execute();
                } else {
                    Toast.makeText(LocalPhotoActivity.this, "你没有授权", Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        glide.stop();
    }
}
