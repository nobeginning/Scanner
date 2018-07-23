package com.young.scanner

import android.graphics.Rect

class DecodeFactoryEmpty:IDecoderFactory {
    override fun generate(): IDecoder {
        return DecoderEmpty()
    }

    class DecoderEmpty:IDecoder{
        override fun isDecoding(): Boolean = true

        override fun decode(bytes: ByteArray, width: Int, height: Int, rect: Rect, decodeListener: DecodeListener) {
            // do nothing
        }
    }
}