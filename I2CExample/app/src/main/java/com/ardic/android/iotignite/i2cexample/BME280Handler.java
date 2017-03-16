package com.ardic.android.iotignite.i2cexample;

import android.content.Context;
import android.util.Log;

import com.google.android.things.contrib.driver.bmx280.Bmx280;
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver;

import java.io.IOException;

/**
 * Created by yavuz.erzurumlu on 3/15/17.
 */

public class BME280Handler {
    private static final String TAG = BME280Handler.class.getSimpleName();

    /**
     * Raspberry Pi I2C Bus Name
     */
    private static final String BME_I2C_BUS = "I2C1";
    private Bmx280 mBmx280;
    private Bmx280SensorDriver mBmx280SensorDriver;
    private Context appContext;
    private float mTemp, mPress;

    private static BME280Handler INSTANCE;


    private BME280Handler(Context appContext) {
        this.appContext = appContext;
        mTemp = 0;
        mPress = 0;

    }

    public static BME280Handler getInstance(Context appContext) {

        if (INSTANCE == null) {
            INSTANCE = new BME280Handler(appContext);
        }
        return INSTANCE;
    }


    public void start()  {
        try {
            mBmx280 = new Bmx280(BME_I2C_BUS);
            Log.i(TAG,"BME Chip ID : " + mBmx280.getChipId());
            mBmx280.setTemperatureOversampling(Bmx280.OVERSAMPLING_1X);
            mBmx280.setPressureOversampling(Bmx280.OVERSAMPLING_1X);
            mBmx280.setMode(Bmx280.MODE_NORMAL);
        } catch (IOException e) {
           Log.e(TAG,"IOException :" + e);
        }
    }


    public float readTemperature() {
        try {
            if(mBmx280 != null) {
                mTemp = mBmx280.readTemperature();
            }
        } catch (IOException e) {
            Log.e(TAG,"IOException :" + e);
        }
        return mTemp;
    }

    public float readPressure() {
        try {
            if(mBmx280 != null) {
                mPress = mBmx280.readPressure();
            }
        } catch (IOException e) {
            Log.e(TAG,"IOException :" + e);
        }
        return mPress;
    }


    public void stop(){

        if(mBmx280 != null) {
            try {
                mBmx280.close();

            } catch (IOException e) {
                Log.e(TAG, "IOException :" + e);
            } finally {
                mBmx280 = null;
            }
        }
    }
}
