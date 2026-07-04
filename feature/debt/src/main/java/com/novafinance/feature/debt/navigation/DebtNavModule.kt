package com.novafinance.feature.debt.navigation

import com.novafinance.core.navigation.NovaNavGraphProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class DebtNavModule {

    @Binds
    @IntoSet
    abstract fun bindDebtNavGraphProvider(
        impl: DebtNavGraphProvider
    ): NovaNavGraphProvider
}
