package com.young.scanner.zxing

import com.young.scanner.IDecoder
import com.young.scanner.IDecoderFactory

class DecodeFactoryZxing:IDecoderFactory {
    override fun generate(): IDecoder {
        return DecoderImplZxing()
    }
}