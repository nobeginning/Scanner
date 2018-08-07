package com.young.scanner.camera

import android.annotation.TargetApi
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.hardware.Camera
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.SurfaceView
import android.view.WindowManager
import com.young.scanner.CameraStatusCallback
import com.young.scanner.ScannerComponent.Companion.DURATION_AUTO_FOCUS
import java.util.*

@TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
class CameraManagerV20(private val context: Context,
                       surfaceView: SurfaceView,
                       private val decodeDelegate: IDecodeDelegate,
                       private val cameraStatusCallback: CameraStatusCallback?=null) : ICameraManager {

    private val msgTypeAutoFocus = 0x01

    private var camera: Camera? = null
    @Volatile
    private var readyToDecode: Boolean = false

    private val surfaceHolder = surfaceView.holder
    private var bestSize: Point? = null
    private var cameraId = -1
    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoFocusHandler: Handler = Handler(Looper.getMainLooper()) {
        return@Handler try {
            when (it.what) {
                msgTypeAutoFocus -> camera?.also {
                    autoFocus(it)
                }
            }
            true
        } catch (e: Exception) {
            true
        }
    }

    private val autoFocusCallback = Camera.AutoFocusCallback { _: Boolean, _: Camera? ->
        autoFocusHandler.sendMessageDelayed(Message.obtain(autoFocusHandler, msgTypeAutoFocus), DURATION_AUTO_FOCUS)
    }

    private val previewCallback = Camera.PreviewCallback { bytes, camera ->
        if (camera != null) {
            if (readyToDecode) {
                val parameters = camera.parameters
                if (parameters != null) {
                    val si = parameters.previewSize
                    if (bytes != null && si != null) {
                        decodeDelegate.decode(bytes, si.width, si.height)
                    }
                }
            } else {
                camera.stopPreview()
            }
        }
    }

    private fun initCamera() {
        if (camera == null) {
            Thread {
                try {
                    if (cameraId < 0) {
                        val cameraCount = Camera.getNumberOfCameras()
                        if (cameraCount == 0) {
                            return@Thread
                        }
                        for (idx in (0 until cameraCount)) {
                            val info = Camera.CameraInfo()
                            Camera.getCameraInfo(idx, info)
                            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                cameraId = idx
                                break
                            }
                        }
                    }
                    if (cameraId < 0) {
                        return@Thread
                    }
                    camera = Camera.open(cameraId)
                    mainHandler.post {
                        cameraStatusCallback?.onCameraOpenSucceed()
                    }
                    val parameters = camera!!.parameters
                    val supportedPreviewSizes = parameters.supportedPreviewSizes
                    val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
                    val display = manager!!.defaultDisplay
                    val screenResolution = Point(display.width, display.height)
                    val screenResolutionForCamera = Point()
                    screenResolutionForCamera.x = screenResolution.x
                    screenResolutionForCamera.y = screenResolution.y
                    // preview size is always something like 480*320, other 320*480
                    if (screenResolution.x < screenResolution.y) {
                        screenResolutionForCamera.x = screenResolution.y
                        screenResolutionForCamera.y = screenResolution.x
                    }
                    val pointForPreview = bestSize
                            ?: findBestPreviewSizeValue(supportedPreviewSizes, screenResolutionForCamera)
                    parameters.setPreviewSize(pointForPreview.x, pointForPreview.y)
                    //                parameters.setPreviewSize(1280, 720);
                    parameters.previewFormat = ImageFormat.NV21
//                parameters.whiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO
                    parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
                    camera!!.setPreviewDisplay(surfaceHolder)
                    camera!!.parameters = parameters

                    camera!!.setPreviewCallback(previewCallback)
                    camera!!.startPreview()
                    camera!!.setDisplayOrientation(90)
                    autoFocus(camera!!)
                } catch (ex: Exception) {
                    println("$ex : ${ex.message}")
                    mainHandler.post {
                        cameraStatusCallback?.onCameraOpenFailed()
                    }
                }
            }.start()
        } else {
            camera!!.setPreviewCallback(previewCallback)
            camera!!.startPreview()
            autoFocus(camera!!)
        }
    }

    override fun startPreview() {

    }

    override fun stopPreview() {

    }

    override fun startDecode() {
        initCamera()
        readyToDecode = true
    }

    override fun stopDecode() {
        readyToDecode = false
    }

    override fun releaseCamera() {
        val finalCamera = camera
        camera = null
        if (finalCamera != null) {
            Thread {
                finalCamera.stopPreview()
                finalCamera.setPreviewCallback(null)
                finalCamera.release()
            }.start()
        }
    }

    private fun autoFocus(camera: Camera) {
        try {
            camera.autoFocus(autoFocusCallback)
        } catch (e: Exception) {
        }
    }

    private fun findBestPreviewSizeValue(sizes: List<Camera.Size>, screenResolution: Point): Point {
        var bestX = screenResolution.x
        var bestY = screenResolution.y
        val screenRatio = screenResolution.x * 1.0 / screenResolution.y
//        Collections.sort(sizes) { size, t1 -> size.width - t1.width }
        Collections.sort(sizes) { size, t1 -> t1.width - size.width }   //从大到小排序
        var diff = java.lang.Double.MAX_VALUE
        for (size in sizes) {
            if (size.width < size.height) {
//                    || (decodeProxy.aidlUsed() && size.width * size.height * 3 / 2 > 1024 * 1024)) {//如果byte[]大于1M，aidl会调用失败(预览bytes的size=previewWidth*previewHeight*3/2)
                continue
            }
            val newX = size.width
            val newY = size.height
            val ratio = newX * 1.0 / newY
            val newDiff = Math.abs(screenRatio - ratio)
            if (newDiff == 0.0) {
                bestX = newX
                bestY = newY
                break
            } else if (newDiff < diff) {
                bestX = newX
                bestY = newY
                diff = newDiff
            }
        }
        bestSize = Point(bestX, bestY)
        return bestSize!!
    }
}