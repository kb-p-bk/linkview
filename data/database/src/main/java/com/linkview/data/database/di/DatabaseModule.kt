package com.linkview.data.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.linkview.data.database.LinkViewDatabase
import com.linkview.data.database.dao.CardDao
import com.linkview.data.database.dao.CollectionDao
import com.linkview.data.database.dao.ContextUnitDao
import com.linkview.data.database.dao.UnitReferenceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS collections (" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "createdAt INTEGER NOT NULL, " +
                "updatedAt INTEGER NOT NULL)"
        )
        db.execSQL("INSERT OR IGNORE INTO collections (id, name, createdAt, updatedAt) VALUES ('default', 'Default', 0, 0)")
        db.execSQL("ALTER TABLE cards ADD COLUMN collectionId TEXT NOT NULL DEFAULT 'default'")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_cards_collectionId ON cards (collectionId)")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): LinkViewDatabase {
        return Room.databaseBuilder(
            context,
            LinkViewDatabase::class.java,
            "linkview.db"
        )
            .addMigrations(MIGRATION_2_3)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL("INSERT OR IGNORE INTO collections (id, name, createdAt, updatedAt) VALUES ('default', 'Default', 0, 0)")
                }
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("INSERT OR IGNORE INTO collections (id, name, createdAt, updatedAt) VALUES ('default', 'Default', 0, 0)")
                }
            })
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCollectionDao(database: LinkViewDatabase): CollectionDao = database.collectionDao()

    @Provides
    fun provideCardDao(database: LinkViewDatabase): CardDao = database.cardDao()

    @Provides
    fun provideContextUnitDao(database: LinkViewDatabase): ContextUnitDao = database.contextUnitDao()

    @Provides
    fun provideUnitReferenceDao(database: LinkViewDatabase): UnitReferenceDao = database.unitReferenceDao()
}
