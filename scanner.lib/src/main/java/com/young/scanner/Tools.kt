package com.young.scanner

import android.content.Context
import android.graphics.Point
import android.view.WindowManager

private val size = Point(-1, -1)

fun Context.screenWidth(): Int {
    if (size.x > 0) {
        return size.x
    }
    val wm: WindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    wm.defaultDisplay.getSize(size)
    return size.x
}
fun Context.screenHeight(): Int {
    if (size.y > 0) {
        return size.y
    }
    val wm: WindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    wm.defaultDisplay.getSize(size)
    return size.y
}


//裁切NV21 - 此方法只能从中心裁切...
//private fun getDecodeAreaData(data: ByteArray, width: Int, height: Int, realArea: Rect): ByteArray {
//    val goalwidth = realArea.width()
//    val goalheight = realArea.height()
//    val dst = ByteArray(realArea.width() * realArea.height() * 3 / 2)
//
//    var h_div = (height - goalheight) / 2
//    var w_div = (width - goalwidth) / 2
//    if (h_div % 2 != 0) {
//        h_div--
//    }
//    if (w_div % 2 != 0) {
//        w_div--
//    }
//    val src_y_length = width * height
//    val dst_y_length = goalwidth * goalheight
//    for (i in (0 until goalheight)) {
//        for (j in (0 until goalwidth)) {
//            dst[i * goalwidth + j] = data[(i + h_div) * width + j + w_div]
//        }
//    }
//
//    var index = dst_y_length
//    val src_begin = src_y_length + h_div * width / 4
//    val src_u_length = src_y_length / 4
//    val dst_u_length = dst_y_length / 4
//    for (i in (0 until goalheight / 2)) {
//        for (j in (0 until goalwidth / 2)) {
//            val p = src_begin + i * (width shr 1) + (w_div shr 1) + j
//            dst[index] = data[p]
//            dst[dst_u_length + index++] = data[p + src_u_length]
//        }
//    }
//    return dst
//}