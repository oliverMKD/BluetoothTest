package com.oliver.a5bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.util.Log;

import java.util.List;

@TargetApi(value = 21)
public class BluetoothTaoNewScanner implements BluetoothTaoScanner {
    private BluetoothTaoScanCallback mCallback;

    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            Log.d("Activ5", "onScanResult " + device);
            mCallback.addDevice(device.getName(), device.getAddress(), result.getRssi(), device);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
        }

        @Override
        public void onScanFailed(int errorCode) {
        }
    };


    public  void startScan(BluetoothAdapter ba, final BluetoothTaoScanCallback callback) {
        mCallback = callback;
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        BluetoothLeScanner bluetoothLeScanner = ba.getBluetoothLeScanner();
        if(bluetoothLeScanner != null)
        {
            bluetoothLeScanner.startScan(null, settings, mScanCallback);
        }
    }

    public  void stopScan(BluetoothAdapter ba) {
        BluetoothLeScanner bluetoothLeScanner = ba.getBluetoothLeScanner();
        if(bluetoothLeScanner != null)
        {
            bluetoothLeScanner.stopScan(mScanCallback);
        }
    }
}

