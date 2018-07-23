package com.young.sample.scanner

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import com.young.scanner.DecodeListener
import com.young.scanner.ScannerComponent
import com.young.scanner.ScannerConfiguration
import com.young.scanner.zxing.DecodeFactoryZxing
import kotlinx.android.synthetic.main.activity_scan.*
import me.imid.swipebacklayout.lib.app.SwipeBackActivity

class ScanActivity : SwipeBackActivity(), DecodeListener {

    override fun onDecode(result: String) {
        println("Result: $result")
        ScanResultActivity.launch(this@ScanActivity, result)
//        ScanActivity.launch(this)
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, ScanActivity::class.java))
        }
    }

    var scannerComponent: ScannerComponent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScannerConfiguration.decodeFactory = DecodeFactoryZxing()
        setContentView(R.layout.activity_scan)
        tv_close.setOnClickListener {
            finish()
        }

        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 10001)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10001) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scannerComponent = ScannerComponent(this, surface_view, this, scanner_anim_view)
            }
        }
    }

    override fun onResume() {
        println("Scanner : ScanActivity onResume")
        super.onResume()
//        scannerComponent?.onStart(this)
//        scannerComponent?.startDecode()
    }

    override fun onStart() {
        println("Scanner : ScanActivity onStart")
        super.onStart()
    }

    override fun onPause() {
        println("Scanner : ScanActivity onPause")
        super.onPause()
//        scannerComponent?.onStop(this)
//        scannerComponent?.stopDecode()
    }

    override fun onStop() {
        println("Scanner : ScanActivity onStop")
        super.onStop()
    }

    override fun onDestroy() {
        println("Scanner : ScanActivity onDestroy")
        super.onDestroy()
//        scannerComponent?.onDestroy(this)
    }
}
