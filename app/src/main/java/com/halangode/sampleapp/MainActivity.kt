package com.halangode.sampleapp

import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.halangode.devicescanner.DeviceScannerUtil
import com.halangode.devicescanner.IScanEvents
import com.halangode.sampleapp.databinding.ActivityMainBinding
import java.lang.Error

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var deviceScannerUtil: DeviceScannerUtil
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView (
            this,
            R.layout.activity_main)
        deviceScannerUtil = DeviceScannerUtil.instance
        binding.deviceButton.setOnClickListener {
            deviceScannerUtil.setEnableLog(true)
                .setEventListener(object : IScanEvents {
                    override fun onScanCompleted(ips: Map<String, String>) {
                        binding.devicesTV.text = ips.toString()
                    }

                    override fun onError(error: Error) {
                        binding.devicesTV.text = error.message
                    }

                })
                .startDeviceScan(this@MainActivity)
        }
    }
}
