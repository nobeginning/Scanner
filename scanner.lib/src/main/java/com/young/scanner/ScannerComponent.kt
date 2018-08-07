package com.young.scanner

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.*
import com.young.scanner.camera.CompatCameraManager
import com.young.scanner.camera.IDecodeDelegate

/**
 * Created by young on 17-10-28.
 */
class ScannerComponent(lifecycleOwner: LifecycleOwner,
                       private val surfaceView: SurfaceView,
                       private val decodeListener: DecodeListener,
                       cameraStatusCallback: CameraStatusCallback? = null,
                       private val scannerView: IScannerView? = null,
                       private val disposable: Boolean? = null,
                       decodeFactory: IDecoderFactory? = null) :
        SurfaceHolder.Callback, DecodeListener, LifecycleObserver, IDecodeDelegate {

    companion object {
        val RECT_WHOLE_SCREEN = Rect(-1, -1, -1, -1)
        var DURATION_AUTO_FOCUS = 1800L
    }

    private val screenSize: Point = Point()

    private var hasSurfaceHolder: Boolean = false
    private var resumed: Boolean = false

    private var decodeSuccess: Boolean = false

    private val context: Context = surfaceView.context
    private val compatCameraManager: CompatCameraManager = CompatCameraManager(context, surfaceView, this, cameraStatusCallback)
    private val surfaceHolder: SurfaceHolder = surfaceView.holder
    private val decodeProxy: DecodeProxy = DecodeProxy(this, decodeFactory)

    init {
//        hasSurfaceHolder = true
        surfaceView.visibility = View.GONE
        surfaceHolder.addCallback(this)
        surfaceView.visibility = View.VISIBLE
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume(lifecycleOwner: LifecycleOwner) {
                resumed = true
                startDecode()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun onPause(lifecycleOwner: LifecycleOwner) {
                resumed = false
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onStop(lifecycleOwner: LifecycleOwner) {
                releaseCamera()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy(lifecycleOwner: LifecycleOwner) {
                lifecycleOwner.lifecycle.removeObserver(this)
            }
        })
        val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getSize(screenSize)

        surfaceView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
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

    override fun decode(data: ByteArray, width: Int, height: Int) {
        val rect = scannerView?.getDecodeArea() ?: RECT_WHOLE_SCREEN
        val scaledRect = if (rect == RECT_WHOLE_SCREEN) {
            null
        } else {
            scaleRect(rect, width, height)
        }
        decodeProxy.decode(data, width, height, scaledRect)
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

    fun startDecode() {
        if (hasSurfaceHolder && resumed) {
            decodeSuccess = false
            compatCameraManager.startDecode()
            scannerView?.onStartPreview()
        }
    }

    fun stopDecode() {
        compatCameraManager.stopDecode()
        scannerView?.onStopPreview()
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {}

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        hasSurfaceHolder = false
        releaseCamera()
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
        hasSurfaceHolder = true
        startDecode()
    }

    public fun releaseCamera() {
        compatCameraManager.releaseCamera()
    }
}
