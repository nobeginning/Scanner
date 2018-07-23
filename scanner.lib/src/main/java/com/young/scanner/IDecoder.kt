package com.young.scanner

import android.graphics.Rect

/**
 * Created by young on 17-10-26.
 */
interface IDecoder {
    fun aidlUsed(): Boolean {
        return false
    }
    fun onStart(){}
    fun isDecoding():Boolean
    fun decode(bytes:ByteArray, width:Int, height:Int, rect: Rect, decodeListener: DecodeListener)
}