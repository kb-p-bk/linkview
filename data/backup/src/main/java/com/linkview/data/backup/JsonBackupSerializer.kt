package com.linkview.data.backup

import com.linkview.core.model.BackupEnvelope
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonBackupSerializer @Inject constructor() {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeToString(envelope: BackupEnvelope): String {
        return json.encodeToString(BackupEnvelope.serializer(), envelope)
    }

    fun decodeFromString(jsonString: String): BackupEnvelope {
        return json.decodeFromString(BackupEnvelope.serializer(), jsonString)
    }
}
