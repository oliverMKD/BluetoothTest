package com.oliver.a5bluetooth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;

import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class BLEManager {

    private static final String START_HR = "HR!";
    private static final String START_ISOM = "ISOM!";
    public static boolean isDeviceConnected;
//    static long timePassed = 0;
    private static BLEManager bleManager;
    /**
     * Used by {@link BLEService} to start connecting to device in background.
     */
    static long timePassed = 0;
    public boolean isConnected = false;
    public String mBluetoothDeviceAddress = "";
    AlertDialog alert = null;
    boolean mScanning = false;
    private Activ5DeviceListener activ5DeviceListener;
    private String lastPressureMessage = "", currentPressureMessage = "";
    private BluetoothManager manager = null;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private ArrayList<String> deviceNames;
    private int pressureValue = 0;
    private boolean isCanceled = false;
    private BluetoothDevice mSelectedDevice;
    private PressureManager pressureManager;
    private CurrentManager cm = null;
    private boolean once = false;
//    private AppPreferences pref;
    private String prefix = "";
    private Handler connectionHandler;
    private Handler mTimeHandler;
    private BluetoothGattCharacteristic writeChar;
    private Dialog builder;
    private BluetoothGattCharacteristic readCharacteristics;
    private IBluetoothEvents bluetoothEventHandler;
    private boolean isDeviceInGameMode = false;

    /*public void setContext(Context applicationContext)
    {
        context = applicationContext;
    }*/
    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                scanLeDevice18(false);
            } else {
                scanLeDevice21(false);
            }
            mTimeHandler.removeCallbacks(mStopRunnable);
        }
    };
    private Runnable mStartRunnable = new Runnable() {

        @Override
        public void run() {
            startScanning();
        }

    };
    private BluetoothAdapter.LeScanCallback managerScanFeedback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (!deviceNames.contains(device.getName()) && device.getName() != null) {
                if (device.getName().contains("TAO") || device.getName().contains("ACTIV")) {
                    if (checkForPrefix()) {
                        if (device.getName().equals(prefix)) {
                            deviceNames.add(device.getName());
                            if (activ5DeviceListener != null) {
                                activ5DeviceListener.deviceFound(device);
                            }
                        }
                    } else {
                        deviceNames.add(device.getName());
                        if (activ5DeviceListener != null) {
                            activ5DeviceListener.deviceFound(device);
                        }
                    }
                }
            } else {
                device = null;
                scanRecord = null;
            }
        }
    };
    private BluetoothGattCallback deviceFeedback = new BluetoothGattCallback() {

        public void onConnectionStateChange(final BluetoothGatt gatt,
                                            int status, int newState) {

            LogUtils.i(String.valueOf(newState));
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                LogUtils.i("***** CONNECTED *****");
                isDeviceConnected = false;      // will set this to tru only after characteristics are set to the device

                List<BluetoothDevice> items = manager
                        .getConnectedDevices(BluetoothProfile.GATT);
                boolean deviceIsBondedWithManager = items
                        .contains(mSelectedDevice);
                /*if (deviceIsBondedWithManager) {
                    if (Singleton.getInstance().isBLEDeviceConnected() == false)
						Singleton.getInstance().changeConnectionState(true);*/

                setCm(CurrentManager.PRESSURE);

                // if device is connected start services discovery
                gatt.discoverServices();
                /*} else {
                    gatt.disconnect();

					disconnect();

					startScanning();
				}*/
            } else {
                LogUtils.i("***** DISCONNECTED *****");
                isDeviceConnected = false;
                if (activ5DeviceListener != null) {
                    if (status == 133) {
                        activ5DeviceListener.on133Error();
                    } else {
                        activ5DeviceListener.onDeviceConnected(false);
                    }
                }
                // reset pressure if the current manager is pressureManager in
                // order to escape the white line of death
                // waitIdle();

                setCm(CurrentManager.PRESSURE);
                // startScanning();
                pressureValue = 0;
                writeChar = null;
            }

            /*if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                if(activ5DeviceListener != null)
                {
                    activ5DeviceListener.onDeviceConnected(false);
                }
                LogUtils.i("***** DISCONNECTED *****");
                // reset pressure if the current manager is pressureManager in
                // order to escape the white line of death
                // waitIdle();

                setCm(CurrentManager.PRESSURE);
                // startScanning();
                pressureValue = 0;
                writeChar = null;
            }*/

        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                UUID hidUUID = UUID
                        .fromString("00001812-0000-1000-8000-00805F9B34FB");

                BluetoothGattService isHIDServiceFound = gatt
                        .getService(hidUUID);

                if (isHIDServiceFound != null) {
                    setDeviceInGameMode(true);
                    // Log.d("HIDMode", "Detected");
                    disconnect();

                    if (bluetoothEventHandler != null)
                        bluetoothEventHandler.onDeviceInGameModeDetected();
                }

                if (gatt != null) {
                    BluetoothGattCharacteristic characteristics;
                    characteristics = gatt
                            .getService(
                                    UUID.fromString("00005000-0000-1000-8000-00805f9b34fb"))
                            .getCharacteristic(
                                    UUID.fromString("00005a02-0000-1000-8000-00805f9b34fb"));

                    // store the write characteristic
                    writeChar = characteristics;

                    readCharacteristics = gatt
                            .getService(
                                    UUID.fromString("00005000-0000-1000-8000-00805f9b34fb"))
                            .getCharacteristic(
                                    UUID.fromString("00005a01-0000-1000-8000-00805f9b34fb"));

                    boolean ready = gatt.setCharacteristicNotification(
                            readCharacteristics, true);


                    List<BluetoothGattDescriptor> descriptor = readCharacteristics
                            .getDescriptors();

                    int indexOfDescriptor = -1;

                    for (int index = 0; index < descriptor.size(); index++) {
                        BluetoothGattDescriptor currentDescriptor = descriptor
                                .get(index);
                        if (currentDescriptor
                                .getUuid()
                                .equals(UUID
                                        .fromString("00002902-0000-1000-8000-00805f9b34fb"))) {
                            indexOfDescriptor = index;
                        }
                    }

                    if (indexOfDescriptor != -1) {
                        String manqk = descriptor.get(indexOfDescriptor)
                                .getUuid().toString();
                        byte[] test = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                        descriptor
                                .get(indexOfDescriptor)
                                .setValue(
                                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor.get(indexOfDescriptor));
                    }
                    //left for future use if by any reason is needed to read that charachteristic
                    // gatt.readCharacteristic(readCharacteristics);

                    once = true;
                }

                LogUtils.i("***** ON CHARACTERISTICS SET *****");
                isDeviceConnected = true;
                if (activ5DeviceListener != null) {
                    activ5DeviceListener.onDeviceConnected(true);
                }
            }
        }

        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic chars, int status) {

            // Log.d("Char Read", String.valueOf(new String(chars.getValue())));

        }

        public void onCharacteristicWrite(final BluetoothGatt gatt,
                                          BluetoothGattCharacteristic chars, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                String dataToParse = new String(chars.getValue());

                if (dataToParse.contains("TVGTIME")) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
                    Calendar cal = Calendar.getInstance();

                    String data = "D" + dateFormat.format(cal.getTime()) + "/D";
                    byte[] messageToSendASCII = data.getBytes();

                    byte[] dataToSendInBytes = new byte[messageToSendASCII.length + 2];

                    // this will be byte 65 as signed byte
                    dataToSendInBytes[0] = 65;

                    for (int index = 1; index <= messageToSendASCII.length; index++) {
                        dataToSendInBytes[index] = messageToSendASCII[index - 1];
                    }

                    // this will be byte 19 as signed byte
                    dataToSendInBytes[messageToSendASCII.length + 1] = 25;

                    writeChar.setValue(dataToSendInBytes);

                    gatt.writeCharacteristic(writeChar);
                }

                if (dataToParse.contains("AD")) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("HHmmss");
                    Calendar cal = Calendar.getInstance();

                    String data = "T" + dateFormat.format(cal.getTime()) + "/T";
                    byte[] messageToSendASCII = data.getBytes();

                    byte[] dataToSendInBytes = new byte[messageToSendASCII.length + 2];

                    // this will be byte 65 as signed byte
                    dataToSendInBytes[0] = 65;

                    for (int index = 1; index <= messageToSendASCII.length; index++) {
                        dataToSendInBytes[index] = messageToSendASCII[index - 1];
                    }

                    // this will be byte 19 as signed byte
                    dataToSendInBytes[messageToSendASCII.length + 1] = 25;

                    writeChar.setValue(dataToSendInBytes);

                    gatt.writeCharacteristic(writeChar);
                }

                if (dataToParse.contains("/T")) {

                    byte[] messageToSendASCII = START_ISOM.getBytes();

                    // // Log.i("hi","messagetToSendAscII length=="+messageToSendASCII.length);

                    byte[] dataToSendInBytes = new byte[messageToSendASCII.length + 3];

                    // // Log.i("hi","dataToSendInBytes length=="+dataToSendInBytes.length);

                    // this will be byte 65 as signed byte

                    dataToSendInBytes[0] = 10;
                    dataToSendInBytes[1] = 65;

                    for (int index = 2; index <= messageToSendASCII.length + 1; index++) {
                        dataToSendInBytes[index] = messageToSendASCII[index - 2];
                    }

                    // // Log.i("hi","dataToSendInBytes length After=="+dataToSendInBytes.length+"==message length=="+messageToSendASCII.length + 2);

                    // this will be byte 19 as signed byte
                    dataToSendInBytes[messageToSendASCII.length + 2] = 25;

                    // // Log.i("hi","dataToSendInBytes length Final=="+dataToSendInBytes.length+"===value==="+new String(dataToSendInBytes));


                    writeChar.setValue(dataToSendInBytes);

                    byte[] value = writeChar.getValue();

                    // // Log.i("hi","value==="+new String(value));


                    boolean isSuccess = mBluetoothGatt.writeCharacteristic(writeChar);

                    // // Log.i("hi","isSuccess==="+isSuccess);

                    setCm(CurrentManager.PRESSURE);

                }

                if (dataToParse.contains("ISOM")) {
                    // Log.d("Time isometric send : ", String.valueOf(System.currentTimeMillis()));
                    setCm(CurrentManager.PRESSURE);
                }

                if (dataToParse.contains("STOP")) {
                    setCm(CurrentManager.NONE);
                }
            }

            if (status == BluetoothGatt.GATT_FAILURE) {
                String dataToParse = new String(chars.getValue());

                if (dataToParse.contains("TVGTIME")) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
                    Calendar cal = Calendar.getInstance();

                    String data = "D" + dateFormat.format(cal.getTime()) + "/D";
                    byte[] messageToSendASCII = data.getBytes();

                    byte[] dataToSendInBytes = new byte[messageToSendASCII.length + 2];

                    // this will be byte 65 as signed byte
                    dataToSendInBytes[0] = 65;

                    for (int index = 1; index <= messageToSendASCII.length; index++) {
                        dataToSendInBytes[index] = messageToSendASCII[index - 1];
                    }

                    // this will be byte 19 as signed byte
                    dataToSendInBytes[messageToSendASCII.length + 1] = 25;

                    writeChar.setValue(dataToSendInBytes);

                    gatt.writeCharacteristic(writeChar);
                }

                if (dataToParse.contains("AD")) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("HHmmss");
                    Calendar cal = Calendar.getInstance();

                    String data = "T" + dateFormat.format(cal.getTime()) + "/T";
                    byte[] messageToSendASCII = data.getBytes();

                    byte[] dataToSendInBytes = new byte[messageToSendASCII.length + 2];

                    // this will be byte 65 as signed byte
                    dataToSendInBytes[0] = 65;

                    for (int index = 1; index <= messageToSendASCII.length; index++) {
                        dataToSendInBytes[index] = messageToSendASCII[index - 1];
                    }

                    // this will be byte 19 as signed byte
                    dataToSendInBytes[messageToSendASCII.length + 1] = 25;

                    writeChar.setValue(dataToSendInBytes);

                    gatt.writeCharacteristic(writeChar);
                }
            }
        }

        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic chars) {

            String dataToParse = new String(chars.getValue());

            //LogUtils.i("***** onCharacteristicChanged *****");

            if (dataToParse.contains("TC5k")) {

                String[] deviceDataSplitted = dataToParse.split(";");

//                pref.savePreference("PROTOCOL_VERSION", deviceDataSplitted[2]);
//                pref.savePreference(AppConstants.Devices.CONNECTED_DEVICE_FIRMWARE, deviceDataSplitted[1]);

                LogUtils.i("PROTOCOL - " + deviceDataSplitted[2] + " FIRMWARE - " + deviceDataSplitted[1]);

                if (bluetoothEventHandler != null) {
                    bluetoothEventHandler.onDeviceFirmwareAndProtocolVersionReceived();
                }

                // if (deviceDataSplitted[2].equals("04")
                // && deviceDataSplitted[3].contains("BATOK")) {
                byte[] messageToSendASCII = "TVGTIME".getBytes();

                byte[] dataToSendInBytes = new byte[messageToSendASCII.length + 2];

                // this will be byte 65 as signed byte
                dataToSendInBytes[0] = 65;

                for (int index = 1; index <= messageToSendASCII.length; index++) {
                    dataToSendInBytes[index] = messageToSendASCII[index - 1];
                }

                // this will be byte 19 as signed byte
                dataToSendInBytes[messageToSendASCII.length + 1] = 25;

                writeChar.setValue(dataToSendInBytes);

                gatt.writeCharacteristic(writeChar);
            }

            if (getCurrentManagerState() == CurrentManager.PRESSURE) {
                try {
                    pressureValue = pressureManager.parsePressureData(chars);
                    timePassed = System.currentTimeMillis();

                    // Log.d("Time ",String.valueOf(timePassed)+"===pressureValue==="+pressureValue);

                    if (bluetoothEventHandler != null)
                        bluetoothEventHandler.onDeviceConnected();
                } catch (Exception ex) {
                    ex.printStackTrace();

                    currentPressureMessage = new String(chars.getValue());
                    pressureValue = 0;
                    LogUtils.i(ex.toString());
                }

            }
            if (getCurrentManagerState() == CurrentManager.HEARTRATE) {
                //heartManager.handleResponse(new String(chars.getValue()));
                timePassed = System.currentTimeMillis();
            }
        }
    };

    private BLEManager(Context context, Activ5DeviceListener activ5DeviceListener) {
        //context = activityContext;
        this.activ5DeviceListener = activ5DeviceListener;
        connectionHandler = new Handler(context.getMainLooper());
        mTimeHandler = new Handler(context.getMainLooper());
//        pref = AppPreferences.getInstance();
        //Singleton.initialize(context);
        manager = (BluetoothManager) context
                .getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        this.deviceNames = new ArrayList<>();
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            final Intent enableBT = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
        }
        this.once = false;
        this.isCanceled = false;
        this.setCm(CurrentManager.NONE);
        if (pressureManager == null)
            pressureManager = new PressureManager();
        LogUtils.i("BLEManager instance created - " + isConnected);
    }

    public static BLEManager getInstance(Context activityContext, Activ5DeviceListener activ5DeviceListener) {
        if (bleManager == null) {
            bleManager = new BLEManager(activityContext, activ5DeviceListener);
        }
        return bleManager;
    }

    public void deleteInstance() {
        bleManager = null;
    }

    public void setListener(Activ5DeviceListener activ5DeviceListener) {
        this.activ5DeviceListener = activ5DeviceListener;
    }

    public void setEventHandler(IBluetoothEvents event) {
        bluetoothEventHandler = event;
    }

    public IBluetoothEvents getEventsHandler() {
        return this.bluetoothEventHandler;
    }

    private boolean checkForPrefix() {
//        String tempName = pref.getPreference(SensorConstants.SETTINGS_TC_PREFIX, prefix);
//        if (tempName.equals("")) {
//            return false;
//        } else {
//            this.prefix = tempName;
//            return true;
//        }
        return false;
    }

    public void restPressureValue() {
        this.pressureValue = 0;
        setCm(CurrentManager.NONE);
    }

    public void closeConnection() {
        this.isCanceled = true;
        if (this.mBluetoothGatt == null) {
            return;
        }

        refreshDeviceCache(mBluetoothGatt);

        this.mBluetoothGatt.close();
    }

    public void disconnect() {

        isConnected = false;

        if (mBluetoothGatt == null)
            return;

        this.isCanceled = true;

        deviceNames.clear();

        if (this.mBluetoothGatt == null) {
            return;
        }

        refreshDeviceCache(mBluetoothGatt);

        try {
            Method m = mBluetoothGatt.getDevice().getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(mBluetoothGatt.getDevice(), (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
            // Log.e("fail", e.getMessage());
        }

        connectionHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothGatt == null) {
                    connectionHandler.removeCallbacks(this);
                    return;
                }

                try {
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt.close();
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                }
                mBluetoothGatt = null;
                connectionHandler.removeCallbacks(this);
                return;
            }
        });

        setCm(CurrentManager.NONE);
    }

    //used in the old version of tvg
    //left for future use if needed
    //@Deprecated
    public void sendStopCommand() {
        restPressureValue();
        try {
            if (mBluetoothGatt == null) {
                return;
            }

            byte[] messageToSendASCII = "STOP!".getBytes();

            byte[] dataToSendInBytes = new byte[messageToSendASCII.length + 2];

            // this will be byte 65 as signed byte
            dataToSendInBytes[0] = 65;

            for (int index = 1; index <= messageToSendASCII.length; index++) {
                dataToSendInBytes[index] = messageToSendASCII[index - 1];
            }

            // this will be byte 19 as signed byte
            dataToSendInBytes[messageToSendASCII.length + 1] = 25;

            writeChar.setValue(dataToSendInBytes);

            mBluetoothGatt.writeCharacteristic(writeChar);

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            this.setCm(CurrentManager.NONE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void startIsometricExecution() {
        try {
            if (mBluetoothGatt == null || writeChar == null) {
                return;
            }

            byte[] messageToSendASCII = START_ISOM.getBytes();

            byte[] dataToSendInBytes = new byte[messageToSendASCII.length + 2];

            // this will be byte 65 as signed byte
            dataToSendInBytes[0] = 65;

            for (int index = 1; index <= messageToSendASCII.length; index++) {
                dataToSendInBytes[index] = messageToSendASCII[index - 1];
            }

            // this will be byte 19 as signed byte
            dataToSendInBytes[messageToSendASCII.length + 1] = 25;

            writeChar.setValue(dataToSendInBytes);

            mBluetoothGatt.writeCharacteristic(writeChar);

            // Log.d("Time isometric", String.valueOf(System.currentTimeMillis()));

            // try {
            // Thread.sleep(10);
            // } catch (InterruptedException e) {
            // e.printStackTrace();
            // }

            this.setCm(CurrentManager.PRESSURE);
            this.pressureManager = new PressureManager();
            //heartManager = null;
            messageToSendASCII = null;
            dataToSendInBytes = null;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void startFirmwareUpdate() {
        if (mBluetoothGatt == null) {
            return;
        }
        this.setCm(CurrentManager.FIRMWARE);
        this.pressureManager = null;
        //this.heartManager = null;
        File extStore = Environment.getExternalStorageDirectory();
        new FirmwareManager(extStore + "/tc.bin");
        this.once = true;
    }

    public void stopAllCommands() {
        if (mBluetoothGatt == null) {
            return;
        }

        this.setCm(CurrentManager.PRESSURE);

        byte[] messageToSendASCII = "STOP!".getBytes();

        byte[] dataToSendInBytes = new byte[messageToSendASCII.length + 2];

        // this will be byte 65 as signed byte
        dataToSendInBytes[0] = 65;

        for (int index = 1; index <= messageToSendASCII.length; index++) {
            dataToSendInBytes[index] = messageToSendASCII[index - 1];
        }

        // this will be byte 19 as signed byte
        dataToSendInBytes[messageToSendASCII.length + 1] = 25;

        writeChar.setValue(dataToSendInBytes);

        mBluetoothGatt.writeCharacteristic(writeChar);

    }

    public void startHeartrateMeasurement() {
        if (mBluetoothGatt == null) {
            return;
        }
        this.setCm(CurrentManager.HEARTRATE);
        this.pressureManager = null;
        //	heartManager = new TCBleHearthBeatsManager(context);

        // send HR! command to TC
        BluetoothGattCharacteristic characteristics;
        try {
            characteristics = mBluetoothGatt
                    .getService(
                            UUID.fromString("00005000-0000-1000-8000-00805f9b34fb"))
                    .getCharacteristic(
                            UUID.fromString("00005a02-0000-1000-8000-00805f9b34fb"));

            characteristics.setValue(START_HR);
            mBluetoothGatt.writeCharacteristic(characteristics);
        } catch (Exception io) {
            io.printStackTrace();
        }

    }

    public int getPressureData() {
        return this.pressureValue;
    }

    public String lastReceivedMessage() {
        return this.lastPressureMessage;
    }

    public void setLastReceivedMessage(String message) {
        this.lastPressureMessage = message;
    }

    public String currentReceivedMessage() {
        return this.currentPressureMessage;
    }

    public void stopScanningForReal() {
        mScanning = false;
        mBluetoothAdapter.stopLeScan(managerScanFeedback);
        mTimeHandler.removeCallbacks(mStopRunnable);
        mTimeHandler.removeCallbacks(mStartRunnable);
    }

    public void refresh() {
        disconnect();
        deviceNames.clear();
        stopScanningForReal();
        startScanning();
    }

    public void startScanning() {
        if (mBluetoothAdapter != null && !mScanning) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                scanLeDevice18(true);
            } else {
                scanLeDevice21(true);
            }
        }
    }

    public void stopScanning() {
        if (mBluetoothAdapter != null && mScanning) {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(managerScanFeedback);
            //mTimeHandler.postDelayed(mStartRunnable, 1000);
        }
    }

    public boolean connect(final String address, final Context context) {
        if (mBluetoothAdapter == null || address == null) {
            // Log.w("Some", "BluetoothAdapter not initialized or unspecified address.");
            if (activ5DeviceListener != null) {
                activ5DeviceListener.onDeviceConnected(false);
            }
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            LogUtils.i("Trying to use an existing mBluetoothGatt for connection !");
            if (mBluetoothGatt.connect()) {
                if (activ5DeviceListener != null) {
                    activ5DeviceListener.onDeviceConnected(true);
                }
                return true;
            }
            /*else
            {
                return false;
            }*/
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            if (activ5DeviceListener != null) {
                activ5DeviceListener.onDeviceConnected(false);
            }
            LogUtils.i("Device not found.  Unable to connect.");
            return false;
        } else {
            mSelectedDevice = device;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        // pairDevice(device);
        connectionHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt = device.connectGatt(context, false,
                        deviceFeedback);

                refreshDeviceCache(mBluetoothGatt);

                return;

                // waitIdle();
            }
        });

        if (!checkForPrefix()) {
//            pref.savePreference(SensorConstants.SETTINGS_TC_PREFIX,
//                    device.getName());
        }
        LogUtils.i("Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    public void addTaoDeviceToConnect(String deviceName) {
        if (!checkForPrefix()) {
//            pref.savePreference(SensorConstants.SETTINGS_TC_PREFIX,
//                    deviceName.trim());
        } else {
            return;
        }
    }

    public void reconnect() {
        disconnect();

        // force disconnect the device
        // mBluetoothGatt.disconnect();
        mSelectedDevice = null;
        mScanning = false;

        if (isDeviceInGameMode()) {
            setDeviceInGameMode(false);
        }

        startScanning();
    }

    public void showDialog() {
        if (builder != null && !builder.isShowing()) {
            builder.show();
        }
    }

    public CurrentManager getCurrentManagerState() {
        return cm;
    }

    public void setCm(CurrentManager cm) {
        this.cm = cm;
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod(
                    "refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(
                        localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        } catch (Exception localException) {
            localException.printStackTrace();
            // Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }

    public String getProtocolVersion() {
//        return pref.getPreference("PROTOCOL_VERSION", "no device picked");
        return null;
    }

    public boolean isDeviceInGameMode() {
        return isDeviceInGameMode;
    }

    void setDeviceInGameMode(boolean isDeviceInGameMode) {
        this.isDeviceInGameMode = isDeviceInGameMode;
    }

    //@RequiresApi(21)
    private void scanLeDevice21(final boolean enable) {
        //final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mScanning = true;
            //bluetoothLeScanner.startScan(mLeScanCallback21);
            if (activ5DeviceListener != null) {
                activ5DeviceListener.scanningStarted();
            }
            mTimeHandler.postDelayed(mStopRunnable, 10000 * 2);
        } else {
            mScanning = false;
            //bluetoothLeScanner.stopScan(mLeScanCallback21);
            if (activ5DeviceListener != null) {
                activ5DeviceListener.scanningStopped();
            }
        }
    }

    /*@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private final ScanCallback mLeScanCallback21 = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();
            if (!deviceNames.contains(device.getName()) && device.getName() != null)
            {
                if (device.getName().contains("TAO") || device.getName().contains("ACTIV"))
                {
                    if (checkForPrefix())
                    {
                        if (device.getName().equals(prefix))
                        {
                            deviceNames.add(device.getName());
                            if(activ5DeviceListener!=null)
                            {
                                activ5DeviceListener.deviceFound(device);
                            }
                        }
                    }
                    else
                    {
                        deviceNames.add(device.getName());
                        if(activ5DeviceListener!=null)
                        {
                            activ5DeviceListener.deviceFound(device);
                        }
                    }
                }
            }
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            super.onBatchScanResults(results);
        }
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };*/

    /**
     * Scan BLE devices on Android API 18 to 20
     *
     * @param enable Enable scan
     */
    private void scanLeDevice18(boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            //mBluetoothAdapter.startLeScan(mLeScanCallback18);
            if (activ5DeviceListener != null) {
                activ5DeviceListener.scanningStarted();
            }
            mTimeHandler.postDelayed(mStopRunnable, 10000 * 2);
            mScanning = true;

        } else {
            mScanning = false;
            //mBluetoothAdapter.stopLeScan(mLeScanCallback18);
            if (activ5DeviceListener != null) {
                activ5DeviceListener.scanningStopped();
            }
        }
    }

    /*private final BluetoothAdapter.LeScanCallback mLeScanCallback18 = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            if (!deviceNames.contains(device.getName()) && device.getName() != null)
            {
                if (device.getName().contains("TAO") || device.getName().contains("ACTIV"))
                {
                    if (checkForPrefix())
                    {
                        if (device.getName().equals(prefix))
                        {
                            deviceNames.add(device.getName());
                            if(activ5DeviceListener!=null)
                            {
                                activ5DeviceListener.deviceFound(device);
                            }
                            stopScanningForReal();
                            connect(device.getAddress());
                        }
                    }
                    else
                    {
                        deviceNames.add(device.getName());
                        if(activ5DeviceListener!=null)
                        {
                            activ5DeviceListener.deviceFound(device);
                        }
                    }
                }
            }
        }
    };*/

    public enum CurrentManager {
        PRESSURE(0), HEARTRATE(1), FIRMWARE(2), NONE(3), GAME(4);

        private int status;

        private CurrentManager(int code) {
            status = code;
        }

        public int getStatus() {
            return status;
        }
    }
}
