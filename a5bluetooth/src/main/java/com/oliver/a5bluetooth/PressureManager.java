package com.oliver.a5bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;

public class PressureManager {
    private String TAG = PressureManager.class.getSimpleName();
    private String dataToParse;
    private final int ZERO = 0;

    public PressureManager() {
        this.dataToParse = "";
    }

    public int parsePressureData(BluetoothGattCharacteristic messageToParse) {

        // Log.i("hi","sanitizeResponse(messageToParse)=="+sanitizeResponse(messageToParse));
        return sanitizeResponse(messageToParse);
    }

    private int sanitizeResponse(BluetoothGattCharacteristic chars) {
        //check for IS100/IS message and parse the value if such exists
        dataToParse = new String(chars.getValue());
        dataToParse.trim();

        if (dataToParse.contains("TC5k")) {
            return ZERO;
        }

        int indexOfDash = dataToParse.indexOf('/');
        int indexOfS = dataToParse.indexOf('S') + 1;

        //int lenght = indexOfDash - indexOfS;

        try {
            dataToParse = dataToParse.substring(indexOfS, indexOfDash);
        } catch (StringIndexOutOfBoundsException e) {
            LogUtils.i("My exception caught 1 ***");
            e.printStackTrace();
        }

        try {
            if (Integer.valueOf(dataToParse) == (int) Integer.valueOf(dataToParse)) {
                return Integer.valueOf(dataToParse);
            } else {
                return ZERO;
            }
        } catch (NumberFormatException e) {
            LogUtils.i("My exception caught 2 ***");
            e.printStackTrace();
            return ZERO;
        }
    }
}
