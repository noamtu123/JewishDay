package com.turel.jewishdaynext.feature.mizrach

import androidx.lifecycle.ViewModel
import com.turel.jewishdaynext.data.JewishDayRepository
import com.turel.jewishdaynext.model.MizrachInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MizrachUiState(
    val mizrachInfo: MizrachInfo,
)

@HiltViewModel
class MizrachViewModel @Inject constructor(
    jewishDayRepository: JewishDayRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MizrachUiState(
        mizrachInfo = jewishDayRepository.getMizrach(),
    ))
    val uiState: StateFlow<MizrachUiState> = _uiState.asStateFlow()
}
