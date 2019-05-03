package com.halangode.devicescanner

import java.util.HashMap

/**
 * Created by Harikumar Alangode on 24-Jun-17.
 */

interface IScanEvents {
    fun onScanCompleted(ips: Map<String, String>)
    fun onError(error: Error)
}
