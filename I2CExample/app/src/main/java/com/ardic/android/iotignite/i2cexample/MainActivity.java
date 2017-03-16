package com.ardic.android.iotignite.i2cexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private IotIgniteHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = IotIgniteHandler.getInstance(getApplicationContext());
        mHandler.start();

    }


    @Override
    protected void onDestroy() {
        mHandler.shutdown();
        super.onDestroy();
    }
}
