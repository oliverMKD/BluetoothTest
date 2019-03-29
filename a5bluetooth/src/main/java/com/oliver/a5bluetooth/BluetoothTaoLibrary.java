package com.oliver.a5bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BluetoothTaoLibrary implements BluetoothTaoCallbacks,BluetoothTaoScanCallback {
    public static final ParcelUuid SERVICE_UUID = ParcelUuid.fromString("00005000-0000-1000-8000-00805f9b34fb");
    private final String TAG = "Activ5";
    private static HashMap<String, TAODevice> mFoundTaoDevicesMap;
    private static HashMap<String, TAODevice> mConnectedTaoDevicesMap;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private LocalBroadcastManager mLocalBroadcastManager;
    private Activity mActivity;
    private Handler mTimeHandler;
    private boolean mDesireScanning;
    private BluetoothTaoScanner mScanner;

    public void addDevice(String name, String address,int rssi, BluetoothDevice device) {
        if (isTAODeviceName(name)) {
            TAODevice taoDevice = new TAODevice(name, address, rssi, device);
            onDeviceFound(taoDevice);
        }
    }

    private Runnable mStartRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("Activ5", "mStartRunnable scan again");
            startScanning();
        }
    };

    private Runnable mStopRunnable = new Runnable() {

        @Override
        public void run() {
            stopScanning();
        }

    };

    private boolean isTAODeviceName(String name)
    {
        return (name != null) && ((name.contains("TAO")) || (name.contains("ACTIV5")));
    }

    public void initTaoLibrary(Activity activity)
    {
        checkthread();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(activity,android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, /*app defined int*/1);
            }
        }
        Log.d("Activ5","activity " + activity + " getapp " + activity.getApplicationContext());
        this.mActivity = activity;
        Log.d("Activ5","context " + this.mActivity + " getapp " + this.mActivity.getApplicationContext());
        this.mBluetoothManager = ((BluetoothManager)this.mActivity.getSystemService(Context.BLUETOOTH_SERVICE));
        this.mBluetoothAdapter = this.mBluetoothManager.getAdapter();
        this.mLocalBroadcastManager = LocalBroadcastManager.getInstance(this.mActivity);
        this.mTimeHandler = new Handler(this.mActivity.getMainLooper());
        mConnectedTaoDevicesMap = new HashMap();

        if (isLollipopOrHigher()) {
            mScanner = new BluetoothTaoNewScanner();
        }
        else {
            mScanner = new BluetoothTaoOldScanner();
        }
    }

    public synchronized boolean getSearching() {
        return this.mDesireScanning;
    }

    public synchronized void startDiscovery()
    {
        if (!(this.mScanning  && this.mDesireScanning)) {
            disconnectAllDevice();
            mFoundTaoDevicesMap = new HashMap();

            this.mDesireScanning = true;
            startScanning();
        }
    }


    public synchronized void stopDiscovery()
    {
        this.mDesireScanning = false;
        stopScanningForReal();
    }

    public synchronized void stopScanningForReal() {
        this.mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if (mBluetoothAdapter != null && mScanning) {
                    mScanner.stopScan(mBluetoothAdapter);
                }
                mScanning = false;
            }
        });
        mTimeHandler.removeCallbacks(mStopRunnable);
        mTimeHandler.removeCallbacks(mStartRunnable);
    }

    private boolean isLollipopOrHigher() {
        return Build.VERSION.SDK_INT >= 21;
    }


    public void startScanning() {
        this.mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if (mBluetoothAdapter != null && !mScanning) {
                    mScanning = true;
                    mScanner.startScan(mBluetoothAdapter, BluetoothTaoLibrary.this);
                    mTimeHandler.postDelayed(mStopRunnable, 5000);
                }
            }
        });
    }

    public void stopScanning() {
        stopScanningForReal();
        if(this.mDesireScanning) {
            mTimeHandler.postDelayed(mStartRunnable, 1000);
        }
    }

    public int pressure(String address)
    {
        TAODevice taoDeviceInt = (TAODevice)mConnectedTaoDevicesMap.get(address);
        if (taoDeviceInt != null) {
            return taoDeviceInt.pressure();
        }

        return 0;
    }

    public int count = 0;

    public void pressureChange(String address, int value)
    {
        if((count++ & 0xF) == 0) {
            Log.d("Activ5", "pressureChange " + value);
        }
        TAODevice taoDeviceInt = (TAODevice)mConnectedTaoDevicesMap.get(address);
        if (taoDeviceInt != null)
            taoDeviceInt.setPressure(value);
    }

    public void connected(String address)
    {
        Log.d("Activ5", "connect callback " + address);
        TAODevice taoDeviceInt = (TAODevice)mConnectedTaoDevicesMap.get(address);
        if (taoDeviceInt != null)
            broadcast("TAO_DEVICES_CONNECTED_ACTION", taoDeviceInt);
    }

    public void disconnected(String address)
    {
        Log.d("Activ5", "disconnect callback " + address);
        TAODevice taoDeviceInt = (TAODevice)mFoundTaoDevicesMap.get(address);
        if (taoDeviceInt != null) {
            closegatt(taoDeviceInt);
            broadcast("TAO_DEVICES_DISCONNECTED_ACTION", taoDeviceInt);
        }
    }

    // WE call this after disconnect to make sure disconnect callback try does not exception
    private void closegatt(TAODevice taoDeviceInt) {
        if (taoDeviceInt != null) {
            final BluetoothGatt bluetoothGatt = taoDeviceInt.getBluetoothGatt();
            taoDeviceInt.setBluetoothGatt(null);
            if (bluetoothGatt != null) {
                Log.d("Activ5", "calling gatt close " + taoDeviceInt.getAddress());
                this.mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        bluetoothGatt.close();
                    }
                });
            }

        }
    }


    // 133 is a connection error.
    public void connectionfailed(final String address, final BluetoothGatt gatt, int status)
    {
        Log.d("Activ5", "failedconnect callback " + address + " status " + status);
        final TAODevice taoDeviceInt = (TAODevice)mFoundTaoDevicesMap.get(address);
        if (taoDeviceInt != null) {
            closegatt(taoDeviceInt);
            int CONNECTRETRIES = 2;
            int rc = taoDeviceInt.getRetrycount();
            Log.d("Activ5", " retrycount is " + rc);
            if (rc > CONNECTRETRIES || status != 133) {
                broadcast("TAO_DEVICES_DISCONNECTED_ACTION", taoDeviceInt);
            } else {
                taoDeviceInt.setRetrycount(++rc);
                Log.d("Activ5", "reconnectDevice " + taoDeviceInt.getAddress());
                mTimeHandler.postDelayed(new Runnable() {
                    public void run() {
                        connectDevice(address,true);
                    }
                },rc*750);
            }
        }
    }

    public void writecomplete(String address)
    {
        Log.d("Activ5", "writecomplete callback " + address);
    }

    public void firstmessage(String address)
    {
        Log.d("Activ5", "firstmessage callback " + address);
        TAODevice taoDeviceInt = (TAODevice)mConnectedTaoDevicesMap.get(address);
        if (taoDeviceInt != null)
            broadcast("FIRST_MESSAGE_RECEIVED_ACTION", taoDeviceInt);
    }

    public synchronized HashMap<String, Boolean> getSupportedPeripherals(String address)
    {
        TAODevice taoDeviceInt = (TAODevice)mConnectedTaoDevicesMap.get(address);
        if (taoDeviceInt != null) {
            return taoDeviceInt.getSupportedPeripherals();
        }
        return null;
    }

    public boolean checkthread() {
        Log.d("Activ5", "Looper " + Looper.getMainLooper().getThread() + " current " + Thread.currentThread());
        if(Looper.getMainLooper().getThread() != Thread.currentThread()) {
            Log.d("Activ5", "  ******* Wrong thread error");
            return false;
        }
        return true;
    }

    public synchronized boolean connectDevice(final String address,final boolean retry)
    {
        final TAODevice taoDeviceInt = (TAODevice)mFoundTaoDevicesMap.get(address);

        if (taoDeviceInt != null) {
            final BluetoothGatt bluetoothGatt = taoDeviceInt.getBluetoothGatt();
            if(bluetoothGatt == null) {
                // stopScanningForReal();


                final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

                if (device == null) {
                    Log.d("Activ5", "Device not found.  Unable to connect " + taoDeviceInt.getAddress());
                    broadcast("TAO_DEVICES_DISCONNECTED_ACTION", taoDeviceInt);
                    return false;
                }

                this.mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d("Activ5", "New connectGatt in post" + taoDeviceInt.getAddress());
                        taoDeviceInt.setDisconnect(false);
                        if (!retry) {
                            taoDeviceInt.setRetrycount(0);
                        }
                        BluetoothTaoGattCallback callback = new BluetoothTaoGattCallback(BluetoothTaoLibrary.this);
                        checkthread();

                        // I don't think this helps. Would require runtime version handling for older apis
                        //BluetoothGatt btg = device.connectGatt(BluetoothTaoLibrary.this.mActivity, false, callback, BluetoothDevice.TRANSPORT_LE);
                        BluetoothGatt btg = device.connectGatt(BluetoothTaoLibrary.this.mActivity, false, callback);
                        taoDeviceInt.setBluetoothGatt(btg);
                        taoDeviceInt.setCallback(callback);
                        mConnectedTaoDevicesMap.put(address, taoDeviceInt);
                        // Requiring a scanning pass after connectGatt appears to help 133. Questionable greg@activbody.com
                        //mTimeHandler.postDelayed(mStartRunnable, 1000);
                    }
                });
                return true;
            }
        }

        return false;
    }


    public synchronized void disconnectDevice(String address)
    {
        Log.d("Activ5", "disconnectDevice" + address);
        final TAODevice taoDeviceInt = (TAODevice)mConnectedTaoDevicesMap.get(address);
        if(taoDeviceInt != null) {
            mConnectedTaoDevicesMap.remove(address);
            final BluetoothGatt bluetoothGatt = taoDeviceInt.getBluetoothGatt();
            this.mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    //taoDeviceInt.setDisconnect(true);
                    //taoDeviceInt.sendStop();
                    if(bluetoothGatt != null) {
                        bluetoothGatt.disconnect();
                    }
                }
            });
        }
    }

    public synchronized void disconnectAllDevice() {
        Iterator it = mConnectedTaoDevicesMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            TAODevice taoDeviceInt = (TAODevice)pair.getValue();
            disconnectDevice(taoDeviceInt.getAddress());
        }
    }


    // Undocumented call to clear out cache of services and force discovery.
    private boolean refreshDeviceCache(BluetoothGatt gatt)
    {
        try
        {
            final BluetoothGatt localBluetoothGatt = gatt;

            final Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);

            if (localMethod != null)
            {
                return ((Boolean)localMethod.invoke(localBluetoothGatt, new Object[0]))
                        .booleanValue();
            }
        }
        catch (Exception localException) {
            Log.e("Activ5", "An exception occured while refreshing device");
        }
        return false;
    }

    static int getDeviceRssi(String address)
    {
        return ((TAODevice)mFoundTaoDevicesMap.get(address)).getRssiField();
    }

    private void broadcast(String action, TAODevice taoDevice)
    {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra("TAO_DEVICE_EXTRA", taoDevice);
        try {
            this.mLocalBroadcastManager.sendBroadcast(intent);
        }
        catch (Exception ex) {
            String str = "123";
        }
    }

    private void onDeviceFound(TAODevice taoDevice)
    {
        Log.d("Activ5", "onDeviceFound " + taoDevice.getAddress());
        if (mFoundTaoDevicesMap.containsKey(taoDevice.getAddress())) {
            Log.d("Activ5", "already have " + taoDevice.getAddress());
            TAODevice alreadySaveDevice = (TAODevice)mFoundTaoDevicesMap.get(taoDevice.getAddress());
            alreadySaveDevice.setAddress(taoDevice.getAddress());
            alreadySaveDevice.setRssi(taoDevice.getRssiField());
        } else {
            Log.d("Activ5", "add " + taoDevice.getAddress());
            broadcast("TAO_DEVICE_FOUND_ACTION", taoDevice);
            mFoundTaoDevicesMap.put(taoDevice.getAddress(), taoDevice);
        }
    }


}

