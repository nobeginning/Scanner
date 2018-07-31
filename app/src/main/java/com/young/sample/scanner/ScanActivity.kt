package com.young.sample.scanner

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import com.young.scanner.DecodeListener
import com.young.scanner.ScannerComponent
import com.young.scanner.ScannerConfiguration
import com.young.scanner.zxing.DecodeFactoryZxing
import kotlinx.android.synthetic.main.activity_scan.*
import me.imid.swipebacklayout.lib.app.SwipeBackActivity

class ScanActivity : SwipeBackActivity(), DecodeListener {

    override fun onDecode(result: String) {
        println("Result: $result")
        if (result.startsWith("http")) {    //模拟一下错误的情况
            Toast.makeText(this, "HA HA, CUO LE BA", Toast.LENGTH_LONG).show()
            Handler(Looper.getMainLooper()).postDelayed({
                scannerComponent?.startDecode()
            }, 1000)
        } else {
            ScanResultActivity.launch(this@ScanActivity, result)
        }
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, ScanActivity::class.java))
        }
    }

    private var scannerComponent: ScannerComponent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScannerConfiguration.decodeFactory = DecodeFactoryZxing()
        setContentView(R.layout.activity_scan)
        tv_close.setOnClickListener {
            finish()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            scannerComponent = ScannerComponent(this, surface_view, this, scanner_anim_view)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 10001)
        }
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
    }

    override fun onStart() {
        println("Scanner : ScanActivity onStart")
        super.onStart()
    }

    override fun onPause() {
        println("Scanner : ScanActivity onPause")
        super.onPause()
    }

    override fun onStop() {
        println("Scanner : ScanActivity onStop")
        super.onStop()
    }

    override fun onDestroy() {
        println("Scanner : ScanActivity onDestroy")
        super.onDestroy()
    }
}
