package com.oliver.a5bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

@SuppressWarnings("deprecation")
public class BluetoothTaoOldScanner implements BluetoothTaoScanner {
    private BluetoothTaoScanCallback mCallback;

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d("Activ5", "onLeScan " + device);
            mCallback.addDevice(device.getName(), device.getAddress(), rssi, device);
        }
    };

    public  void startScan(BluetoothAdapter ba, final BluetoothTaoScanCallback callback) {
        mCallback = callback;
        ba.startLeScan(mLeScanCallback);
    }

    public  void stopScan(BluetoothAdapter ba) {
        ba.stopLeScan(mLeScanCallback);
    }
}

