package com.young.sample.scanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_scan_result.*
import me.imid.swipebacklayout.lib.app.SwipeBackActivity

class ScanResultActivity : SwipeBackActivity() {

    companion object {
        fun launch(context: Context, result:String) {
            context.startActivity(Intent(context, ScanResultActivity::class.java).apply {
                putExtra("result", result)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_result)
        val result = intent.getStringExtra("result")
        tv_scan_result.text = result
    }
}
