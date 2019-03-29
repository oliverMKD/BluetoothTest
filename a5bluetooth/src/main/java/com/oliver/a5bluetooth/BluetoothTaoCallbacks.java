package com.oliver.a5bluetooth;

import android.bluetooth.BluetoothGatt;

public abstract interface BluetoothTaoCallbacks
{
    public abstract void pressureChange(String paramString, int paramInt);

    public abstract void writecomplete(String paramString);

    public abstract void firstmessage(String paramString);

    public abstract void connected(String paramString);

    public abstract void disconnected(String paramString);

    public abstract void connectionfailed(String paramString, BluetoothGatt gatt, int status);
}