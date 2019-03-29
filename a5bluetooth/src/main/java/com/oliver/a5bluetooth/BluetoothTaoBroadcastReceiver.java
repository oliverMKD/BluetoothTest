package com.oliver.a5bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import java.io.Serializable;

public abstract class BluetoothTaoBroadcastReceiver extends BroadcastReceiver
{
    public static final IntentFilter getIntentFilter()
    {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("TAO_DEVICE_FOUND_ACTION");
        intentFilter.addAction("TAO_DEVICES_CONNECTED_ACTION");
        intentFilter.addAction("TAO_DEVICES_DISCONNECTED_ACTION");
        intentFilter.addAction("FIRST_MESSAGE_RECEIVED_ACTION");
        return intentFilter;
    }

    public final void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }

        Serializable taoDevice = extras.getSerializable("TAO_DEVICE_EXTRA");
        if (taoDevice == null) {
            return;
        }

        if ("TAO_DEVICE_FOUND_ACTION".equals(action))
            onSingleDeviceFound((TAODevice)taoDevice);
        else if ("TAO_DEVICES_CONNECTED_ACTION".equals(action))
            onDeviceConnected((TAODevice)taoDevice);
        else if ("TAO_DEVICES_DISCONNECTED_ACTION".equals(action))
            onDeviceDisconnected((TAODevice)taoDevice);
        else if ("FIRST_MESSAGE_RECEIVED_ACTION".equals(action))
            onFirstMessageReceived((TAODevice)taoDevice);
    }

    public abstract void onSingleDeviceFound(TAODevice paramTAODevice);

    public abstract void onDeviceConnected(TAODevice paramTAODevice);

    public abstract void onDeviceDisconnected(TAODevice paramTAODevice);

    public abstract void onFirstMessageReceived(TAODevice paramTAODevice);
}
