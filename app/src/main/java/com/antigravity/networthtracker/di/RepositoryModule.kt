package com.antigravity.networthtracker.di

import com.antigravity.networthtracker.data.repository.AssetRepositoryImpl
import com.antigravity.networthtracker.data.repository.LivePriceRepositoryImpl
import com.antigravity.networthtracker.data.repository.NewsRepositoryImpl
import com.antigravity.networthtracker.domain.repository.AssetRepository
import com.antigravity.networthtracker.domain.repository.LivePriceRepository
import com.antigravity.networthtracker.domain.repository.NewsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAssetRepository(
        assetRepositoryImpl: AssetRepositoryImpl
    ): AssetRepository

    @Binds
    @Singleton
    abstract fun bindLivePriceRepository(
        livePriceRepositoryImpl: LivePriceRepositoryImpl
    ): LivePriceRepository

    @Binds
    @Singleton
    abstract fun bindNewsRepository(
        newsRepositoryImpl: NewsRepositoryImpl
    ): NewsRepository
}

