package com.baset.googlelens.view

import android.os.Build
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baset.googlelens.model.ImageLabelModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProcessResultUiState {
    data object Init : ProcessResultUiState()
    data object NoResultFound : ProcessResultUiState()
    data object ShowResults : ProcessResultUiState()
    data class SearchUrl(val url: String) : ProcessResultUiState()
}

@HiltViewModel
class ProcessResultBottomSheetViewModel @Inject constructor() : ViewModel() {
    private val _processResultStateFlow =
        MutableStateFlow<MutableList<ImageLabelModel>?>(mutableListOf())
    val processResultStateFlow = _processResultStateFlow.asStateFlow()
    private val _uiState = MutableStateFlow<ProcessResultUiState>(ProcessResultUiState.Init)
    val uiState = _uiState.asStateFlow()

    fun extractArguments(arguments: Bundle?) {
        val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArray(
                ProcessResultBottomSheet.PROCESS_RESULTS,
                ImageLabelModel::class.java
            )
        } else {
            arguments?.getParcelableArray(ProcessResultBottomSheet.PROCESS_RESULTS) as Array<out ImageLabelModel>
        }
        if (results.isNullOrEmpty()) {
            _uiState.value = ProcessResultUiState.NoResultFound
            return
        }
        _uiState.value = ProcessResultUiState.ShowResults
        viewModelScope.launch(Dispatchers.Default) {
            results.sortByDescending { it.formattedConfidence }
            _processResultStateFlow.value = results.toMutableList()
        }
    }

    fun onResultItemClick(model: ImageLabelModel) {
        _uiState.value =
            ProcessResultUiState.SearchUrl("https://www.google.com/search?q=${model.text}")
    }
}