package com.oliver.a5bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import java.io.Serializable;
import java.util.HashMap;

public class TAODevice
        implements Serializable
{
    private String mName;
    private String mAddress;
    private int mRssi;
    private int mPressure;
    private int mRetrycount;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothTaoGattCallback mCallback;
    private boolean mDisconnect;
    private HashMap<String, Boolean> mSupportedHardware;

    TAODevice(String name, String address, int rssi, BluetoothDevice bluetoothDevice)
    {
        this.mName = name;
        this.mAddress = address;
        this.mRssi = rssi;
        this.mPressure = 0;
        this.mRetrycount = 0;
    }

    public int pressure()
    {
        return this.mPressure;
    }

    public void setPressure(int value) {
        this.mPressure = value;
    }

    public int getRetrycount() {
        return this.mRetrycount;
    }

    public void setRetrycount(int value) {
        this.mRetrycount = value;
    }

    public void setPeripheralHardware(HashMap<String, Boolean> SupportedHardware)
    {
        this.mSupportedHardware = SupportedHardware;
    }

    public HashMap<String, Boolean> getSupportedPeripherals()
    {
        return this.mSupportedHardware;
    }

    public String getAddress()
    {
        return this.mAddress;
    }

    void setAddress(String address) {
        this.mAddress = address;
    }

    public String getName()
    {
        return this.mName;
    }

    public int getRssi()
    {
        return BluetoothTaoLibrary.getDeviceRssi(this.mAddress);
    }

    void setRssi(int rssi) {
        this.mRssi = rssi;
    }

    int getRssiField() {
        return this.mRssi;
    }

    void setDisconnect(boolean b) {
        this.mDisconnect = b;
    }
    boolean getDisconnect() {
        return(this.mDisconnect);
    }

    public BluetoothGatt getBluetoothGatt() {
        return this.mBluetoothGatt;
    }

    void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.mBluetoothGatt = bluetoothGatt;
    }

    void setCallback(BluetoothTaoGattCallback mCallback) {
        this.mCallback = mCallback;
    }

    void sendStop() {
        if(this.mBluetoothGatt != null) {
            mCallback.sendStop(this.mBluetoothGatt);
        }
    }

    public boolean equals(Object o)
    {
        if ((o != null) && ((o instanceof TAODevice)) &&
                (this.mAddress != null) && (this.mAddress.equals(((TAODevice)o).getAddress()))) {
            return true;
        }

        return false;
    }

    public int hashCode()
    {
        return 1;
    }
}
