package com.oliver.a5bluetooth;

import android.util.Log;

import java.util.HashMap;

public class TAO_Device
{
    private final String TAG = "Activ5";

    private MutableInteger m_pressure = new MutableInteger();
    private MutableInteger m_accelerometerX = new MutableInteger();
    private MutableInteger m_accelerometerY = new MutableInteger();
    private MutableInteger m_accelerometerZ = new MutableInteger();
    private MutableInteger m_gyroscopeX = new MutableInteger();
    private MutableInteger m_gyroscopeY = new MutableInteger();
    private MutableInteger m_gyroscopeZ = new MutableInteger();
    private MutableInteger m_heartRate = new MutableInteger();

    private Status m_status = Status.Disconnected;
    private boolean m_heartRateMeasuring = false;
    private String m_deviceaddress = null;
    private String m_devicename = null;
    private TAO_SearcherDevices m_searcherDevices = null;


    public TAO_Device(String address, String name, TAO_SearcherDevices searcherDevices)
    {
        this.m_deviceaddress = address;
        this.m_devicename = name;
        this.m_searcherDevices = searcherDevices;
    }

    public void connect()
    {
        if (this.m_searcherDevices.isBluetoothEnabled()) {
            Log.d("Activ5", "TAO_Device connect " + name());
            if(this.m_searcherDevices.getTaoLibrary().connectDevice(this.m_deviceaddress,false)) {
                setStatus(Status.Process);
            }
        }
    }

    public void disconnect()
    {
        if (this.m_searcherDevices.isBluetoothEnabled()) {
            Log.d("Activ5", "TAO_Device disconnect " + name());
            this.m_searcherDevices.getTaoLibrary().disconnectDevice(this.m_deviceaddress);
        }
    }

    public void startHR()
    {
    }

    public void stopHR()
    {
    }

    public boolean measuringHR()
    {
        return this.m_heartRateMeasuring;
    }

    public int pressure()
    {
        return this.m_searcherDevices.getTaoLibrary().pressure(this.m_deviceaddress);
    }

    public int heartRate()
    {
        return this.m_heartRate.value;
    }

    public int accelerometerX()
    {
        return this.m_accelerometerX.value;
    }

    public int accelerometerY()
    {
        return this.m_accelerometerY.value;
    }

    public int accelerometerZ()
    {
        return this.m_accelerometerZ.value;
    }

    public int gyroscopeX()
    {
        return this.m_gyroscopeX.value;
    }

    public int gyroscopeY()
    {
        return this.m_gyroscopeY.value;
    }

    public int gyroscopeZ()
    {
        return this.m_gyroscopeZ.value;
    }

    public void setLEDColor(int red, int green, int blue, int intensity)
    {
        if (this.m_searcherDevices.isBluetoothEnabled());
    }

    public void setLCDText(String text)
    {
        if (this.m_searcherDevices.isBluetoothEnabled());
    }

    public void setVibratorIntensity(int intensity)
    {
        if (this.m_searcherDevices.isBluetoothEnabled());
    }

    public void setBuzzerFrequency(int frequency)
    {
        if (this.m_searcherDevices.isBluetoothEnabled());
    }

    public String name()
    {
        return this.m_devicename;
    }

    public String address()
    {
        return this.m_deviceaddress;
    }

    public String serialNumber()
    {
        return this.m_deviceaddress;
    }

    public Status status()
    {
        return this.m_status;
    }

    public boolean available(Peripheral peripheral)
    {
        String[] needPeripheralNames = peripheralsNames(peripheral);
        if (null == needPeripheralNames) {
            return false;
        }

        HashMap supportedPeripherals = this.m_searcherDevices.getTaoLibrary().getSupportedPeripherals(this.m_deviceaddress);
        if (null == supportedPeripherals) {
            return false;
        }

        for (String peripheralName : needPeripheralNames) {
            if (!supportedPeripherals.containsKey(peripheralName)) {
                return false;
            }
        }
        return true;
    }

    void setStatus(Status newStatus)
    {
        Log.d("Activ5", "setStatus start of call " + name() + " status on:" + newStatus.toString());
        // On 7.1.1 the connected callback can come after we have already set to Initialized
        if (newStatus != this.m_status && !(newStatus == Status.Connected && this.m_status == Status.Initialized)) {
            this.m_status = newStatus;
            Log.d("Activ5", "Change device " + name() + " status on:" + newStatus.toString());
        }
    }

    void receiveMessage(HashMap<String, Integer> gapiMessage)
    {
        setGAPIValueFromMessage(this.m_pressure, gapiMessage, "pressure");
        setGAPIValueFromMessage(this.m_heartRate, gapiMessage, "hr");
        setGAPIValueFromMessage(this.m_accelerometerX, gapiMessage, "accx");
        setGAPIValueFromMessage(this.m_accelerometerY, gapiMessage, "accy");
        setGAPIValueFromMessage(this.m_accelerometerZ, gapiMessage, "accz");
        setGAPIValueFromMessage(this.m_gyroscopeX, gapiMessage, "gyrox");
        setGAPIValueFromMessage(this.m_gyroscopeY, gapiMessage, "gyrox");
        setGAPIValueFromMessage(this.m_gyroscopeZ, gapiMessage, "gyrox");
    }

    private int statusOrder() {
        return this.m_status.ordinal();
    }

    private boolean availableByPeripherialOrder(int order) {
        if ((order < 0) || (order >= Peripheral.values().length)) {
            return false;
        }
        return available(Peripheral.values()[order]);
    }

    private void setGAPIValueFromMessage(MutableInteger mvalue, HashMap<String, Integer> gapiMessages, String name) {
        if (gapiMessages.containsKey(name))
            mvalue.value = ((Integer)gapiMessages.get(name)).intValue();
    }

    private String[] peripheralsNames(Peripheral peripheral) {
        switch (peripheral) {
            case Pressure: return new String[] {GAPIConstants.PRESSURE};
            case HeartRate: return new String[] {GAPIConstants.HEART_RATE};
            case Accelerometer: return new String[] {GAPIConstants.ACCELEROMETER_X_AXIS, GAPIConstants.ACCELEROMETER_Y_AXIS, GAPIConstants.ACCELEROMETER_Z_AXIS};
            case Gyroscope: return new String[] {GAPIConstants.GYROSCOPE_X_AXIS,GAPIConstants.GYROSCOPE_Y_AXIS,GAPIConstants.GYROSCOPE_Z_AXIS};
            //case LED: return new String[] {GAPIConstants.LED};
            case LCD: return new String[] {GAPIConstants.LCD};
            case Vibrator: return new String[] {GAPIConstants.VIBRATOR};
            case Buzzer: return new String[] {GAPIConstants.BUZZER};
        }
        return null;
    }

    public static enum Status
    {
        Disconnected,

        Connected,

        Initialized,

        Process;
    }

    public static enum Peripheral
    {
        Pressure,
        HeartRate,
        Accelerometer,
        Gyroscope,

        LCD,
        Vibrator,
        Buzzer;
    }

    private class MutableInteger {
        public int value = 0;

        private MutableInteger() {
        }
    }
}

