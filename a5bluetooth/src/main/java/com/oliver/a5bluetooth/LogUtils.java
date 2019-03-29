package com.oliver.a5bluetooth;

import android.util.Log;

public class LogUtils {

    private static final String LOG_TAG = "Activ5Logs";

    private static boolean isLogEnabled = true;

    public static void i(String log){
        if (isLogEnabled){
            Log.i(LOG_TAG, log);
        }
    }

    public static void w(String log){
        if (isLogEnabled){
            Log.w(LOG_TAG, log);
        }
    }

    public static void e(String log){
        if (isLogEnabled){
            Log.e(LOG_TAG, log);
        }
    }

    public static void v(String log){
        if (isLogEnabled){
            Log.v(LOG_TAG, log);
        }
    }

    public static void d(String log){
        if (isLogEnabled){
            Log.d(LOG_TAG, log);
        }
    }
}