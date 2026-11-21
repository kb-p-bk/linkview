package com.linkview.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.linkview.data.database.dao.CardDao
import com.linkview.data.database.dao.ContextUnitDao
import com.linkview.data.database.dao.UnitReferenceDao
import com.linkview.data.database.entity.CardEntity
import com.linkview.data.database.entity.CardTagEntity
import com.linkview.data.database.entity.ContextUnitEntity
import com.linkview.data.database.entity.UnitReferenceEntity

@Database(
    entities = [
        CardEntity::class,
        ContextUnitEntity::class,
        UnitReferenceEntity::class,
        CardTagEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LinkViewDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun contextUnitDao(): ContextUnitDao
    abstract fun unitReferenceDao(): UnitReferenceDao
}
