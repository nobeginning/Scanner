package com.young.scanner

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.View

/**
 * Created by young on 17-10-28.
 */
class ScannerAnimView(context: Context?, attrs: AttributeSet?) : View(context, attrs), IScannerView {
    override fun onStartPreview() {
        lineAnimationEnabled = true
    }

    override fun onStopPreview() {
        lineAnimationEnabled = false
    }

    override fun getDecodeArea(): Rect {
        return Rect(finalRectLeft, finalRectTop, finalRectRight, finalRectBottom)
    }

    private var finalRectLeft = (context?.screenWidth() ?: 0) / 4
    private var finalRectRight = finalRectLeft * 3
    private val mW = finalRectRight - finalRectLeft
    private var finalRectTop = (context?.screenHeight() ?: 0) / 2 - mW
    private var finalRectBottom = finalRectTop + mW

    private var rectLeft = finalRectLeft
    private var rectTop = finalRectTop
    private var rectRight = finalRectRight
    private var rectBottom = finalRectBottom

    private val CORNER_THICK = 3
    private val CORNER_WIDTH = 50

//    var drawCenterRect = false
//        set(value) {
//            field = value
//            postInvalidate()
//        }

    private var lineAnimationEnabled = false
        set(value) {
            field = value
            postInvalidate()
        }
    private val LINE_MARGIN = 10
    private val LINE_STEP = 10
    private val LINE_ANIM_DURATION = 12L
    private var lineStartTop = rectTop
    private val lineColor = Color.parseColor("#eeeeee")
    private val lineBitmap = Bitmap.createBitmap(IntArray(4000) { lineColor }, 400, 10, Bitmap.Config.RGB_565)
    private val lineSrcRect = Rect(0, 0, lineBitmap.width, lineBitmap.height)
    private val lineDestRect = Rect(0, 0, 0, 0)

    private val leftRect = Rect(0, 0, rectLeft, height)
    private val topRect = Rect(rectLeft, 0, rectRight, rectTop)
    private val rightRect = Rect(rectRight, 0, width, height)
    private val bottomRect = Rect(rectLeft, rectBottom, rectRight, height)

    private val leftTopCorner1 = Rect(rectLeft - CORNER_THICK, rectTop - CORNER_THICK, rectLeft, rectTop - CORNER_THICK + CORNER_WIDTH)
    private val leftTopCorner2 = Rect(rectLeft, rectTop - CORNER_THICK, rectLeft + CORNER_WIDTH - CORNER_THICK, rectTop)
    private val rightTopCorner1 = Rect(rectRight, rectTop - CORNER_THICK, rectRight + CORNER_THICK, rectTop - CORNER_THICK + CORNER_WIDTH)
    private val rightTopCorner2 = Rect(rectRight + CORNER_THICK - CORNER_WIDTH, rectTop - CORNER_THICK, rectRight, rectTop)
    private val leftBottomCorner1 = Rect(rectLeft - CORNER_THICK, rectBottom + CORNER_THICK - CORNER_WIDTH, rectLeft, rectBottom + CORNER_THICK)
    private val leftBottomCorner2 = Rect(rectLeft, rectBottom, rectLeft - CORNER_THICK + CORNER_WIDTH, rectBottom + CORNER_THICK)
    private val rightBottomCorner1 = Rect(rectRight, rectBottom + CORNER_THICK - CORNER_WIDTH, rectRight + CORNER_THICK, rectBottom + CORNER_THICK)
    private val rightBottomCorner2 = Rect(rectRight + CORNER_THICK - CORNER_WIDTH, rectBottom, rectRight, rectBottom + CORNER_THICK)

    private val paint = Paint()

