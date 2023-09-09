package com.baset.googlelens.view

import android.net.Uri
import androidx.annotation.StringRes
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baset.googlelens.R
import com.baset.googlelens.model.ImageLabelModel
import com.baset.googlelens.util.Event
import com.baset.googlelens.util.FileUtils
import com.baset.googlelens.util.ImageLabeling
import com.baset.googlelens.util.ModuleInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiState {
    data object Nothing : UiState()
    data class Error(val message: String? = null, @StringRes val messageResourceId: Int? = null) :
        UiState()

    data object PermissionDeniedError : UiState()

    data object LoadCameraPreview : UiState()

    data object Loading : UiState()

    data object HideLoading : UiState()

    data class ShowDetectedObject(val objects: List<ImageLabelModel>) : UiState()
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val imageLabeling: ImageLabeling,
    private val moduleInstaller: ModuleInstaller,
    private val fileUtils: FileUtils
) : ViewModel() {

    var isCameraPreviewHasInitialized = false
        private set
    private val _actionsVisibility = MutableStateFlow(Event(false))
    val actionsVisibility = _actionsVisibility.asStateFlow()
    private val _uiState = MutableStateFlow<Event<UiState>>(Event(UiState.Nothing))
    val uiState = _uiState.asStateFlow()

    private val imageLabelingCallback = object : ImageLabeling.ProcessCallback {
        override fun onSuccess(result: List<ImageLabelModel>) {
            showDetectedObjects(result)
        }

        override fun onError(e: Exception) {
            _uiState.value = Event(UiState.HideLoading)
            _actionsVisibility.value = Event(true)
            _uiState.value = Event(UiState.Error(e.message))
        }

    }

    fun onCameraPreviewLoaded() {
        isCameraPreviewHasInitialized = true
        _uiState.value = Event(UiState.HideLoading)
        _actionsVisibility.value = Event(true)
        moduleInstaller.installTfLiteModule()
    }

    fun onCameraPermissionGranted() {
        if (isCameraPreviewHasInitialized) {
            return
        }
        _uiState.value = Event(UiState.Loading)
        _uiState.value = Event(UiState.LoadCameraPreview)
    }

    fun onCameraPermissionDenied() {
        _uiState.value = Event(UiState.PermissionDeniedError)
    }

    fun onLoadPreviewError(throwable: Throwable) {
        _actionsVisibility.value = Event(false)
        _uiState.value = Event(UiState.Error(throwable.message))
    }

    fun onMediaFromPickerReceived(uri: Uri) {
        _uiState.value = Event(UiState.Loading)
        _actionsVisibility.value = Event(false)
        viewModelScope.launch(Dispatchers.Default) {
            when (fileUtils.getMediaType(uri)) {
                FileUtils.MediaType.MediaTypeImage -> {
                    imageLabeling.processImage(uri, imageLabelingCallback)
                }

                FileUtils.MediaType.MediaTypeVideo -> {
                    fileUtils.getFirstAvailableVideoFrameFromVideo(uri)?.let {
                        imageLabeling.processImage(it, imageLabelingCallback)
                    } ?: kotlin.run {
                        _uiState.value = Event(UiState.HideLoading)
                        _actionsVisibility.value = Event(true)
                        _uiState.value =
                            Event(UiState.Error(messageResourceId = R.string.error_process_video))
                    }
                }

                FileUtils.MediaType.Unknown -> {
                    _uiState.value = Event(UiState.HideLoading)
                    _actionsVisibility.value = Event(true)
                    _uiState.value =
                        Event(UiState.Error(messageResourceId = R.string.error_unknown_file))
                }
            }
        }
    }

    @ExperimentalGetImage
    fun onImageCaptured(imageProxy: ImageProxy) {
        imageLabeling.processImage(imageProxy, object : ImageLabeling.ProcessCallback {
            override fun onSuccess(result: List<ImageLabelModel>) {
                showDetectedObjects(result)
            }

            override fun onError(e: Exception) {
                _uiState.value = Event(UiState.HideLoading)
                _actionsVisibility.value = Event(true)
                _uiState.value = Event(UiState.Error(e.message))
            }
        })
    }

    private fun showDetectedObjects(imageLabels: List<ImageLabelModel>) {
        _uiState.value = Event(UiState.HideLoading)
        _actionsVisibility.value = Event(true)
        _uiState.value = Event(UiState.ShowDetectedObject(imageLabels))
    }

    fun onImageCaptureError(exception: ImageCaptureException) {
        _uiState.value = Event(UiState.Error(exception.message))
    }

    fun onStartCaptureImage() {
        _uiState.value = Event(UiState.Loading)
        _actionsVisibility.value = Event(false)
    }
}