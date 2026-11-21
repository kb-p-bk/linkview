package com.linkview.data.datastore.di

import com.linkview.data.datastore.PreferencesDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    // PreferencesDataSource is provided by its @Inject constructor
}
