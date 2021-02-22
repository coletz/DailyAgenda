package com.coletz.dailyagenda

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.fasterxml.jackson.databind.ObjectMapper

class CanvasView : View {
    constructor(context: Context?): super(context)
    constructor(context: Context?, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    companion object {
        private const val ERASER_SIZE = 26
    }

    var lastEditMs: Long = -1L
        private set

    private val defaultPaint = newPaint()
    private val eraserPaint = newPaint(colorHex = R.color.eraser.color(context).hex).apply { style = Paint.Style.FILL_AND_STROKE }

    private val drawees = mutableListOf<Drawee>()

    private val canvasClip by lazy { Region(0, 0, width, height) }

    var isErasing: Boolean = false
    private var touchedX = -1F
    private var touchedY = -1F

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        if (isErasing) {
            drawEraser(canvas)
        }
        drawees.forEach {
            canvas.drawPath(it, it.draweePaint?.paint ?: defaultPaint)
        }
    }

    private fun drawEraser(canvas: Canvas) {
        if (touchedX >= 0 && touchedY >= 0) {
            canvas.drawRect(
                touchedX - ERASER_SIZE/2F,
                touchedY - ERASER_SIZE/2F,
                touchedX + ERASER_SIZE/2F,
                touchedY + ERASER_SIZE/2F,
                eraserPaint
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return super.onTouchEvent(event)
        // Get the coordinates of the touch event.
        touchedX = event.x
        touchedY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                lastEditMs = System.currentTimeMillis()
                if (isErasing) {
                    deleteIfTouched(event)
                } else {
                    // Set a new starting point
                    drawees.add(newDrawee(event))
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isErasing) {
                    deleteIfTouched(event)
                } else {
                    // Connect the points
                    drawees.last().lineTo(touchedX, touchedY)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastEditMs = System.currentTimeMillis()
                touchedX = -1F
                touchedY = -1F
            }
            else -> {
                return false
            }
        }

        // Makes our view repaint and call onDraw
        invalidate()
        return true
    }

    private fun deleteIfTouched(event: MotionEvent) {
        val regionTouched = Region(
            event.x.toInt() - ERASER_SIZE/2,
            event.y.toInt() - ERASER_SIZE/2,
            event.x.toInt() + ERASER_SIZE/2,
            event.y.toInt() + ERASER_SIZE/2
        )
        val hasRemoved = drawees.removeIf {
            val regionTest = Region().apply { setPath(it, canvasClip) }
            !regionTest.quickReject(regionTouched) && regionTest.op(regionTouched, Region.Op.INTERSECT)
        }
        if (hasRemoved) {
            invalidate()
        }
    }

    private fun newDrawee(initialEvent: MotionEvent? = null): Drawee {
        return Drawee().apply {
            if (initialEvent != null) {
                moveTo(initialEvent.x, initialEvent.y)
            }
        }
    }

    fun clear() {
        drawees.clear()
        invalidate()
    }

    fun loadDrawees(newDrawees: MutableList<Drawee>) {
        drawees.clear()
        drawees.addAll(newDrawees)
        newDrawees.forEach { it.copySerialToPath() }
        invalidate()
    }

    fun jsonByteArray(): ByteArray {
        return ObjectMapper().writeValueAsBytes(drawees)
    }
}