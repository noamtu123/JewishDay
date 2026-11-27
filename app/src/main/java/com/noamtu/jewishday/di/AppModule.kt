// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.di

import com.noamtu.jewishday.data.AndroidCurrentLocationRepository
import com.noamtu.jewishday.data.AndroidDeviceIdentity
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.DataStoreAppSettingsRepository
import com.noamtu.jewishday.data.DataStoreDeveloperOverridesRepository
import com.noamtu.jewishday.data.DefaultJewishDayRepository
import com.noamtu.jewishday.data.DeveloperOverridesRepository
import com.noamtu.jewishday.data.DeviceIdentity
import com.noamtu.jewishday.data.InstallMarker
import com.noamtu.jewishday.data.JewishDayRepository
import com.noamtu.jewishday.data.NotificationPromptState
import com.noamtu.jewishday.data.SharedPreferencesInstallMarker
import com.noamtu.jewishday.data.SharedPreferencesNotificationPromptState
import com.noamtu.jewishday.data.SharedPreferencesStartupSettingsCache
import com.noamtu.jewishday.data.StartupSettingsCache
import com.noamtu.jewishday.i18n.AndroidPerAppLanguage
import com.noamtu.jewishday.i18n.PerAppLanguage
import com.noamtu.jewishday.notification.DateStatusIconScheduler
import com.noamtu.jewishday.notification.ServiceDateStatusIconScheduler
import com.noamtu.jewishday.update.ApkInstaller
import com.noamtu.jewishday.update.AppUpdateRepository
import com.noamtu.jewishday.update.GitHubAppUpdateRepository
import com.noamtu.jewishday.update.PackageInstallerApkInstaller
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
    abstract fun bindNotificationPromptState(
        state: SharedPreferencesNotificationPromptState,
    ): NotificationPromptState

    @Binds
    @Singleton
    abstract fun bindDeviceIdentity(identity: AndroidDeviceIdentity): DeviceIdentity

    @Binds
    @Singleton
    abstract fun bindInstallMarker(marker: SharedPreferencesInstallMarker): InstallMarker

    @Binds
    @Singleton
    abstract fun bindPerAppLanguage(language: AndroidPerAppLanguage): PerAppLanguage

    @Binds
    @Singleton
    abstract fun bindStartupSettingsCache(cache: SharedPreferencesStartupSettingsCache): StartupSettingsCache

    @Binds
    @Singleton
    abstract fun bindCurrentLocationRepository(repository: AndroidCurrentLocationRepository): CurrentLocationRepository

    @Binds
    @Singleton
    abstract fun bindDeveloperOverridesRepository(
        repository: DataStoreDeveloperOverridesRepository,
    ): DeveloperOverridesRepository

    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(repository: GitHubAppUpdateRepository): AppUpdateRepository

    @Binds
    @Singleton
    abstract fun bindApkInstaller(installer: PackageInstallerApkInstaller): ApkInstaller

    @Binds
    @Singleton
    abstract fun bindDateStatusIconScheduler(scheduler: ServiceDateStatusIconScheduler): DateStatusIconScheduler
}
