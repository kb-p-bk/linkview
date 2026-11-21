package com.linkview.data.backup

import com.linkview.core.model.BackupEnvelope
import com.linkview.core.model.ReadableCard
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JsonBackupSerializerTest {

    private val serializer = JsonBackupSerializer()

    @Test
    fun `encodeReadableCardsToString encodes array without database ids or timestamps`() {
        val readableCards = listOf(
            ReadableCard(
                title = "Kotlin",
                tags = listOf("Language", "Android"),
                units = listOf("Modern language for @[Android].", "Concise and expressive.")
            ),
            ReadableCard(
                title = "Android",
                tags = listOf("Platform"),
                units = listOf("Mobile operating system.")
            )
        )

        val json = serializer.encodeReadableCardsToString(readableCards)

        assertTrue(json.contains("\"title\": \"Kotlin\""))
        assertTrue(json.contains("\"Language\""))
        assertTrue(json.contains("\"Modern language for @[Android].\""))
        // Verify database internal fields are not present
        assertTrue(!json.contains("\"createdAt\""))
        assertTrue(!json.contains("\"updatedAt\""))
        assertTrue(!json.contains("\"sortOrder\""))
    }

    @Test
    fun `decodeFromString decodes readable cards array correctly into BackupEnvelope`() {
        val readableJson = """
            [
              {
                "title": "Machine Learning",
                "tags": ["AI"],
                "units": ["Context unit one", "Context unit two with @[Deep Learning]"]
              }
            ]
        """.trimIndent()

        val envelope = serializer.decodeFromString(readableJson)

        assertEquals(1, envelope.cards.size)
        val card = envelope.cards.first()
        assertEquals("Machine Learning", card.title)
        assertEquals(listOf("AI"), card.tags)
        assertEquals(2, card.units.size)
        assertEquals("Context unit one", card.units[0])
    }

    @Test
    fun `decodeFromString decodes full BackupEnvelope correctly`() {
        val fullJson = """
            {
              "schemaVersion": 1,
              "exportedAt": "2026-11-21T15:00:00Z",
              "cards": [
                {
                  "id": "card-1",
                  "title": "System Card",
                  "tags": ["System"],
                  "createdAt": 1000,
                  "updatedAt": 2000,
                  "contextUnits": [
                    {
                      "id": "unit-1",
                      "sortOrder": 0,
                      "rawText": "Unit raw text"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val envelope = serializer.decodeFromString(fullJson)

        assertEquals(1, envelope.schemaVersion)
        assertEquals(1, envelope.cards.size)
        val card = envelope.cards.first()
        assertEquals("card-1", card.id)
        assertEquals("System Card", card.title)
        assertEquals(1, card.contextUnits.size)
        assertEquals("Unit raw text", card.contextUnits[0].rawText)
    }

    @Test
    fun `decodeFromString correctly parses multi-card collection with forward references`() {
        val json = """
            [
              {
                "title": "Panaetius",
                "tags": ["Person"],
                "units": [
                  "Greek Stoic philosopher who taught @[Publius Rutilius Rufus]."
                ]
              },
              {
                "title": "Publius Rutilius Rufus",
                "tags": ["Person"],
                "units": [
                  "Roman statesman, jurist, orator, and historian."
                ]
              }
            ]
        """.trimIndent()

        val envelope = serializer.decodeFromString(json)

        assertEquals(2, envelope.cards.size)
        assertEquals("Panaetius", envelope.cards[0].title)
        assertEquals("Publius Rutilius Rufus", envelope.cards[1].title)
        assertEquals(1, envelope.cards[0].units.size)
        assertEquals("Greek Stoic philosopher who taught @[Publius Rutilius Rufus].", envelope.cards[0].units[0])
    }
}
