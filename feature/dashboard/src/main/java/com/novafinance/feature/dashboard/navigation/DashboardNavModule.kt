package com.novafinance.feature.dashboard.navigation

import com.novafinance.core.navigation.NovaNavGraphProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardNavModule {

    @Binds
    @IntoSet
    abstract fun bindDashboardNavGraphProvider(
        impl: DashboardNavGraphProvider
    ): NovaNavGraphProvider
}
