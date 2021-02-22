package com.coletz.dailyagenda

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

@JsonDeserialize(using = SerializablePath.DeserializablePathSerializer::class)
sealed class SerializablePath {
    open val type: Int = -1

    @JsonDeserialize(using = JsonDeserializer.None::class)
    data class MoveTo(@JsonProperty("x") val x: Float = -1.0f, @JsonProperty("y") val y: Float = -1.0f): SerializablePath() {
        @JsonProperty("t") override val type: Int = DraweeAction.MOVE_TO.id
    }
    @JsonDeserialize(using = JsonDeserializer.None::class)
    data class LineTo(@JsonProperty("x") val x: Float = -1.0f, @JsonProperty("y") val y: Float = -1.0f): SerializablePath() {
        @JsonProperty("t") override val type: Int = DraweeAction.LINE_TO.id
    }


    class DeserializablePathSerializer(vc: Class<*>? = null) : StdDeserializer<SerializablePath>(vc) {
        private val objectMapper = ObjectMapper()

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): SerializablePath {
            val node = p.codec.readTree<JsonNode>(p)
            return when (node["t"].intValue()) {
                DraweeAction.MOVE_TO.id -> objectMapper.treeToValue(node, MoveTo::class.java)
                DraweeAction.LINE_TO.id -> objectMapper.treeToValue(node, LineTo::class.java)
                else -> throw IllegalArgumentException("Cannot deserialize invalid actionId: ${node["t"].intValue()}")
            }
        }
    }
}
