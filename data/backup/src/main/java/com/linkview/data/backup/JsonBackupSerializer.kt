package com.linkview.data.backup

import com.linkview.core.model.BackupCard
import com.linkview.core.model.BackupEnvelope
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonBackupSerializer @Inject constructor() {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    fun encodeToString(envelope: BackupEnvelope): String {
        return json.encodeToString(BackupEnvelope.serializer(), envelope)
    }

    fun decodeFromString(jsonString: String): BackupEnvelope {
        val trimmed = jsonString.trim()
        val element = json.parseToJsonElement(trimmed)
        return when {
            element is JsonObject && "schemaVersion" in element -> {
                json.decodeFromJsonElement(BackupEnvelope.serializer(), element)
            }
            element is JsonArray -> {
                val cards = json.decodeFromJsonElement(ListSerializer(BackupCard.serializer()), element)
                BackupEnvelope(
                    schemaVersion = 1,
                    exportedAt = "",
                    cards = cards
                )
            }
            element is JsonObject && "title" in element -> {
                val singleCard = json.decodeFromJsonElement(BackupCard.serializer(), element)
                BackupEnvelope(
                    schemaVersion = 1,
                    exportedAt = "",
                    cards = listOf(singleCard)
                )
            }
            else -> {
                json.decodeFromString(BackupEnvelope.serializer(), trimmed)
            }
        }
    }
}
