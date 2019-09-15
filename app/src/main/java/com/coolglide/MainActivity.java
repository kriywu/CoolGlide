package com.coolglide;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * author : kriywu
 * date : 2019/9/15
 * description : this lists all test
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_local).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LocalPhotoActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_web).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, WebPhotoActivity.class);
            startActivity(intent);
        });
    }
}
