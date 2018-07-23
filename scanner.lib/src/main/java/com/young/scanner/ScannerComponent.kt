package com.young.scanner

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.graphics.*
import android.hardware.Camera
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ImageView
import java.io.ByteArrayOutputStream
import java.lang.ref.SoftReference
import java.util.*

/**
 * Created by young on 17-10-28.
 */
class ScannerComponent(lifecycleOwner: LifecycleOwner,
                       private val surfaceView: SurfaceView,
                       private val decodeListener: DecodeListener,
                       private val scannerView: IScannerView? = null,
                       private val disposable: Boolean? = null,
                       decodeFactory: IDecoderFactory? = null) :
        SurfaceHolder.Callback, DecodeListener, LifecycleObserver {

    companion object {
        val RECT_WHOLE_SCREEN = Rect(-1, -1, -1, -1)
    }

    private val MSG_TYPE_AUTO_FOCUS = 0x01
    private val screenSize: Point = Point()

    private var hasSurfaceHolder: Boolean = false
    private var resumed: Boolean = false

    private var decodeSuccess: Boolean = false
    private var camera: Camera? = null
    private val context: Context = surfaceView.context
    private val surfaceHolder: SurfaceHolder = surfaceView.holder
    private val decodeProxy: DecodeProxy = DecodeProxy(this, decodeFactory)
    @Volatile
    private var readyToDecode: Boolean = false
    private var bestSize: Point? = null
    private val autoFocusHandler: Handler = Handler(Looper.getMainLooper(), {
        return@Handler try {
            when (it.what) {
                MSG_TYPE_AUTO_FOCUS -> camera?.autoFocus(autoFocusCallback)
            }
            true
        } catch (e: Exception) {
            true
        }
    })

    private val autoFocusCallback = Camera.AutoFocusCallback { _: Boolean, _: Camera? ->
        autoFocusHandler.sendMessageDelayed(Message.obtain(autoFocusHandler, MSG_TYPE_AUTO_FOCUS), 1800L)
    }

    private val previewCallback = Camera.PreviewCallback { bytes, camera ->
        if (camera != null) {
            if (readyToDecode) {
                val parameters = camera.parameters
                if (parameters != null) {
                    val si = parameters.previewSize
                    if (bytes != null && si != null) {
                        decode(bytes, si.width, si.height)
                    }
                }
            } else {
                camera.stopPreview()
                scannerView?.onStopPreview()
            }
        }
    }

    init {
        hasSurfaceHolder = true
        surfaceHolder.addCallback(this)
//        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume(lifecycleOwner: LifecycleOwner) {
                println("Scanner : lifecycle - onResume")
                resumed = true
                startDecode()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun onPause(lifecycleOwner: LifecycleOwner) {
                println("Scanner : lifecycle - onPause")
                resumed = false
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onStop(lifecycleOwner: LifecycleOwner) {
                println("Scanner : lifecycle - onStop")
                releaseCamera()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy(lifecycleOwner: LifecycleOwner) {
                println("Scanner : lifecycle - onDestroy")
                lifecycleOwner.lifecycle.removeObserver(this)
            }
        })
        val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getSize(screenSize)

        surfaceView.viewTreeObserver.addOnPreDrawListener(object :ViewTreeObserver.OnPreDrawListener{
            override fun onPreDraw(): Boolean {
                screenSize.x = surfaceView.width
                screenSize.y = surfaceView.height
                surfaceView.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onDecode(result: String) {
        val realResult: String = result
        val finalDisposable = disposable ?: ScannerConfiguration.disposable
        if (finalDisposable && !decodeSuccess) {
            //一次性解码：解码之后不再继续解码
            decodeSuccess = true
            stopDecode()
            postResult(realResult)
        } else if (finalDisposable && decodeSuccess) {
            //一次性解码：解码之后不再处理解码结果（Zxing出现过十毫秒级连续两次解码）
        } else {
            postResult(realResult)
        }
    }

    private fun postResult(result: String) {
        mainHandler.post {
            decodeListener.onDecode(result)
        }
    }

    private fun autoFocus(camera: Camera) {
        try {
            camera.autoFocus(autoFocusCallback)
        } catch (e: Exception) { }
    }

    private fun decode(origin: ByteArray, originWidth: Int, originHeight: Int) {
        val rect = scannerView?.getDecodeArea() ?: RECT_WHOLE_SCREEN
        val scaledRect = if (rect == RECT_WHOLE_SCREEN) {
            null
        } else {
            scaleRect(rect, originWidth, originHeight)
        }
        decodeProxy.decode(origin, originWidth, originHeight, scaledRect)
    }

    private var cachedScaledRect: Rect? = null
    private fun scaleRect(rect: Rect, originWidth: Int, originHeight: Int): Rect {
        if (cachedScaledRect == null) {
            cachedScaledRect = Rect().apply {
                left = rect.left * originHeight / screenSize.x
                top = rect.top * originWidth / screenSize.y
                right = rect.right * originHeight / screenSize.x
                bottom = rect.bottom * originWidth / screenSize.y
            }
        }
        return cachedScaledRect!!
    }

    public fun startDecode() {
        initCamera(true)
    }

    public fun stopDecode() {
        readyToDecode = false
        decodeSuccess = true
//        if (camera != null) {
//            camera!!.stopPreview()
//            camera!!.setPreviewCallback(null)
//            autoFocusHandler.removeCallbacksAndMessages(null)
//            camera!!.cancelAutoFocus()
//        }
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
        println("Scanner：surfaceChanged")
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        println("Scanner：surfaceDestroyed")
        hasSurfaceHolder = false
        releaseCamera()
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
        println("Scanner：surfaceCreated：" + surfaceView.context::class.java.simpleName)
        hasSurfaceHolder = true
        startDecode()
    }

    private fun initCamera(continueToDecode: Boolean = false) {
        if (!hasSurfaceHolder || !resumed) {
            return
        }
        if (camera == null) {
            Thread {
                camera = Camera.open(0)
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
                val pointForPreview = findBestPreviewSizeValue(supportedPreviewSizes, screenResolutionForCamera)
                parameters.setPreviewSize(pointForPreview.x, pointForPreview.y)
                //                parameters.setPreviewSize(1280, 720);
                parameters.previewFormat = ImageFormat.NV21
//                parameters.whiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
                camera!!.setPreviewDisplay(surfaceHolder)
                camera!!.parameters = parameters

                camera!!.setDisplayOrientation(90)
                camera!!.setPreviewCallback(previewCallback)
                camera!!.startPreview()
                scannerView?.onStartPreview()
                autoFocus(camera!!)
                println("Scanner : initCamera : finish")
                if (continueToDecode) {
                    mainHandler.post {
                        decodeSuccess = false
                        if (hasSurfaceHolder && resumed) {
                            readyToDecode = true
                            decodeProxy.onStart()
                        }
                    }
                }
            }.start()
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
            if (size.width < size.height ||
                    (decodeProxy.aidlUsed() && size.width * size.height *3 /2 > 1024 * 1024)) {//如果byte[]大于1M，aidl会调用失败(预览bytes的size=previewWidth*previewHeight*3/2)
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

    public fun releaseCamera() {
        val a = System.currentTimeMillis()
        val finalCamera = camera
        camera = null
        if (finalCamera != null) {
            Thread {
                finalCamera.stopPreview()
                finalCamera.setPreviewCallback(null)
                finalCamera.release()
            }.start()
        }
        println("Scanner : Time : releaseCamera() : ${System.currentTimeMillis() - a}ms")
    }
}
