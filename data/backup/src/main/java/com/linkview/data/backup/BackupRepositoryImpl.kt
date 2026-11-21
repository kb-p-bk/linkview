package com.linkview.data.backup

import androidx.room.withTransaction
import com.linkview.core.common.DispatcherProvider
import com.linkview.core.common.Result
import com.linkview.core.common.mention.MentionTokenizer
import com.linkview.core.common.runCatchingResult
import com.linkview.core.model.BackupCard
import com.linkview.core.model.BackupEnvelope
import com.linkview.core.model.BackupUnit
import com.linkview.core.model.Card
import com.linkview.core.model.ImportMode
import com.linkview.core.model.ImportSummary
import com.linkview.data.database.LinkViewDatabase
import com.linkview.data.database.dao.CardDao
import com.linkview.data.database.dao.ContextUnitDao
import com.linkview.data.database.dao.UnitReferenceDao
import com.linkview.data.database.entity.CardEntity
import com.linkview.data.database.entity.CardTagEntity
import com.linkview.data.database.entity.ContextUnitEntity
import com.linkview.data.database.entity.UnitReferenceEntity
import com.linkview.domain.repository.BackupRepository
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: LinkViewDatabase,
    private val cardDao: CardDao,
    private val contextUnitDao: ContextUnitDao,
    private val unitReferenceDao: UnitReferenceDao,
    private val serializer: JsonBackupSerializer,
    private val dispatchers: DispatcherProvider
) : BackupRepository {

    override suspend fun exportJson(): Result<String> = withContext(dispatchers.io) {
        runCatchingResult {
            val allCards = cardDao.getAllCardsList()
            val backupCards = allCards.map { card ->
                val units = contextUnitDao.getUnitsForCard(card.id)
                val backupUnits = units.map { unit ->
                    val refs = MentionTokenizer.extractReferences(unit.rawText).toList()
                    BackupUnit(
                        id = unit.id,
                        sortOrder = unit.sortOrder,
                        rawText = unit.rawText,
                        references = refs
                    )
                }
                val domainCard = card.toDomain()
                BackupCard(
                    id = card.id,
                    title = card.title,
                    tags = domainCard.allTags,
                    type = card.type,
                    createdAt = card.createdAt,
                    updatedAt = card.updatedAt,
                    contextUnits = backupUnits
                )
            }

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val envelope = BackupEnvelope(
                schemaVersion = 1,
                exportedAt = isoFormat.format(Date()),
                cards = backupCards
            )

            serializer.encodeToString(envelope)
        }
    }

    override suspend fun importJson(jsonContent: String, mode: ImportMode): Result<ImportSummary> = withContext(dispatchers.io) {
        runCatchingResult {
            val envelope = serializer.decodeFromString(jsonContent)
            require(envelope.schemaVersion == 1) {
                "Unsupported backup schema version: ${envelope.schemaVersion}"
            }

            var cardsImported = 0
            var unitsImported = 0
            var refsCreated = 0

            database.withTransaction {
                if (mode == ImportMode.REPLACE_ALL) {
                    cardDao.deleteAllCards()
                }

                val importedCardIds = envelope.cards.map { it.id }.toSet()

                for (card in envelope.cards) {
                    val domainCard = Card(
                        id = card.id,
                        title = card.title.trim(),
                        tags = card.allTags,
                        type = card.type?.trim()?.ifBlank { null },
                        createdAt = card.createdAt,
                        updatedAt = card.updatedAt
                    )
                    val cardEntity = CardEntity.fromDomain(domainCard)
                    cardDao.upsertCard(cardEntity)
                    cardsImported++

                    // Sync tags
                    cardDao.deleteCardTagsForCard(card.id)
                    val tagEntities = domainCard.allTags.map { CardTagEntity(cardId = card.id, tag = it) }
                    if (tagEntities.isNotEmpty()) {
                        cardDao.insertCardTags(tagEntities)
                    }

                    if (mode == ImportMode.MERGE) {
                        contextUnitDao.deleteUnitsForCard(card.id)
                    }

                    if (card.contextUnits.isNotEmpty()) {
                        val unitEntities = card.contextUnits.map { unit ->
                            ContextUnitEntity(
                                id = unit.id,
                                sourceCardId = card.id,
                                sortOrder = unit.sortOrder,
                                rawText = unit.rawText,
                                plainText = MentionTokenizer.toPlainText(unit.rawText)
                            )
                        }
                        contextUnitDao.insertUnits(unitEntities)
                        unitsImported += unitEntities.size

                        val references = mutableListOf<UnitReferenceEntity>()
                        for (unit in card.contextUnits) {
                            for (refTargetId in unit.references) {
                                references.add(
                                    UnitReferenceEntity(
                                        unitId = unit.id,
                                        targetCardId = refTargetId
                                    )
                                )
                            }
                        }
                        if (references.isNotEmpty()) {
                            unitReferenceDao.insertReferences(references)
                            refsCreated += references.size
                        }
                    }
                }
            }

            ImportSummary(
                cardsImported = cardsImported,
                unitsImported = unitsImported,
                referencesCreated = refsCreated
            )
        }
    }
}
