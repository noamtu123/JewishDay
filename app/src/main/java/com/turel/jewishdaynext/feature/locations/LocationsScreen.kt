package com.turel.jewishdaynext.feature.locations

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turel.jewishdaynext.R
import com.turel.jewishdaynext.data.SavedPlace
import com.turel.jewishdaynext.data.defaultSavedPlace
import com.turel.jewishdaynext.data.offlineKnownPlaces
import com.turel.jewishdaynext.ui.components.InfoCard
import com.turel.jewishdaynext.ui.components.ScreenPaddingValues
import com.turel.jewishdaynext.ui.components.ScreenSurface
import com.turel.jewishdaynext.ui.components.ValuePill
import com.turel.jewishdaynext.ui.components.readableWidth
import com.turel.jewishdaynext.ui.localizedLocationName
import com.turel.jewishdaynext.ui.localizedString
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val CurrentLocationPlaceId = "current_location"

@Composable
fun LocationsScreen(
    modifier: Modifier = Modifier,
    viewModel: LocationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddPlaceDialog by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            saveCurrentDevicePlace(context, viewModel::savePlace)
        }
    }

    ScreenSurface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .readableWidth()
                .fillMaxSize(),
            contentPadding = ScreenPaddingValues,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { LocationsHeader() }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { showAddPlaceDialog = true }) {
                        Text(localizedString(R.string.locations_add_place, R.string.locations_add_place_hebrew))
                    }
                    OutlinedButton(
                        onClick = {
                            if (context.hasLocationPermission()) {
                                saveCurrentDevicePlace(context, viewModel::savePlace)
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            }
                        },
                    ) {
                        Text(localizedString(R.string.locations_use_current, R.string.locations_use_current_hebrew))
                    }
                }
            }
            items(
                items = uiState.savedPlaces,
                key = SavedPlace::id,
            ) { place ->
                LocationCard(
                    place = place,
                    selected = place.id == uiState.selectedPlaceId,
                    onSelect = { viewModel.selectPlace(place.id) },
                    onDelete = { viewModel.deletePlace(place.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showAddPlaceDialog) {
        AddPlaceDialog(
            onDismiss = { showAddPlaceDialog = false },
            onSave = { place ->
                viewModel.savePlace(place)
                showAddPlaceDialog = false
            },
        )
    }
}

@Composable
private fun LocationsHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = localizedString(R.string.locations_summary, R.string.locations_summary_hebrew),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LocationCard(
    place: SavedPlace,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDetails by remember(place.id) { mutableStateOf(false) }

    InfoCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedLocationName(place.name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (selected) {
                    Spacer(Modifier.height(4.dp))
                    ValuePill(text = localizedString(R.string.locations_selected, R.string.locations_selected_hebrew))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = { showDetails = !showDetails }) {
            Text(
                if (showDetails) {
                    localizedString(R.string.locations_hide_details, R.string.locations_hide_details_hebrew)
                } else {
                    localizedString(R.string.locations_show_details, R.string.locations_show_details_hebrew)
                },
            )
        }
        if (showDetails) {
            Spacer(Modifier.height(6.dp))
            LocationDetail(text = localizedString(R.string.locations_latitude, R.string.locations_latitude_hebrew, place.latitude))
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            LocationDetail(text = localizedString(R.string.locations_longitude, R.string.locations_longitude_hebrew, place.longitude))
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            LocationDetail(text = localizedString(R.string.locations_elevation, R.string.locations_elevation_hebrew, place.elevationMeters))
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            LocationDetail(text = localizedString(R.string.locations_timezone, R.string.locations_timezone_hebrew, place.zoneId.id))
            Spacer(Modifier.height(14.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSelect, enabled = !selected) {
                Text(localizedString(R.string.locations_select, R.string.locations_select_hebrew))
            }
            if (place.id != defaultSavedPlace.id) {
                TextButton(onClick = onDelete) {
                    Text(localizedString(R.string.locations_delete, R.string.locations_delete_hebrew))
                }
            }
        }
    }
}

@Composable
private fun LocationDetail(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AddPlaceDialog(
    onDismiss: () -> Unit,
    onSave: (SavedPlace) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SavedPlace>>(emptyList()) }

    LaunchedEffect(query) {
        hasSearched = query.isNotBlank()
        delay(250L)
        results = searchPlaces(context, query)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localizedString(R.string.locations_add_place, R.string.locations_add_place_hebrew)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = localizedString(R.string.locations_pick_from_results, R.string.locations_pick_from_results_hebrew),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(localizedString(R.string.locations_search_place, R.string.locations_search_place_hebrew)) },
                    singleLine = true,
                )
                if (hasSearched && results.isEmpty()) {
                    Text(
                        text = localizedString(R.string.locations_no_results, R.string.locations_no_results_hebrew),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                results.forEach { place ->
                    OutlinedButton(
                        onClick = { onSave(place) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(place.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(localizedString(R.string.locations_cancel, R.string.locations_cancel_hebrew))
            }
        },
    )
}

private suspend fun searchPlaces(
    context: Context,
    query: String,
): List<SavedPlace> = withContext(Dispatchers.IO) {
    val normalized = query.trim()
    val offlineResults = if (normalized.isBlank()) {
        offlineKnownPlaces.take(12)
    } else {
        offlineKnownPlaces.filter { place ->
            place.name.contains(normalized, ignoreCase = true)
        }
    }

    if (normalized.length < 3 || !Geocoder.isPresent()) {
        return@withContext offlineResults
    }

    @Suppress("DEPRECATION")
    val geocoderResults = Geocoder(context, Locale.getDefault())
        .getFromLocationName(normalized, 8)
        .orEmpty()
        .mapIndexedNotNull { index, address ->
            if (!address.hasLatitude() || !address.hasLongitude()) return@mapIndexedNotNull null
            SavedPlace(
                id = "place_${System.currentTimeMillis()}_$index",
                name = address.bestDisplayName(query),
                latitude = address.latitude,
                longitude = address.longitude,
                elevationMeters = 0.0,
                zoneId = ZoneId.systemDefault(),
            )
        }
    (offlineResults + geocoderResults).distinctBy { place ->
        "${place.name}:${place.latitude}:${place.longitude}"
    }
}

private fun android.location.Address.bestDisplayName(query: String): String =
    listOfNotNull(featureName, locality, adminArea, countryName)
        .distinct()
        .joinToString(", ")
        .ifBlank { query }

@SuppressLint("MissingPermission")
private fun saveCurrentDevicePlace(
    context: Context,
    onPlace: (SavedPlace) -> Unit,
) {
    if (!context.hasLocationPermission()) return

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter(locationManager::isProviderEnabled)
    providers
        .mapNotNull(locationManager::getLastKnownLocation)
        .maxByOrNull(Location::getTime)
        ?.let { location ->
            onPlace(location.toSavedPlace())
            return
        }

    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onPlace(location.toSavedPlace())
            locationManager.removeUpdates(this)
        }
    }
    providers.forEach { provider ->
        locationManager.requestLocationUpdates(provider, 0L, 0f, listener)
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Location.toSavedPlace(): SavedPlace = SavedPlace(
    id = CurrentLocationPlaceId,
    name = "Current location",
    latitude = latitude,
    longitude = longitude,
    elevationMeters = if (hasAltitude()) altitude else 0.0,
    zoneId = ZoneId.systemDefault(),
)
