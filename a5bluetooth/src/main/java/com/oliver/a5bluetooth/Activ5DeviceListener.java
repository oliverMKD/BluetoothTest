package com.oliver.a5bluetooth;

import android.bluetooth.BluetoothDevice;

public interface Activ5DeviceListener
{
    void scanningStarted();
    void deviceFound(BluetoothDevice device);
    void scanningStopped();
    void onDeviceConnected(boolean isConnected);
    void on133Error();
}