package com.young.scanner.camera

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.InputConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.support.annotation.RequiresApi
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import java.util.*

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class CameraManagerV21(val context: Context, val surfaceView: SurfaceView, private val decodeDelegate: IDecodeDelegate) : ICameraManager {

    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val surfaceHolder = surfaceView.holder
    private var cameraId: String? = null
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var bestSize:Point? = null

    init {
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder?) {}

            override fun surfaceCreated(holder: SurfaceHolder?) {
                if (imageReader==null){
                    initImageReader()
                }
            }
        })
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice?) {
            cameraDevice = camera
            startPreview()
        }

        override fun onDisconnected(camera: CameraDevice?) {
            camera?.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice?, error: Int) {
            camera?.close()
            cameraDevice = null
        }
    }

    override fun startPreview() {
        val device = cameraDevice ?: return
        val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        if (bestSize!=null) {
            surfaceView.post {
                surfaceHolder.setFixedSize(bestSize!!.x, bestSize!!.y)
            }
        }
        builder.addTarget(surfaceHolder.surface)
        if (imageReader==null){
            initImageReader()
        }
        imageReader?.apply {
            builder.addTarget(surface)
        }
        val list = ArrayList<Surface>()
        list.apply {
            add(surfaceHolder.surface)
            imageReader?.also {
                add(it.surface)
            }
        }
        device.createCaptureSession(list, object : CameraCaptureSession.StateCallback() {
            override fun onConfigureFailed(session: CameraCaptureSession?) {
                println("Scanner V21 : 预览失败")
                session?.close()
            }

            override fun onConfigured(session: CameraCaptureSession?) {
                cameraCaptureSession = session
                cameraCaptureSession?.apply {
                    cameraDevice ?: return
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_STATE_ACTIVE_SCAN)
                    builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    setRepeatingRequest(builder.build(), null, getBackgroundHandler())
                }
            }

        }, null)
    }

    private fun initImageReader(){
        imageReader = ImageReader.newInstance(surfaceView.width, surfaceView.height, ImageFormat.YUV_420_888, 5).apply {
            setOnImageAvailableListener({
                val img = acquireLatestImage()
                img.use { img ->
                    img?.also {
                        //                                    session?.stopRepeating()
                        val planes = it.planes
                        val byteBuffer = planes[0].buffer
                        val bytes = ByteArray(byteBuffer.remaining())
                        byteBuffer.get(bytes)
                        decodeDelegate.decode(bytes, it.width, it.height)
                    }
                }
            }, getBackgroundHandler())
        }
    }

    override fun stopPreview() {

    }

    override fun startDecode() {
        initCamera()
    }

    override fun stopDecode() {

    }

    override fun releaseCamera() {
        cameraCaptureSession?.stopRepeating()
        cameraCaptureSession?.close()
        cameraDevice?.close()
        cameraDevice = null
        cameraCaptureSession = null
    }

    @SuppressLint("MissingPermission")
    private fun initCamera() {
        if (cameraId == null) {
            val cameraList = cameraManager.cameraIdList
            for (cId in cameraList) {
                val characteristics = cameraManager.getCameraCharacteristics(cId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        ?: continue
                val size = map.getOutputSizes(SurfaceTexture::class.java)
                println("Scanner V21 : ${Arrays.toString(size)}")
                if (bestSize==null && size!=null && size.isNotEmpty()) {
                    val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
                    val display = manager!!.defaultDisplay
                    val screenResolution = Point(display.width, display.height)
                    if (surfaceView.width>0 && surfaceView.height>0){
                        screenResolution.x = surfaceView.width
                        screenResolution.y = surfaceView.height
                    }
                    val screenResolutionForCamera = Point()
                    screenResolutionForCamera.x = screenResolution.x
                    screenResolutionForCamera.y = screenResolution.y
                    // preview size is always something like 480*320, other 320*480
                    if (screenResolution.x < screenResolution.y) {
                        screenResolutionForCamera.x = screenResolution.y
                        screenResolutionForCamera.y = screenResolution.x
                    }

                    findBestPreviewSizeValue(size, screenResolutionForCamera)
                    println("Scanner V21 : Find The Best Size : $bestSize")
                }
                cameraId = cId
                break
            }
        }
        cameraManager.openCamera(cameraId, stateCallback, getBackgroundHandler())
    }

    private fun getBackgroundHandler(): Handler {
        val bgThread = HandlerThread("CameraManagerV21").apply {
            start()
        }
        return Handler(bgThread.looper)
    }

    private fun findBestPreviewSizeValue(sizes:Array<Size>, screenResolution: Point): Point {
        var bestX = screenResolution.x
        var bestY = screenResolution.y
        val screenRatio = screenResolution.x * 1.0 / screenResolution.y
//        Collections.sort(sizes) { size, t1 -> size.width - t1.width }
        Collections.sort(sizes.asList()) { size, t1 -> t1.width - size.width }   //从大到小排序
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