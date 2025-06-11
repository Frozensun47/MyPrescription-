package com.example.myprescription.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Date

// This object tells kotlinx.serialization how to handle the Date type.
object DateSerializer : KSerializer<Date> {
    // Describe the data type we are serializing to. In this case, a Long.
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

    // Converts a Date object into a Long for serialization.
    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeLong(value.time)
    }

    // Converts a Long back into a Date object during deserialization.
    override fun deserialize(decoder: Decoder): Date {
        return Date(decoder.decodeLong())
    }
}