package com.oliver.a5bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TAO_SearcherDevices
{
    private final String TAG = "Activ5";

    private BluetoothTaoLibrary m_taoLibrary = null;
    private HashMap<TAODevice, TAO_Device> m_deviceMap = new HashMap();
    private List<TAO_Device> m_devices = new ArrayList();
    private BluetoothTaoBroadcastReceiver m_receiver = new TAOBroadcastReceiver();
    private Activity m_activity = null;
    private TaoDeviceCallback callback;
    private BluetoothStateChangedReceiver m_receiveBluetoothStateChanged;


    public TAO_SearcherDevices(Activity activity, TaoDeviceCallback callback)
    {
        this.m_activity = activity;
        this.callback = callback;

        Log.d("Activ5", "TAO_SearcherDevices constructor " + activity);
        LocalBroadcastManager.getInstance(activity).registerReceiver(this.m_receiver,
                TAOBroadcastReceiver.getIntentFilter());

        if (this.m_receiveBluetoothStateChanged == null) {
            this.m_receiveBluetoothStateChanged = new BluetoothStateChangedReceiver();
        }
        activity.getApplicationContext().registerReceiver(this.m_receiveBluetoothStateChanged, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        initTAOLibrary();
    }

    public void close()
    {
        stopSearch();
        disconnectAllDevices();
        this.m_taoLibrary = null;
        if (this.m_receiveBluetoothStateChanged != null) {
            this.m_activity.getApplicationContext().unregisterReceiver(this.m_receiveBluetoothStateChanged);
        }
    }

    public void startSearch()
    {
        if (isBluetoothEnabled()) {
            Log.v("Activ5", "startSearch() ");

            this.m_devices.clear();
            this.m_taoLibrary.startDiscovery();
            if(callback != null)
            {
                callback.scanningStarted();
            }
        }
    }

    public void stopSearch()
    {
        if (isBluetoothEnabled())
        {
            Log.v("Activ5", "stopSearch() ");
            this.m_taoLibrary.stopDiscovery();
            if(callback != null)
            {
                callback.scanningStopped();
            }
        }
    }

    public boolean isSearching()
    {
        return this.m_taoLibrary.getSearching();
    }

    public TAO_Device[] devices()
    {
        return (TAO_Device[])this.m_devices.toArray(new TAO_Device[this.m_devices.size()]);
    }

    public boolean isBluetoothEnabled()
    {
        return (null != this.m_taoLibrary) && (BluetoothAdapter.getDefaultAdapter().isEnabled());
    }

    public void enableBluetooth(boolean showDialog)
    {
        Log.v("Activ5", "enableBluetooth() " + showDialog);
        if (isBluetoothEnabled()) {
            return;
        }

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (showDialog)
            this.m_activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), -1);
        else
            mBluetoothAdapter.enable();
    }

    public void disableBluetooth()
    {
        Log.v("Activ5", "disableBluetooth() ");
        if (!isBluetoothEnabled()) {
            return;
        }

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.disable();
    }

    void disconnectAllDevices()
    {
        for (TAO_Device device : this.m_devices) {
            device.disconnect();
            device.setStatus(TAO_Device.Status.Disconnected);
        }
    }

    BluetoothTaoLibrary getTaoLibrary()
    {
        return this.m_taoLibrary;
    }

    private TAO_Device createVirtualTaoDevice(TAODevice taoDevice) {
        return new TAO_Device(taoDevice.getAddress(), taoDevice.getName(), this);
    }

    private void initTAOLibrary()
    {
        this.m_taoLibrary = new BluetoothTaoLibrary();
        this.m_taoLibrary.initTaoLibrary(this.m_activity);
    }

    private class TAOBroadcastReceiver extends BluetoothTaoBroadcastReceiver
    {
        private TAOBroadcastReceiver()
        {
        }

        public void onSingleDeviceFound(TAODevice taoDevice)
        {
            Log.v("Activ5", "on Device Found: " + taoDevice.getName());

            TAO_Device currentDevice = (TAO_Device)TAO_SearcherDevices.this.m_deviceMap.get(taoDevice);
            if ((null == currentDevice) || (!TAO_SearcherDevices.this.m_devices.contains(currentDevice))) {
                TAO_Device virtualDevice = TAO_SearcherDevices.this.createVirtualTaoDevice(taoDevice);
                TAO_SearcherDevices.this.m_deviceMap.put(taoDevice, virtualDevice);
                TAO_SearcherDevices.this.m_devices.add(virtualDevice);

                if(callback != null)
                {
                    callback.onDeviceFound(virtualDevice);
                }
            }
        }

        public void onDeviceConnected(TAODevice taoDevice)
        {
            Log.v("Activ5", "on Device Connected: " + taoDevice.getName());
            onSingleDeviceFound(taoDevice); // For case on a late connect and have already cleared list for another search
            if (TAO_SearcherDevices.this.m_deviceMap.containsKey(taoDevice))
                ((TAO_Device)TAO_SearcherDevices.this.m_deviceMap.get(taoDevice)).setStatus(TAO_Device.Status.Connected);
        }

        public void onDeviceDisconnected(TAODevice taoDevice)
        {
            Log.v("Activ5", "on Device Disconnected: " + taoDevice.getName());
            if (TAO_SearcherDevices.this.m_deviceMap.containsKey(taoDevice)) {
                Log.v("Activ5", "set status TAO_Device.Status.Disconnected: " + taoDevice.getName());
                ((TAO_Device)TAO_SearcherDevices.this.m_deviceMap.get(taoDevice)).setStatus(TAO_Device.Status.Disconnected);
            }
            else
            {
                Log.v("Activ5", "not found: " + taoDevice.getName());
            }
        }

        public void onFirstMessageReceived(TAODevice taoDevice)
        {
            if (TAO_SearcherDevices.this.m_deviceMap.containsKey(taoDevice))
                ((TAO_Device)TAO_SearcherDevices.this.m_deviceMap.get(taoDevice)).setStatus(TAO_Device.Status.Initialized);
        }
    }

    public interface TaoDeviceCallback
    {
        void scanningStarted();
        void onDeviceFound(TAO_Device taoDevice);
        void scanningStopped();
    }

    private class BluetoothStateChangedReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                Log.v("Activ5", "Bluetooth State Changed on " + state);

                if (state == BluetoothAdapter.STATE_ON) {
                    TAO_SearcherDevices.this.initTAOLibrary();
                }

                if ((state == BluetoothAdapter.STATE_OFF) || (state == BluetoothAdapter.STATE_TURNING_OFF)) {
                    TAO_SearcherDevices.this.disconnectAllDevices();
                    TAO_SearcherDevices.this.m_taoLibrary = null;
                }
            }
        }
    };
}
