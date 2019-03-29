package com.oliver.a5bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

public class BLEService extends Service {
    private final IBinder mBinder = new LocalBinder();
    private BLEManager bleManagerInstance;
    private IsometricListener isometricListener;
    private BLEValuesCallback bleValuesCallback;
    private Timer testTimer;
    private boolean isLb;
    private ConnectServiceCallback connectServiceCallback;

    @Override
    public void onCreate() {
        super.onCreate();

            bleManagerInstance = BLEManager.getInstance(getApplicationContext(), activ5DeviceListener);
            bleManagerInstance.setListener(activ5DeviceListener);

    }

    private final Activ5DeviceListener activ5DeviceListener = new Activ5DeviceListener() {
        @Override
        public void scanningStarted() {
        }

        @Override
        public void deviceFound(BluetoothDevice device) {
        }

        @Override
        public void scanningStopped() {
        }

        @Override
        public void onDeviceConnected(boolean isConnected) {
            LogUtils.i("connected in service - " + isConnected);

                bleManagerInstance.isConnected = isConnected;

            if (connectServiceCallback != null) {
                connectServiceCallback.onConnected(isConnected);
            }

            if (!isConnected) {
                stopReading();
            }
        }

        @Override
        public void on133Error() {
            LogUtils.i("133 error received in service");
            if (connectServiceCallback != null) {
                connectServiceCallback.on133Error();
            }
        }
    };

    public boolean isConnected() {
            return bleManagerInstance != null && bleManagerInstance.isConnected;
    }

    public void clearBleManagerInstance() {
        if (bleManagerInstance != null) {
            bleManagerInstance.disconnect();
            bleManagerInstance.deleteInstance();
            bleManagerInstance = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        LogUtils.i("ble service on destroy called");
        this.bleValuesCallback = null;
        if (isometricListener != null) {
            isometricListener.stop();
        }

        /*
         * simulation purpose only
         */
        if (bleManagerInstance != null) {
                bleManagerInstance.disconnect();
                bleManagerInstance.deleteInstance();
                bleManagerInstance = null;
            }

        super.onDestroy();
    }

    public void startReading(BLEValuesCallback bleValuesCallback) {
        this.bleValuesCallback = bleValuesCallback;
//        isLb = AppPreferences.getInstance().getPreference(AppConstants.CRR_UNIT, "lb").equalsIgnoreCase("lb");
        if (isometricListener != null) {
            isometricListener.stop();
            isometricListener = null;
        }
        /*
         * simulation purpose only
         */
        if (bleManagerInstance != null) {
                bleManagerInstance.startIsometricExecution();
            }

        isometricListener = new IsometricListener();
        isometricListener.start();
    }

    public void stopReading() {
        this.bleValuesCallback = null;
        if (isometricListener != null) {
            isometricListener.stop();
        }

        if (bleManagerInstance != null) {
            bleManagerInstance.sendStopCommand();
        }
    }

    public void setConnectionCallbacksListener(ConnectServiceCallback connectServiceCallback) {
        this.connectServiceCallback = connectServiceCallback;
        if (bleManagerInstance != null) {
            bleManagerInstance.setListener(activ5DeviceListener);
        }
    }

    public void connectRememberedDevice(ConnectServiceCallback connectServiceCallback) {
        this.connectServiceCallback = connectServiceCallback;

        if (bleManagerInstance != null) {
            bleManagerInstance.disconnect();
            bleManagerInstance.deleteInstance();
            bleManagerInstance = null;
        }
        bleManagerInstance = BLEManager.getInstance(getApplicationContext(), activ5DeviceListener);
        bleManagerInstance.setListener(activ5DeviceListener);
//        bleManagerInstance.connect(AppPreferences.getInstance().getPreference(AppConstants.REMEMBERED_DEVICE, ""),
//                getApplicationContext());
    }

    public interface BLEValuesCallback {
        void updateValue(float newPressureValue, float newtonValue);
    }

    public interface ConnectServiceCallback {
        void onConnected(boolean isConnected);

        void on133Error();
    }

    public class LocalBinder extends Binder {
        public BLEService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BLEService.this;
        }
    }

    public static final int READ_INTERVAL = 50;

    private static float g = 9.80665002864f;
    private static float lb = 2.20462262185f;

    private class IsometricListener {
        private IsometricListener() {
           long timePassed = System.currentTimeMillis();
        }

        public void start() {
            testTimer = new Timer();
            testTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {

                    if (bleValuesCallback != null) {
                        int pressureData = 0;

                        if (bleManagerInstance != null) {
                            pressureData = bleManagerInstance.getPressureData();
                        }
                        float value;
                        if (isLb) {
                            value = lb * (pressureData / g);
                        } else {
                            value = pressureData / g;
                        }

                        bleValuesCallback.updateValue(value, pressureData);
                        //LogUtils.i("pressure value IsoMetriccListener===" + value);
                    }

                    //pressureValue = 0;
                  long  timePassed = System.currentTimeMillis();
                    // Singleton.getInstance().bleDeviceManager.restPressureValue();
                }

            }, 0, READ_INTERVAL);
        }

        public void stop() {
            if (testTimer != null) {
                testTimer.cancel();
            }
        }
    }
}