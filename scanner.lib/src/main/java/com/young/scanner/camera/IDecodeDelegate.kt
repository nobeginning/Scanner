package com.young.scanner.camera

interface IDecodeDelegate {
    fun decode(data: ByteArray, width: Int, height: Int)
}