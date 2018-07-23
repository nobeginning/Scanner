package com.young.scanner

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Created by young on 17-10-26.
 */
class DecodeProxy(private val decodeListener: DecodeListener,
                  decodeFactory: IDecoderFactory? = null) : DecodeListener {

    override fun onDecode(result: String) {
        mainHandler.post {
            decodeListener.onDecode(result)
        }
    }

    private var decoder: IDecoder = decodeFactory?.generate()
            ?: ScannerConfiguration.decodeFactory.generate()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val threadPoolExecutor: ThreadPoolExecutor = ThreadPoolExecutor(1, 1, 1000, TimeUnit.MILLISECONDS, SynchronousQueue())

    fun aidlUsed(): Boolean {
        return decoder.aidlUsed()
    }

    fun onStart() {
        decoder.onStart()
    }

    fun decode(bytes: ByteArray, width: Int, height: Int, rect: Rect? = null) {
        if (decoder.isDecoding()) {
            return
        }
        val r = rect ?: Rect(0, 0, width, height)
        try {
//            println("Scanner : 解码：线程池->开启解码线程...")
            threadPoolExecutor.submit {
                decoder.decode(bytes, width, height, r, this)
            }
        } catch (e: RejectedExecutionException) {
//            println("Scanner : 解码中...")
        } catch (ex: Exception) {
//            println("Scanner : 解码中，发生其他错误...")
        }
    }

}