    init {

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        paint.color = Color.parseColor("#30000000")
        // 画左边
        leftRect.right = rectLeft
        leftRect.bottom = height
        canvas?.drawRect(leftRect, paint)
        // 画上边
        topRect.left = rectLeft
        topRect.right = rectRight
        topRect.bottom = rectTop
        canvas?.drawRect(topRect, paint)
        // 画右边
        rightRect.left = rectRight
        rightRect.right = width
        rightRect.bottom = height
        canvas?.drawRect(rightRect, paint)
        // 画下边
        bottomRect.left = rectLeft
        bottomRect.top = rectBottom
        bottomRect.right = rectRight
        bottomRect.bottom = height
        canvas?.drawRect(bottomRect, paint)

//        if (drawCenterRect) {
//            canvas?.drawRect(getScanAreaRect(), paint)
//        } else {
        //开始画4个角
        paint.color = Color.parseColor("#55b2cd")

        leftTopCorner1.left = rectLeft - CORNER_THICK
        leftTopCorner1.top = rectTop - CORNER_THICK
        leftTopCorner1.right = rectLeft
        leftTopCorner1.bottom = rectTop - CORNER_THICK + CORNER_WIDTH
        leftTopCorner2.left = rectLeft
        leftTopCorner2.top = rectTop - CORNER_THICK
        leftTopCorner2.right = rectLeft + CORNER_WIDTH - CORNER_THICK
        leftTopCorner2.bottom = rectTop
        canvas?.drawRect(leftTopCorner1, paint)
        canvas?.drawRect(leftTopCorner2, paint)

        rightTopCorner1.left = rectRight
        rightTopCorner1.top = rectTop - CORNER_THICK
        rightTopCorner1.right = rectRight + CORNER_THICK
        rightTopCorner1.bottom = rectTop - CORNER_THICK + CORNER_WIDTH
        rightTopCorner2.left = rectRight + CORNER_THICK - CORNER_WIDTH
        rightTopCorner2.top = rectTop - CORNER_THICK
        rightTopCorner2.right = rectRight
        rightTopCorner2.bottom = rectTop
        canvas?.drawRect(rightTopCorner1, paint)
        canvas?.drawRect(rightTopCorner2, paint)

        leftBottomCorner1.left = rectLeft - CORNER_THICK
        leftBottomCorner1.top = rectBottom + CORNER_THICK - CORNER_WIDTH
        leftBottomCorner1.right = rectLeft
        leftBottomCorner1.bottom = rectBottom + CORNER_THICK
        leftBottomCorner2.left = rectLeft
        leftBottomCorner2.top = rectBottom
        leftBottomCorner2.right = rectLeft - CORNER_THICK + CORNER_WIDTH
        leftBottomCorner2.bottom = rectBottom + CORNER_THICK
        canvas?.drawRect(leftBottomCorner1, paint)
        canvas?.drawRect(leftBottomCorner2, paint)

        rightBottomCorner1.left = rectRight
        rightBottomCorner1.top = rectBottom + CORNER_THICK - CORNER_WIDTH
        rightBottomCorner1.right = rectRight + CORNER_THICK
        rightBottomCorner1.bottom = rectBottom + CORNER_THICK
        rightBottomCorner2.left = rectRight + CORNER_THICK - CORNER_WIDTH
        rightBottomCorner2.top = rectBottom
        rightBottomCorner2.right = rectRight
        rightBottomCorner2.bottom = rectBottom + CORNER_THICK
        canvas?.drawRect(rightBottomCorner1, paint)
        canvas?.drawRect(rightBottomCorner2, paint)
//        }

//        (1 until H step step).forEach {
//            canvas?.drawRect(0f, it.toFloat()-2, W.toFloat(), it.toFloat()+2, paint)
//        }


        if (lineAnimationEnabled) {
            if (lineStartTop >= rectBottom) {
                lineStartTop = rectTop
            }
            lineDestRect.left = rectLeft + LINE_MARGIN
            lineDestRect.top = lineStartTop
            lineDestRect.right = rectRight - LINE_MARGIN
            lineDestRect.bottom = lineStartTop + lineBitmap.height / 2
            canvas?.drawBitmap(lineBitmap, lineSrcRect, lineDestRect, paint)
            lineStartTop += LINE_STEP

            postInvalidateDelayed(LINE_ANIM_DURATION, rectLeft, rectTop,
                    rectRight, rectBottom)
        } else {
            lineStartTop = rectTop
        }
    }

    //以下代码用于扫码框动画，之前收银项目用到的

//    public fun setScanAreaRect(rect: Rect) {
//        rectLeft = rect.left
//        rectTop = rect.top
//        rectRight = rect.right
//        rectBottom = rect.bottom
//        postInvalidate()
//    }
//
//    val scanRect = Rect()
//    public fun getScanAreaRect(): Rect {
//        scanRect.set(rectLeft, rectTop, rectRight, rectBottom)
//        return scanRect
//    }
//
//    val originRect = Rect()
//    public fun getOriginScanAreaRect(): Rect {
//        originRect.set(finalRectLeft, finalRectTop, finalRectRight, finalRectBottom)
//        return originRect
//    }


}