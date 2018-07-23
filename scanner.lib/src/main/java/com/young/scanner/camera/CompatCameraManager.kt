package com.young.scanner.camera

import android.content.Context
import android.os.Build
import android.view.SurfaceView

class CompatCameraManager(context: Context, surfaceView: SurfaceView, decodeDelegate: IDecodeDelegate) : ICameraManager {

    private val cm: ICameraManager by lazy {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CameraManagerV20(context, surfaceView, decodeDelegate)
        }else {
            CameraManagerV20(context, surfaceView, decodeDelegate)
        }
    }

    override fun startPreview() {
        cm.startPreview()
    }

    override fun stopPreview() {
        cm.stopPreview()
    }

    override fun startDecode() {
        cm.startDecode()
    }

    override fun stopDecode() {
        cm.stopDecode()
    }

    override fun releaseCamera() {
        cm.releaseCamera()
    }

}