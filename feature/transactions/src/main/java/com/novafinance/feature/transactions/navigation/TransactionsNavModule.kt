package com.novafinance.feature.transactions.navigation

import com.novafinance.core.navigation.NovaNavGraphProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class TransactionsNavModule {

    @Binds
    @IntoSet
    abstract fun bindTransactionsNavGraphProvider(
        impl: TransactionsNavGraphProvider
    ): NovaNavGraphProvider
}
