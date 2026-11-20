package com.pdfmaster.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SignatureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private var lastX = 0f
    private var lastY = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x; lastY = event.y
                path.moveTo(lastX, lastY)
            }
            MotionEvent.ACTION_MOVE -> {
                path.quadTo(lastX, lastY, (lastX + event.x) / 2, (lastY + event.y) / 2)
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_UP -> { }
        }
        invalidate()
        return true
    }

    fun clear() {
        path.reset()
        invalidate()
    }

    fun isEmpty(): Boolean = path.isEmpty

    fun getBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        canvas.drawPath(path, paint)
        return bmp
    }
}
