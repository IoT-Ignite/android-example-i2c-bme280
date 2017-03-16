package com.ardic.android.iotignite.i2cexample;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.ardic.android.iotignite.callbacks.ConnectionCallback;
import com.ardic.android.iotignite.enumerations.NodeType;
import com.ardic.android.iotignite.enumerations.ThingCategory;
import com.ardic.android.iotignite.enumerations.ThingDataType;
import com.ardic.android.iotignite.exceptions.UnsupportedVersionException;
import com.ardic.android.iotignite.listeners.NodeListener;
import com.ardic.android.iotignite.listeners.ThingListener;
import com.ardic.android.iotignite.nodes.IotIgniteManager;
import com.ardic.android.iotignite.nodes.Node;
import com.ardic.android.iotignite.things.Thing;
import com.ardic.android.iotignite.things.ThingActionData;
import com.ardic.android.iotignite.things.ThingData;
import com.ardic.android.iotignite.things.ThingType;

import java.io.IOException;

/**
 * Created by yavuz.erzurumlu on 3/15/17.
 */

public class IotIgniteHandler implements ConnectionCallback, NodeListener, ThingListener {

    private static final String TAG = IotIgniteHandler.class.getSimpleName();

    private static final String ON_DESTROY_MSG = "Application Destroyed";

    // Static singleton instance
    private static IotIgniteHandler INSTANCE = null;
    private static final long IGNITE_RECONNECT_INTERVAL = 10000L;

    private BME280Handler mBMEHandler;

    private IotIgniteManager mIotIgniteManager;
    private boolean igniteConnected = false;
    private Context appContext;
    private Handler igniteWatchdog = new Handler();

    private long pressureReadFreq = -1L;
    private long tempReadFreq = -1L;

    private Runnable igniteWatchdogRunnable = new Runnable() {
        @Override
        public void run() {

            if (!igniteConnected) {
                rebuildIgnite();
                igniteWatchdog.postDelayed(this, IGNITE_RECONNECT_INTERVAL);
                Log.e(TAG, "Ignite is not connected trying to reconnect...");
            } else {
                Log.e(TAG, "Ignite is already connected");
            }
        }
    };

    private Handler mDataHandler = new Handler();


    private Runnable sendPressureDataRunnable = new Runnable() {
        @Override
        public void run() {

            ThingData data = new ThingData();
            data.addData(mBMEHandler.readPressure());
            mPressureThing.setThingData(data);
            mPressureThing.sendData(data);

            Log.i(TAG,"Sending Pressure Data : " + data.getDataList() + " FREQ : " + pressureReadFreq);

            if(pressureReadFreq > -1L) {
                mDataHandler.postDelayed(this, pressureReadFreq);
            }
        }
    };


    private Runnable sendTempDataRunnable = new Runnable() {
        @Override
        public void run() {

            ThingData data = new ThingData();
            data.addData(mBMEHandler.readTemperature());
            mTempThing.setThingData(data);
            mTempThing.sendData(data);

            Log.i(TAG,"Sending Temp Data : " + data.getDataList() +" FREQ : " + tempReadFreq);

            if(tempReadFreq > -1L) {
                mDataHandler.postDelayed(this, tempReadFreq);
            }
        }
    };


    /**
     * Android Things based things and nodes.
     */

    private static final String ANDROID_THINGS_NODE_ID = "Android Things Node";
    private static final String TEMP_THING_ID = "BME280 TEMPERATURE";
    private static final String PRESSURE_THING_ID = "BME280 PRESSURE";

    private Node androidThingsNode;
    private Thing mTempThing;
    private Thing mPressureThing;
    private ThingType mTempThingType = new ThingType("BME Temperature Sensor", "Bosch", ThingDataType.FLOAT);
    private ThingType mPressureThingType = new ThingType("BME Pressure Sensor", "Bosch", ThingDataType.FLOAT);


    private IotIgniteHandler(Context context) {
        this.appContext = context;
    }

    public static synchronized IotIgniteHandler getInstance(Context appContext) {

        if (INSTANCE == null) {
            INSTANCE = new IotIgniteHandler(appContext);
        }
        return INSTANCE;

    }


    public void start() {
        startIgniteWatchdog();
    }

