package com.novafinance.feature.accounts.navigation

import com.novafinance.core.navigation.NovaNavGraphProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountsNavModule {

    @Binds
    @IntoSet
    abstract fun bindAccountsNavGraphProvider(
        impl: AccountsNavGraphProvider
    ): NovaNavGraphProvider
}
