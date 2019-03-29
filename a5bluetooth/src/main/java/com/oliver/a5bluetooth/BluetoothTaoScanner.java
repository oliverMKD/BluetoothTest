package com.oliver.a5bluetooth;

import android.bluetooth.BluetoothAdapter;

public interface BluetoothTaoScanner {
    void startScan(BluetoothAdapter ba, final BluetoothTaoScanCallback callback);

    void stopScan(BluetoothAdapter ba);
}

