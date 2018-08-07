package com.young.scanner.camera

import android.content.Context
import android.os.Build
import android.view.SurfaceView
import com.young.scanner.CameraStatusCallback

class CompatCameraManager(context: Context,
                          surfaceView: SurfaceView,
                          decodeDelegate: IDecodeDelegate,
                          cameraStatusCallback: CameraStatusCallback? = null) : ICameraManager {

    private val cm: ICameraManager by lazy {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CameraManagerV20(context, surfaceView, decodeDelegate, cameraStatusCallback)
        } else {
            CameraManagerV20(context, surfaceView, decodeDelegate, cameraStatusCallback)
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