package com.turel.jewishdaynext.di

import com.turel.jewishdaynext.data.DefaultJewishDayRepository
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.data.DataStoreAppSettingsRepository
import com.turel.jewishdaynext.data.DailyLearningCache
import com.turel.jewishdaynext.data.DailyLearningRepository
import com.turel.jewishdaynext.data.HebcalDailyLearningRepository
import com.turel.jewishdaynext.data.JewishDayRepository
import com.turel.jewishdaynext.data.SharedPreferencesDailyLearningCache
import com.turel.jewishdaynext.data.SharedPreferencesStartupSettingsCache
import com.turel.jewishdaynext.data.StartupSettingsCache
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
