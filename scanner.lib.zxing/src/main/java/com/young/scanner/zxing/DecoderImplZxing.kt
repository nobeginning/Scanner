package com.young.scanner.zxing

import android.graphics.Rect
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.young.scanner.DecodeListener
import com.young.scanner.IDecoder
import java.util.*

/**
 * Created by young on 17-10-26.
 */
class DecoderImplZxing : IDecoder {
    override fun isDecoding(): Boolean {
        return decoding
    }

    private val multiFormatReader: MultiFormatReader = MultiFormatReader()
    private var decoding: Boolean = false

    init {
        val hints = Hashtable<DecodeHintType, Any>()
        hints[DecodeHintType.CHARACTER_SET] = "utf-8"
        val list = ArrayList<BarcodeFormat>()
        list.add(BarcodeFormat.CODE_128)
        list.add(BarcodeFormat.QR_CODE)

        hints[DecodeHintType.POSSIBLE_FORMATS] = list
        multiFormatReader.setHints(hints)
    }


    override fun decode(bytes: ByteArray, width: Int, height: Int, rect: Rect, decodeListener: DecodeListener) {
        if (decoding) {
            return
        }

        try {
            decoding = true
//            val step0 = System.currentTimeMillis()
            val rotatedData = ByteArray(bytes.size)
            for (y in 0 until height) {
                for (x in 0 until width)
                    rotatedData[x * height + height - y - 1] = bytes[x + y * width]
            }
//            val step1 = System.currentTimeMillis()
//            println("扫码：数组变换耗时：${step1 - step0}")
            val newWidth = height
            val newHeight = width
            val right = rect.right
            val bottom = rect.bottom
            rect.right = bottom
            rect.bottom = right

            val result = realDecode(rotatedData, newWidth, newHeight, rect)

            if (result != null) {
                decodeListener.onDecode(result)
            }
        } catch (e: Exception) {
//            println("扫码结果：解码异常：$e *** ${e.message}")
        } finally {
            decoding = false
        }
    }

    private fun realDecode(bytes: ByteArray, width: Int, height: Int, rect: Rect): String? {
        try {
            val start = System.currentTimeMillis()
            val source = PlanarYUVLuminanceSource(bytes, width, height, rect.left, rect.top,
                    rect.width(), rect.height(), false)
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val step2 = System.currentTimeMillis()
//            println("扫码：对象创建耗时：${step2 - start}")
//            println("扫码中，开始解码")
            val rawResult: Result? = multiFormatReader.decodeWithState(bitmap)
            return if (rawResult != null) {
                val end = System.currentTimeMillis()
    //                Log.d("扫码中", "Found barcode (" + (end - start) + " ms):\n" + rawResult.toString())
    //                println("扫码耗时：${end - start}")
                rawResult.text
            } else {
    //                Log.d("扫码中", "解码失败")
                null
            }
        } catch (re: ReaderException) {
            // continue
//            Log.d("扫码中", "解码失败")
//            println("扫码结果：解码失败：$re *** ${re.message}")
            return null
        } finally {
            multiFormatReader.reset()
        }
    }
}