package org.dosboxplus.emulator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

class EmulatorView(context: Context) : View(context) {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private var pixels = IntArray(0)
    private var bitmap: Bitmap? = null
    private var generation = -1

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        val info = DosboxNative.frameInfo()
        val nextGeneration = ((info ushr 48) and 0xffff).toInt()
        val width = ((info ushr 24) and 0xffffff).toInt()
        val height = (info and 0xffffff).toInt()
        if (width > 0 && height > 0 && nextGeneration != generation) {
            if (bitmap == null || bitmap?.width != width || bitmap?.height != height) {
                pixels = IntArray(width * height)
                bitmap?.recycle()
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            }
            if (DosboxNative.copyFrame(pixels, width, height)) {
                bitmap?.setPixels(pixels, 0, width, 0, 0, width, height)
                generation = nextGeneration
            }
        }
        bitmap?.let { image ->
            val scale = minOf(this.width.toFloat() / image.width, this.height.toFloat() / image.height)
            val drawWidth = (image.width * scale).toInt()
            val drawHeight = (image.height * scale).toInt()
            val left = (this.width - drawWidth) / 2
            val top = (this.height - drawHeight) / 2
            canvas.drawBitmap(image, null, Rect(left, top, left + drawWidth, top + drawHeight), paint)
        }
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        bitmap?.recycle()
        bitmap = null
        pixels = IntArray(0)
        generation = -1
        super.onDetachedFromWindow()
    }
}
