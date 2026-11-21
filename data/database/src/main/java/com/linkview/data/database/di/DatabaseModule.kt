package com.linkview.data.database.di

import android.content.Context
import androidx.room.Room
import com.linkview.data.database.LinkViewDatabase
import com.linkview.data.database.dao.CardDao
import com.linkview.data.database.dao.ContextUnitDao
import com.linkview.data.database.dao.UnitReferenceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideCardDao(database: LinkViewDatabase): CardDao = database.cardDao()

    @Provides
    fun provideContextUnitDao(database: LinkViewDatabase): ContextUnitDao = database.contextUnitDao()

    @Provides
    fun provideUnitReferenceDao(database: LinkViewDatabase): UnitReferenceDao = database.unitReferenceDao()
}
