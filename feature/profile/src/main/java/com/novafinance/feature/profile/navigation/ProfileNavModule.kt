package com.novafinance.feature.profile.navigation

import com.novafinance.core.navigation.NovaNavGraphProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileNavModule {

    @Binds
    @IntoSet
    abstract fun bindProfileNavGraphProvider(
        impl: ProfileNavGraphProvider
    ): NovaNavGraphProvider
}
