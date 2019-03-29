package com.oliver.a5bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Formatter;
import java.util.HashMap;
import java.util.UUID;

public class BluetoothTaoGattCallback extends BluetoothGattCallback {
    private static final String mLogSession = "Activ5";
    private BluetoothTaoCallbacks mCallback;
    BluetoothGattCharacteristic mWriteChar;

    //right to left
    public BitSet fromByte(byte b)
    {
        BitSet bits = new BitSet(8);
        for (int i = 0; i < 8; i++)
        {
            bits.set(i, 1 == (b & 1));
            b >>= 1;
        }
        return bits;
    }

    public BluetoothTaoGattCallback(BluetoothTaoCallbacks callback) {
        mCallback = callback;
    }

    public void onConnectionStateChange(final BluetoothGatt gatt,
                                        int status, int newState) {
        Log.d("Activ5","onConnectionStateChange status=" + status + " newState=" + newState);

        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            Log.v("Activ5", "Discovering Services...");
            Log.d("Activ5", "gatt.discoverServices()");
            gatt.discoverServices();
            this.mCallback.connected(gatt.getDevice().getAddress());
        }
        else  {
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.d("Activ5", "Error: (0x" + Integer.toHexString(status) + ")");
                    this.mCallback.connectionfailed(gatt.getDevice().getAddress(),gatt,status);
                }
                else {
                    this.mCallback.disconnected(gatt.getDevice().getAddress());
                }
            }
        }
    }

    private static final UUID SERVICE_UUID = UUID.fromString("00005000-0000-1000-8000-00805f9b34fb");
    private static final UUID WRITE_UUID = UUID.fromString("00005a02-0000-1000-8000-00805f9b34fb");
    private static final UUID READ_UUID = UUID.fromString("00005a01-0000-1000-8000-00805f9b34fb");
    private final static UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private boolean internalEnableNotifications(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0)
            return false;

        Log.d(mLogSession, "gatt.setCharacteristicNotification(" + characteristic.getUuid() + ", true)");
        gatt.setCharacteristicNotification(characteristic, true);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.v(mLogSession, "Enabling notifications for " + characteristic.getUuid());
            Log.d(mLogSession, "gatt.writeDescriptor(" + CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID + ", value=0x01-00)");
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }


    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

        Log.d("Activ5","onServicesDiscovered " + status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d("Activ5","onServicesDiscovered BluetoothGatt.GATT_SUCCESS");

            if (gatt != null) {
                mWriteChar = gatt.getService(SERVICE_UUID).getCharacteristic(WRITE_UUID);
                BluetoothGattCharacteristic readCharacteristics;

                readCharacteristics = gatt.getService(SERVICE_UUID).getCharacteristic(READ_UUID);

                boolean ready = gatt.setCharacteristicNotification(readCharacteristics, true);
                internalEnableNotifications(gatt, readCharacteristics);
            }
        }
    }



    @Override
    public void onCharacteristicWrite(
            BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic,
            int status)
    {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d("Activ5","OnCharacteristicWrite GATT_SUCCESS " + characteristic.getValue());
        }
        else {
            Log.e("Activ5","OnCharacteristicWrite GATT_ERROR " + characteristic.getValue());
        }
        this.mCallback.writecomplete(gatt.getDevice().getAddress());
    }

    @Override
    public void onCharacteristicRead(
            BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic,
            int status)
    {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d("Activ5","onCharacteristicRead GATT_SUCCESS " + characteristic.getValue());

        }
        else {
            Log.e("Activ5","onCharacteristicRead GATT_ERROR " + characteristic.getValue());
        }
    }


    String byteToHex(final byte[] hash)
    {
        Formatter formatter = new Formatter();
        for (byte b : hash)
        {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    String hexToBinary(String hex) {
        return new BigInteger(hex, 16).toString(2);
    }


    private void parseAvailableGAPI(byte[] data)
    {
        String getHexFromBytes = byteToHex(data);
        //get only the bits we need to check
        getHexFromBytes = getHexFromBytes.substring(4,getHexFromBytes.length());

        String binaryResult = hexToBinary(getHexFromBytes);
        Log.d("Activ5","parseAvailableGAPI " + binaryResult);

        HashMap<String, Boolean> supportedPeripheralsMessage = new HashMap<>();

        for(int index = 0; index < binaryResult.length(); index++)
        {
            if(binaryResult.charAt(index) == '1')
            {
                if(index == 0)
                {
                    Log.d("Activ5","supports pressure ");

                    supportedPeripheralsMessage.put(GAPIConstants.PRESSURE, true);
                }

                if(index == 1)
                {
                    supportedPeripheralsMessage.put(GAPIConstants.HEART_RATE, true);
                }

                if(index == 2)
                {
                    supportedPeripheralsMessage.put(GAPIConstants.ACCELEROMETER_X_AXIS, true);
                    supportedPeripheralsMessage.put(GAPIConstants.ACCELEROMETER_Y_AXIS, true);
                    supportedPeripheralsMessage.put(GAPIConstants.ACCELEROMETER_Z_AXIS, true);
                }

                if(index == 3)
                {
                    supportedPeripheralsMessage.put(GAPIConstants.GYROSCOPE_X_AXIS, true);
                    supportedPeripheralsMessage.put(GAPIConstants.GYROSCOPE_Y_AXIS, true);
                    supportedPeripheralsMessage.put(GAPIConstants.GYROSCOPE_Z_AXIS, true);
                }

                //four is fixed zero

                if(index == 5)
                {
                    supportedPeripheralsMessage.put(GAPIConstants.VIBRATOR, true);
                }

                if(index == 6)
                {
                    supportedPeripheralsMessage.put(GAPIConstants.BUZZER, true);
                }

                if(index == 7)
                {
                    supportedPeripheralsMessage.put(GAPIConstants.LCD, true);
                }


            }
        }

        // mTaoDevice.setPeripheralHardware(supportedPeripheralsMessage);
    }


    private void parseMapMessage(byte[] mapReference)
    {
        //we know the message is starting with 'da' and the indexes of the bytes from the map are at position 17 and 18
        int firstByteFromMap = unsignedToBytes(mapReference[17]);
        String firstByteFromMapToBinary = Integer.toBinaryString( (1 << 8) | firstByteFromMap ).substring(1);
        int secondByteFromMap = unsignedToBytes(mapReference[18]);
        String secondByteFromMapToBinary = Integer.toBinaryString( (1 << 8) | secondByteFromMap ).substring(1);

        //lets check each byte from firstByteFromMap
        for(int index = 0; index < 8; index++)
        {
            boolean bitIsSetHigh = firstByteFromMapToBinary.charAt(index) == '1';

            //we need to decrement byte at some position lets validate it
            if(bitIsSetHigh)
            {
                //ax first bit
                if(index == 0)
                {
                    mapReference[2] = (byte) (mapReference[2] - 1);
                }

                //ax second bit
                if(index == 1)
                {
                    mapReference[3] = (byte) (mapReference[3] - 1);
                }

                //ay first bit
                if(index == 2)
                {
                    mapReference[4] = (byte) (mapReference[4] - 1);
                }

                //ay second bit
                if(index == 3)
                {
                    mapReference[5] = (byte) (mapReference[5] - 1);
                }

                //index 4 is always reserved as zero

                //az first bit
                if (index == 5)
                {
                    mapReference[6] = (byte) (mapReference[6] - 1);
                }

                //az second bit
                if(index == 6)
                {
                    mapReference[7] = (byte) (mapReference[7] - 1);
                }

                //gx first bit
                if(index == 7)
                {
                    mapReference[8] = (byte) (mapReference[8] - 1);
                }
            }

        }


        //lets check each byte from secondByteFromMap
        for(int index = 0; index < 8; index++)
        {
            boolean bitIsSetHigh = secondByteFromMapToBinary.charAt(index) == '1';

            //we need to decrement byte at some position lets validate it
            if(bitIsSetHigh)
            {
                //gx second bit
                if(index == 0)
                {
                    mapReference[9] = (byte) (mapReference[9] - 1);
                }

                //gy first bit
                if(index == 1)
                {
                    mapReference[10] = (byte) (mapReference[10] - 1);
                }

                if(index == 2)
                {
                    mapReference[11] = (byte) (mapReference[11] - 1);
                }

                //gz first bit
                if (index == 3)
                {
                    mapReference[12] = (byte) (mapReference[12] - 1);
                }

                //index 4 is always reserved as zero

                //gz second bit
                if(index == 5)
                {
                    mapReference[13] = (byte) (mapReference[13] - 1);
                }

                //pressure
                if(index == 6)
                {
                    mapReference[14] = (byte) (mapReference[14] - 1);
                }

                if(index == 7)
                {
                    mapReference[15] = (byte) (mapReference[15] - 1);
                }
            }

        }
    }

    private void setInitialReply(BluetoothGatt gatt) {
        /**
         gattwriteCharacteristic.setValue("AGAPI");
         gatt.writeCharacteristic(gattwriteCharacteristic);
         ***/

        mWriteChar.setValue("ATVGTIME!GAPI ");
        gatt.writeCharacteristic(mWriteChar);
        Log.v("Activ5","setInitialReply ");
        this.mCallback.firstmessage(gatt.getDevice().getAddress());
    }

    public void sendStop(BluetoothGatt gatt) {
        Log.d("Activ5","sendStop ");
        if(mWriteChar != null) {
            mWriteChar.setValue("STOP!");
            gatt.writeCharacteristic(mWriteChar);
        }
    }



    public int logout = 0;

    @Override
    public synchronized void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        //Done because values from GAPI are unsigned bytes
        byte[] bytesReceivedFromDevice = characteristic.getValue();

        String getMessageFromBytes = new String(characteristic.getValue());
        if((logout++ & 0xF) == 0) {
            Log.v("Activ5","onCharacteristicChanged " + getMessageFromBytes);
        }

        // Initial message we need to put into Game mode
        if(getMessageFromBytes.contains("TC5k"))
        {
            setInitialReply(gatt);
        }

        if(getMessageFromBytes.contains("ph"))
        {
            parseAvailableGAPI(bytesReceivedFromDevice);
        } else {
            /****
             if(mTaoDevice.getSupportedPeripherals() == null) {
             HashMap<String, Boolean> supportedPeripheralsMessage = new HashMap<>();
             supportedPeripheralsMessage.put(GAPIConstants.ACCELEROMETER_X_AXIS, true);
             supportedPeripheralsMessage.put(GAPIConstants.ACCELEROMETER_Y_AXIS, true);
             supportedPeripheralsMessage.put(GAPIConstants.ACCELEROMETER_Z_AXIS, true);
             supportedPeripheralsMessage.put(GAPIConstants.GYROSCOPE_X_AXIS, true);
             supportedPeripheralsMessage.put(GAPIConstants.GYROSCOPE_Y_AXIS, true);
             supportedPeripheralsMessage.put(GAPIConstants.GYROSCOPE_Z_AXIS, true);
             supportedPeripheralsMessage.put(GAPIConstants.PRESSURE, true);
             supportedPeripheralsMessage.put(GAPIConstants.HEART_RATE, true);
             supportedPeripheralsMessage.put(GAPIConstants.VIBRATOR, true);
             supportedPeripheralsMessage.put(GAPIConstants.BUZZER, true);
             supportedPeripheralsMessage.put(GAPIConstants.LCD, true);

             mTaoDevice.setPeripheralHardware(supportedPeripheralsMessage);
             }
             ******/
        }

        if(getMessageFromBytes.contains("da"))
        {
            parseMapMessage(bytesReceivedFromDevice);

            //here we get all the bytes from the message
            int[] bytes = new int[bytesReceivedFromDevice.length];

            //here we gonna convert them into unsigned bytes
            bytes[14] = unsignedToBytes(bytesReceivedFromDevice[14]);
            bytes[15] = unsignedToBytes(bytesReceivedFromDevice[15]);
            bytes[16] = unsignedToBytes(bytesReceivedFromDevice[16]);

            int ax = (bytesReceivedFromDevice[2] << 8 | bytesReceivedFromDevice[3]);
            int ay = (bytesReceivedFromDevice[4] << 8 | bytesReceivedFromDevice[5]);
            int az = (bytesReceivedFromDevice[6] << 8 | bytesReceivedFromDevice[7]);
            int gx = (bytesReceivedFromDevice[8] << 8 | bytesReceivedFromDevice[9]);
            int gy = (bytesReceivedFromDevice[10] << 8 | bytesReceivedFromDevice[11]);
            int gz = (bytesReceivedFromDevice[12] << 8 | bytesReceivedFromDevice[13]);

            //those are unsigned values
            int pressure = (bytes[14] << 8 | bytes[15]);
            int hr = bytes[16];

            HashMap<String, Integer> gapiMessage = new HashMap<>();

            /****
             if(mTaoDevice.getSupportedPeripherals().containsKey(GAPIConstants.ACCELEROMETER_X_AXIS)) {
             gapiMessage.put(GAPIConstants.ACCELEROMETER_X_AXIS, ax);
             gapiMessage.put(GAPIConstants.ACCELEROMETER_Y_AXIS, ay);
             gapiMessage.put(GAPIConstants.ACCELEROMETER_Z_AXIS, az);
             }

             if(mTaoDevice.getSupportedPeripherals().containsKey(GAPIConstants.GYROSCOPE_X_AXIS)) {
             gapiMessage.put(GAPIConstants.GYROSCOPE_X_AXIS, gx);
             gapiMessage.put(GAPIConstants.GYROSCOPE_Y_AXIS, gy);
             gapiMessage.put(GAPIConstants.GYROSCOPE_Z_AXIS, gz);
             }

             if(mTaoDevice.getSupportedPeripherals().containsKey(GAPIConstants.PRESSURE)) {
             gapiMessage.put(GAPIConstants.PRESSURE, pressure);
             }
             ******/

            // This is the only value returned up to the higher levels at this time
            this.mCallback.pressureChange(gatt.getDevice().getAddress(), pressure);

            /***
             if(mTaoDevice.getSupportedPeripherals().containsKey(GAPIConstants.HEART_RATE)) {
             gapiMessage.put(GAPIConstants.HEART_RATE, hr);
             }
             *****/
        }
    }

    private int unsignedToBytes(byte a) {
        int b = ((int)a) & 0xFF;

        return b;
    }


}
