package com.young.scanner.camera

interface ICameraManager{
    fun startPreview()
    fun stopPreview()
    fun startDecode()
    fun stopDecode()
    fun releaseCamera()
}