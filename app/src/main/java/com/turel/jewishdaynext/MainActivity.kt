package com.turel.jewishdaynext

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turel.jewishdaynext.data.AppSettings
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.data.CurrentLocationRepository
import com.turel.jewishdaynext.data.hasLocationPermission
import com.turel.jewishdaynext.ui.JewishDayApp
import com.turel.jewishdaynext.ui.theme.JewishDayNextTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository

    @Inject
    lateinit var currentLocationRepository: CurrentLocationRepository

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            currentLocationRepository.refreshCurrentLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings = appSettingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )

            LaunchedEffect(Unit) {
                if (hasLocationPermission()) {
                    currentLocationRepository.refreshCurrentLocation()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }
            }

            JewishDayNextTheme(
                themeOption = settings.value.themeOption,
            ) {
                JewishDayApp(useHebrewInterface = settings.value.useHebrewInterface)
            }
        }
    }
}
