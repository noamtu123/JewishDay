package com.turel.jewishdaynext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turel.jewishdaynext.data.AppSettings
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.ui.JewishDayApp
import com.turel.jewishdaynext.ui.theme.JewishDayNextTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings = appSettingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )

            JewishDayNextTheme(
                blueWhite = settings.value.blueWhiteTheme,
                amoledBlack = settings.value.amoledBlackTheme,
            ) {
                JewishDayApp(useHebrewInterface = settings.value.useHebrewInterface)
            }
        }
    }
}
