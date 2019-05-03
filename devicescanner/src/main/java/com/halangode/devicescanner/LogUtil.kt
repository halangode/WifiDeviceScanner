package com.halangode.devicescanner

import android.util.Log

/**
 * Created by Harikumar Alangode on 24-Jun-17.
 */

internal object LogUtil {

    fun d(label: String, description: String) {
        if (Constants.isLogEnabled) {
            Log.d(label, description)
        }

    }

    fun e(label: String, description: String) {
        if (Constants.isLogEnabled) {
            Log.e(label, description)
        }
    }
}
