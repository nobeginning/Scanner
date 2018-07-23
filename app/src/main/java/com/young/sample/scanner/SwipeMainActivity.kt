package com.young.sample.scanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_swipe_main.*
import me.imid.swipebacklayout.lib.app.SwipeBackActivity

class SwipeMainActivity : SwipeBackActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, SwipeMainActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swipe_main)
        btn_scan.setOnClickListener {
            ScanActivity.launch(this)
        }
    }
}
