package com.ylx.zbardemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.ylx.zbardemo.activity.ZxingActivity;

public class MainActivity extends AppCompatActivity {

    private TextView mZxingBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mZxingBtn = (TextView) findViewById(R.id.zxing_btn);
        initListener();
    }

    private void initListener() {
        mZxingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ZxingActivity.class));
            }
        });
    }
}
