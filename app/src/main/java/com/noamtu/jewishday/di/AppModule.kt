// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.di

import com.noamtu.jewishday.data.DefaultJewishDayRepository
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.DataStoreAppSettingsRepository
import com.noamtu.jewishday.data.DailyLearningCache
import com.noamtu.jewishday.data.DailyLearningRepository
import com.noamtu.jewishday.data.HebcalDailyLearningRepository
import com.noamtu.jewishday.data.JewishDayRepository
import com.noamtu.jewishday.data.SharedPreferencesDailyLearningCache
import com.noamtu.jewishday.data.SharedPreferencesStartupSettingsCache
import com.noamtu.jewishday.data.StartupSettingsCache
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindJewishDayRepository(repository: DefaultJewishDayRepository): JewishDayRepository

    @Binds
    @Singleton
    abstract fun bindAppSettingsRepository(repository: DataStoreAppSettingsRepository): AppSettingsRepository

    @Binds
    @Singleton
    abstract fun bindStartupSettingsCache(cache: SharedPreferencesStartupSettingsCache): StartupSettingsCache

    @Binds
    @Singleton
    abstract fun bindDailyLearningCache(cache: SharedPreferencesDailyLearningCache): DailyLearningCache

    @Binds
    @Singleton
    abstract fun bindDailyLearningRepository(repository: HebcalDailyLearningRepository): DailyLearningRepository
}