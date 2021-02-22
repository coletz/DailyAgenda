package com.coletz.dailyagenda

import com.fasterxml.jackson.annotation.JsonValue

enum class DraweeAction(@JsonValue val id: Int) {
    MOVE_TO(1),
    LINE_TO(2);

    companion object {
        fun getById(id: Int) = values().first { it.id == id }
    }
}