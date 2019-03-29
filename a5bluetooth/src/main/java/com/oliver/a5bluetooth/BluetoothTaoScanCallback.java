package com.oliver.a5bluetooth;

import android.bluetooth.BluetoothDevice;

public interface BluetoothTaoScanCallback {
    public abstract void addDevice(String name, String address, int rssi, BluetoothDevice device);
}