package com.coletz.dailyagenda

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

fun newPaint(strokeWidth: Float? = null, colorHex: String? = null): Paint = Paint().also {
    it.isAntiAlias = true
    it.strokeWidth = strokeWidth ?: 3F
    it.color = colorHex?.let(Color::parseColor) ?: Color.BLACK
    it.style = Paint.Style.STROKE
    it.strokeJoin = Paint.Join.ROUND
}

val @receiver:ColorInt Int.hex: String
    get() = "#%06X".format(0xFFFFFF and this)

fun @receiver:ColorRes Int.color(context: Context): Int =
    ContextCompat.getColor(context, this)

fun log(str: String) {
    android.util.Log.e("LOG", str)
}