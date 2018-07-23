package com.young.scanner

import android.graphics.Rect

interface IScannerView {

    fun onStartPreview()

    fun onStopPreview()

    fun getDecodeArea():Rect

}