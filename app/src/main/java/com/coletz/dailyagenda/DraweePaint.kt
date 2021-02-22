package com.coletz.dailyagenda

import com.fasterxml.jackson.annotation.JsonProperty

data class DraweePaint(
    @JsonProperty("sw") val strokeWidth: Float? = null,
    @JsonProperty("c") val colorHex: String? = null
) {
    val paint = newPaint(strokeWidth, colorHex)
}