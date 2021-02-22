package com.coletz.dailyagenda

import android.graphics.Path
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

@JsonSerialize(using = Drawee.DraweeSerializer::class)
@JsonDeserialize(using = Drawee.DraweeDeserializer::class)
class Drawee: Path() {
    var draweePaint: DraweePaint? = null
    private val serializablePath: MutableList<SerializablePath> = mutableListOf()

    override fun moveTo(x: Float, y: Float) {
        super.moveTo(x, y)
        serializablePath.add(SerializablePath.MoveTo(x, y))
    }

    override fun lineTo(x: Float, y: Float) {
        super.lineTo(x, y)
        serializablePath.add(SerializablePath.LineTo(x, y))
    }

    fun copySerialToPath() {
        serializablePath.forEach {
            when (it) {
                is SerializablePath.MoveTo -> super.moveTo(it.x, it.y)
                is SerializablePath.LineTo -> super.lineTo(it.x, it.y)
            }
        }
    }

    companion object {
        private const val KEY_PAINT = "p"
        private const val KEY_SERIALIZABLE_ACTIONS = "a"
    }

    class DraweeDeserializer(vc: Class<*>? = null) : StdDeserializer<Drawee>(vc) {

        private val objectMapper = ObjectMapper()

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Drawee {
            val node = p.codec.readTree<JsonNode>(p)
            // PAINT
            val paintNode = node.get(KEY_PAINT)
            val draweePaint = objectMapper.treeToValue(paintNode, DraweePaint::class.java)
            // ACTION
            val actionNode = node.get(KEY_SERIALIZABLE_ACTIONS)
            val serializableAction = actionNode.map { objectMapper.treeToValue(it, SerializablePath::class.java) }

            return Drawee().apply {
                this.draweePaint = draweePaint

                this.serializablePath.clear()
                this.serializablePath.addAll(serializableAction)
            }
        }

    }

    class DraweeSerializer(t: Class<Drawee>? = null) : StdSerializer<Drawee>(t) {

        override fun serialize(value: Drawee, gen: JsonGenerator, provider: SerializerProvider?) {
            with(gen) {
                // BEGIN
                writeStartObject()
                // PAINT
                if (value.draweePaint != null) {
                    writeObjectField(KEY_PAINT, value.draweePaint)
                }
                // ACTIONS
                writeArrayFieldStart(KEY_SERIALIZABLE_ACTIONS)
                value.serializablePath.forEach { writeObject(it) }
                writeEndArray()
                // END
                writeEndObject()
            }
        }
    }
}