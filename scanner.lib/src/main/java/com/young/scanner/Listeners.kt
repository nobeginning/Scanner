package com.young.scanner

/**
 * Created by young on 17-10-26.
 */
interface DecodeListener {
    fun onDecode(result:String)
}

interface CameraStatusCallback {
    fun onCameraOpenSucceed(){}
    fun onCameraOpenFailed()
}