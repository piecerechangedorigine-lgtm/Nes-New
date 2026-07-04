package com.novafinance.feature.goals.navigation

import com.novafinance.core.navigation.NovaNavGraphProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class GoalsNavModule {

    @Binds
    @IntoSet
    abstract fun bindGoalsNavGraphProvider(
        impl: GoalsNavGraphProvider
    ): NovaNavGraphProvider
}
