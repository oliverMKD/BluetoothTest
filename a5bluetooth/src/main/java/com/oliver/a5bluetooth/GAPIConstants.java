package com.oliver.a5bluetooth;

public class GAPIConstants {

    //GAPI Message Fields
    /**
     * Constant for getting accelerometer x axis measured by the TAO Device, from the GAPI message, value range (−32768 -> 32767)
     */
    public static final String ACCELEROMETER_X_AXIS = "accx";
    /**
     * Constant for getting accelerometer y axis measured by the TAO Device, from the GAPI message, value range (−32768 -> 32767)
     */
    public static final String ACCELEROMETER_Y_AXIS = "accy";
    /**
     * Constant for getting accelerometer z axis measured by the TAO Device, from the GAPI message, value range (−32768 -> 32767)
     */
    public static final String ACCELEROMETER_Z_AXIS = "accz";
    /**
     * Constant for getting gyroscope x axis measured by the TAO Device, from the GAPI message, value range (−32768 -> 32767)
     */
    public static final String GYROSCOPE_X_AXIS = "gyrox";
    /**
     * Constant for getting gyroscope y axis measured by the TAO Device, from the GAPI message, value range (−32768 -> 32767)
     */
    public static final String GYROSCOPE_Y_AXIS = "gyroy";
    /**
     * Constant for getting gyroscope z axis measured by the TAO Device, from the GAPI message, value range (−32768 -> 32767)
     */
    public static final String GYROSCOPE_Z_AXIS = "gyroz";
    /**
     * Constant for getting pressure measured by the TAO Device, from the GAPI message, value range (0 -> 1023)
     */
    public static final String PRESSURE = "pressure";
    /**
     * Constant for getting heart rate measured by the TAO Device, from the GAPI message, value range (0 -> 255)
     */
    public static final String HEART_RATE = "hr";

    /**
     * Constant for getting vibrator by the TAO Device
     */
    public static final String VIBRATOR = "vibrator";

    /**
     * Constant for getting LCD by the TAO Device
     */
    public static final String LCD = "lcd";

    /**
     * Constant for getting BUZZER by the TAO Device
     */
    public static final String BUZZER = "buzzer";



    //Actions
    /**
     * Intent action for device found events
     */
    static final String TAO_DEVICE_FOUND_ACTION = "TAO_DEVICE_FOUND_ACTION";
    /**
     * Intent action for devices connected events
     */
    static final String TAO_DEVICE_CONNECTED_ACTION = "TAO_DEVICES_CONNECTED_ACTION";
    /**
     * Intent action for device disconnected events
     */
    static final String TAO_DEVICE_DISCONNECTED_ACTION = "TAO_DEVICES_DISCONNECTED_ACTION";
    /**
     * Intent action for GAPI message received events
     */
    static final String GAPI_MESSAGE_RECEIVED_ACTION = "GAPI_MESSAGE_RECEIVED_ACTION";

    //Extras
    static final String TAO_DEVICE_EXTRA = "TAO_DEVICE_EXTRA";
    static final String GAPI_MESSAGE_EXTRA = "GAPI_MESSAGE_EXTRA";
}

