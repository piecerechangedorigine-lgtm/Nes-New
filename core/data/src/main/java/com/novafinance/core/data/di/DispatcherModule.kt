package com.novafinance.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Single unqualified [CoroutineDispatcher] binding for domain use cases.
 * Every current use case does CPU-bound aggregation (combine/filter/sum)
 * over flows Room already delivers off the main thread, so [Dispatchers.Default]
 * is correct for all of them — if a future use case needs blocking I/O,
 * give it a qualified `@IoDispatcher` binding rather than repurposing this one.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
