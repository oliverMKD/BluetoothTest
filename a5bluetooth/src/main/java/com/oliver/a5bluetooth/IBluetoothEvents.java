package com.oliver.a5bluetooth;

public interface IBluetoothEvents
{
    public void onDeviceFirmwareAndProtocolVersionReceived();

    public void onDeviceConnected();

    public void onDeviceInGameModeDetected();
}