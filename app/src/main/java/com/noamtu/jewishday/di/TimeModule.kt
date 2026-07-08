// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.di

import com.noamtu.jewishday.data.DeveloperOverridesRepository
import com.noamtu.jewishday.model.DeveloperClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    // The clock honors the hidden developer time override when one is set, and reads the real
    // system time otherwise. See DeveloperClock / DeveloperOverridesRepository.
    @Provides
    @Singleton
    fun provideClock(developerOverrides: DeveloperOverridesRepository): Clock =
        DeveloperClock(Clock.systemDefaultZone(), developerOverrides)
}