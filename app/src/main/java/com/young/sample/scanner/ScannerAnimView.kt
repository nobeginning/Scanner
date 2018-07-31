package com.young.sample.scanner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.young.scanner.IScannerView
import com.young.scanner.screenHeight
import com.young.scanner.screenWidth

class ScannerAnimView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : View(context, attrs, defStyleAttr), IScannerView {
    override fun getDecodeArea(): Rect {
        return Rect(finalRectLeft.toInt(), finalRectTop.toInt(), finalRectRight.toInt(), finalRectBottom.toInt())
    }

    override fun onStartPreview() {
        if (!anim.isStarted) {
            anim.start()
        }
    }

    override fun onStopPreview() {
        anim.cancel()
    }

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?) : this(context, null)

    private val mW = (context?.screenWidth() ?: 0) * 0.7
    private var finalRectLeft = ((context?.screenWidth() ?: 0) - mW) / 2
    private var finalRectRight = finalRectLeft + mW
    private var finalRectTop = (context?.screenHeight() ?: 0) / 2 - mW
    private var finalRectBottom = finalRectTop + mW

    private val paint: Paint = Paint().apply {
        isAntiAlias = true
    }
    private val linePaint: Paint = Paint().apply {
        isAntiAlias = true
    }
    private val borderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.WHITE
    }

    private val colorBg = Color.parseColor("#7f000000")

    private val mode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)

    private val roundRectF = RectF(finalRectLeft.toFloat(), finalRectTop.toFloat(), finalRectRight.toFloat(), finalRectBottom.toFloat())
    private val roundCornerPX = 16
    private val alphaBackground = Rect(0, 0, 0, 0)

    private val lineBitmap = BitmapFactory.decodeResource(context?.resources, R.drawable.scan_line)
    private val lineWidth = lineBitmap.width
    private val lineHeight = lineBitmap.height
    private val linePadding = (mW - lineWidth) / 2
    private val lineSrcRect = Rect(0, 0, lineWidth, lineHeight)
    private val lineLeft = finalRectLeft.toInt() + linePadding.toInt()
    private val finalLineTop = finalRectTop.toInt()
    private var lineTop = finalLineTop
    private val lineDstRect = Rect(lineLeft, finalLineTop, lineLeft + lineWidth, finalLineTop + lineHeight)

    private val anim = ValueAnimator.ofInt(finalRectTop.toInt(), finalRectBottom.toInt() - lineHeight).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        addUpdateListener {
            lineTop = it.animatedValue as Int
            postInvalidate()
        }
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        anim.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        anim.cancel()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) {
            return
        }
        alphaBackground.set(0, 0, width, height)
        val sc = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), 255, Canvas.ALL_SAVE_FLAG)
        paint.color = colorBg
        canvas.drawRect(alphaBackground, paint)
        paint.xfermode = mode
        paint.color = Color.TRANSPARENT
        canvas.drawRoundRect(roundRectF, roundCornerPX.toFloat(), roundCornerPX.toFloat(), paint)
        paint.xfermode = null
        canvas.restoreToCount(sc)

        canvas.drawRoundRect(roundRectF, roundCornerPX.toFloat(), roundCornerPX.toFloat(), borderPaint)

        lineDstRect.top = lineTop
        lineDstRect.bottom = lineTop + lineHeight
        canvas.drawBitmap(lineBitmap, lineSrcRect, lineDstRect, linePaint)
    }

}