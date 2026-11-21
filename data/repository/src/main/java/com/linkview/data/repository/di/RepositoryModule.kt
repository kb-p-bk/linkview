package com.linkview.data.repository.di

import com.linkview.core.common.DefaultDispatcherProvider
import com.linkview.core.common.DispatcherProvider
import com.linkview.data.backup.BackupRepositoryImpl
import com.linkview.data.repository.CardRepositoryImpl
import com.linkview.data.repository.CollectionRepositoryImpl
import com.linkview.data.repository.PreferencesRepositoryImpl
import com.linkview.data.repository.SearchRepositoryImpl
import com.linkview.domain.repository.BackupRepository
import com.linkview.domain.repository.CardRepository
import com.linkview.domain.repository.CollectionRepository
import com.linkview.domain.repository.PreferencesRepository
import com.linkview.domain.repository.SearchRepository
import com.linkview.domain.usecase.AutocompleteCardsUseCase
import com.linkview.domain.usecase.CheckDuplicateTitleUseCase
import com.linkview.domain.usecase.DeleteCardUseCase
import com.linkview.domain.usecase.ExportCardsUseCase
import com.linkview.domain.usecase.GetCardDetailWithBacklinksUseCase
import com.linkview.domain.usecase.GetSearchScopeUseCase
import com.linkview.domain.usecase.ImportCardsUseCase
import com.linkview.domain.usecase.SaveCardUseCase
import com.linkview.domain.usecase.SearchCardsUseCase
import com.linkview.domain.usecase.SetSearchScopeUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository

    companion object {
        @Provides
        @Singleton
        fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

        @Provides
        @Singleton
        fun provideGetCardDetailWithBacklinksUseCase(
            cardRepository: CardRepository
        ): GetCardDetailWithBacklinksUseCase = GetCardDetailWithBacklinksUseCase(cardRepository)

        @Provides
        @Singleton
        fun provideSaveCardUseCase(
            cardRepository: CardRepository
        ): SaveCardUseCase = SaveCardUseCase(cardRepository)

        @Provides
        @Singleton
        fun provideCheckDuplicateTitleUseCase(
            cardRepository: CardRepository
        ): CheckDuplicateTitleUseCase = CheckDuplicateTitleUseCase(cardRepository)

        @Provides
        @Singleton
        fun provideDeleteCardUseCase(
            cardRepository: CardRepository
        ): DeleteCardUseCase = DeleteCardUseCase(cardRepository)

        @Provides
        @Singleton
        fun provideAutocompleteCardsUseCase(
            cardRepository: CardRepository
        ): AutocompleteCardsUseCase = AutocompleteCardsUseCase(cardRepository)

        @Provides
        @Singleton
        fun provideSearchCardsUseCase(
            searchRepository: SearchRepository,
            preferencesRepository: PreferencesRepository
        ): SearchCardsUseCase = SearchCardsUseCase(searchRepository, preferencesRepository)

        @Provides
        @Singleton
        fun provideGetSearchScopeUseCase(
            preferencesRepository: PreferencesRepository
        ): GetSearchScopeUseCase = GetSearchScopeUseCase(preferencesRepository)

        @Provides
        @Singleton
        fun provideSetSearchScopeUseCase(
            preferencesRepository: PreferencesRepository
        ): SetSearchScopeUseCase = SetSearchScopeUseCase(preferencesRepository)

        @Provides
        @Singleton
        fun provideExportCardsUseCase(
            backupRepository: BackupRepository
        ): ExportCardsUseCase = ExportCardsUseCase(backupRepository)

        @Provides
        @Singleton
        fun provideImportCardsUseCase(
            backupRepository: BackupRepository
        ): ImportCardsUseCase = ImportCardsUseCase(backupRepository)
    }
}