    @Override
    public void onConnected() {
        Log.i(TAG, "Ignite Connected");
        // cancel watchdog //
        igniteWatchdog.removeCallbacks(igniteWatchdogRunnable);
        igniteConnected = true;

        mBMEHandler = BME280Handler.getInstance(appContext);
        mBMEHandler.start();


        androidThingsNode = IotIgniteManager.NodeFactory.createNode(
                ANDROID_THINGS_NODE_ID,
                ANDROID_THINGS_NODE_ID,
                NodeType.GENERIC,
                null,
                this
        );

        registerNodeAndSetConnectionOnline(androidThingsNode);


        if (androidThingsNode != null && androidThingsNode.isRegistered()) {

            mTempThing = androidThingsNode.createThing(
                    TEMP_THING_ID,
                    mTempThingType,
                    ThingCategory.BUILTIN,
                    false,
                    this,
                    null);

            registerThingAndSetConnectionOnline(androidThingsNode, mTempThing);


            mPressureThing = androidThingsNode.createThing(
                    PRESSURE_THING_ID,
                    mPressureThingType,
                    ThingCategory.BUILTIN,
                    false,
                    this,
                    null);

            registerThingAndSetConnectionOnline(androidThingsNode, mPressureThing);

        }

        checkConfigsAndStart();

    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "Ignite Disconnected");
        // start watchdog again here.
        igniteConnected = false;
        startIgniteWatchdog();
    }

    /**
     * Connect to iot ignite
     */

    private void rebuildIgnite() {
        try {
            mIotIgniteManager = new IotIgniteManager.Builder()
                    .setConnectionListener(this)
                    .setContext(appContext)
                    .build();
        } catch (UnsupportedVersionException e) {
            Log.e(TAG, "UnsupportedVersionException : " + e);
        }
    }

    /**
     * remove previous callback and setup new watchdog
     */

    private void startIgniteWatchdog() {
        igniteWatchdog.removeCallbacks(igniteWatchdogRunnable);
        igniteWatchdog.postDelayed(igniteWatchdogRunnable, IGNITE_RECONNECT_INTERVAL);

    }

    @Override
    public void onNodeUnregistered(String s) {

    }

    /**
     * Set all things and nodes connection to offline.
     * When the application close or destroyed.
     */


    public void shutdown() {

        setNodeConnection(androidThingsNode, false, ON_DESTROY_MSG);
        setThingConnection(mTempThing, false, ON_DESTROY_MSG);
        setThingConnection(mPressureThing, false, ON_DESTROY_MSG);
        mBMEHandler.stop();


    }

    @Override
    public void onConfigurationReceived(Thing thing) {

        /**
         * Thing configuration messages will be handled here.
         * For example data reading frequency or custom configuration may be in the incoming thing object.
         */

        if (PRESSURE_THING_ID.equals(thing.getThingID())) {




            pressureReadFreq = thing.getThingConfiguration().getDataReadingFrequency();

            mDataHandler.removeCallbacks(sendPressureDataRunnable);
            Log.i(TAG,"Config Received For : " + PRESSURE_THING_ID + " FREQ :" + pressureReadFreq);

            pressureReadFreq = pressureReadFreq == 0L ? 1000L : pressureReadFreq;

            mDataHandler.post(sendPressureDataRunnable);

        } else if (TEMP_THING_ID.equals(thing.getThingID())) {



            tempReadFreq = thing.getThingConfiguration().getDataReadingFrequency();

            Log.i(TAG,"Config Received For : " + TEMP_THING_ID  + " FREQ : " + tempReadFreq);
            mDataHandler.removeCallbacks(sendTempDataRunnable);

            tempReadFreq = tempReadFreq == 0L ? 1000L : tempReadFreq;

            mDataHandler.post(sendTempDataRunnable);

        }

    }

    @Override
    public void onActionReceived(String s, String s1, ThingActionData thingActionData) {

        /**
         * Thing action message will be handled here. Call thingActionData.getMessage()
         */

    }

    @Override
    public void onThingUnregistered(String s, String s1) {

        /**
         * If your thing object unregistered from outside world you will receive this
         * information callback.
         */
    }

    private void registerNodeAndSetConnectionOnline(Node mNode) {

        if (mNode != null) {
            if (mNode.isRegistered() || mNode.register()) {
                mNode.setConnected(true, "");
            }
        }
    }

    private void registerThingAndSetConnectionOnline(Node mNode, Thing mThing) {

        if (mNode != null && mNode.isRegistered() && mThing != null) {

            if (mThing.isRegistered() || mThing.register()) {
                mThing.setConnected(true, "");
            }
        }
    }

    private void setThingConnection(Thing mThing, boolean state, String explanation) {
        if (mThing != null) {
            mThing.setConnected(state, explanation);
        }

    }

    private void setNodeConnection(Node mNode, boolean state, String explanation) {
        if (mNode != null) {
            mNode.setConnected(state, explanation);
        }
    }


    public IotIgniteManager getIgniteManager() {
        return this.mIotIgniteManager;
    }

    private void checkConfigsAndStart() {

        pressureReadFreq = mPressureThing.getThingConfiguration().getDataReadingFrequency();
        tempReadFreq = mTempThing.getThingConfiguration().getDataReadingFrequency();

        pressureReadFreq = pressureReadFreq == 0L ? 1000L : pressureReadFreq;
        tempReadFreq = tempReadFreq == 0L ? 1000L : tempReadFreq;


        Log.i(TAG,"Config For : " + PRESSURE_THING_ID + " FREQ :" + pressureReadFreq);
        Log.i(TAG,"Config For : " + TEMP_THING_ID + " FREQ :" + tempReadFreq);

        mDataHandler.removeCallbacks(sendPressureDataRunnable);
        mDataHandler.post(sendPressureDataRunnable);
        mDataHandler.removeCallbacks(sendTempDataRunnable);
        mDataHandler.post(sendTempDataRunnable);

    }

}