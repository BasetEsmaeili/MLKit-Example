package com.baset.googlelens.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.baset.googlelens.R
import com.baset.googlelens.databinding.ActivityMainBinding
import com.baset.googlelens.util.showSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        private const val CAMERA_PERMISSION = android.Manifest.permission.CAMERA
        private const val IMAGE_QUALITY_PERCENT = 100
    }

    private val imageCapture by lazy(LazyThreadSafetyMode.NONE) {
        ImageCapture.Builder()
            .setJpegQuality(IMAGE_QUALITY_PERCENT)
            .setFlashMode(FLASH_MODE_AUTO)
            .build()
    }
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()
    private val cameraProviderFuture by lazy(LazyThreadSafetyMode.NONE) {
        ProcessCameraProvider.getInstance(
            this
        )
    }
    private val permissionRequestActivityResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                viewModel.onCameraPermissionDenied()
                return@registerForActivityResult
            }
            viewModel.onCameraPermissionGranted()
        }

    private val mediaPickerActivityResult =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, flag)
                viewModel.onMediaFromPickerReceived(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }
        observe()
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { event ->
                    when (val state = event.getContentIfNotHandled()) {
                        is UiState.Error -> {
                            binding.progressbar.isVisible = false
                            binding.root.showSnackBar(state.message)
                        }

                        is UiState.Loading -> {
                            binding.progressbar.isVisible = true
                        }

                        is UiState.HideLoading -> {
                            binding.progressbar.isVisible = false
                        }

                        is UiState.PermissionDeniedError -> {
                            binding.progressbar.isVisible = false
                            binding.root.showSnackBar(stringRes = R.string.error_camera_permission_required,
                                duration = Snackbar.LENGTH_INDEFINITE,
                                actionStringRes = R.string.allow,
                                actionClickListener = {
                                    onAllowActionClick()
                                })
                        }

                        is UiState.LoadCameraPreview -> {
                            binding.progressbar.isVisible = false
                            setupListeners()
                            startCameraPreview()
                        }

                        is UiState.ShowDetectedObject -> {
                            ProcessResultBottomSheet.newInstance(
                                bundleOf(
                                    ProcessResultBottomSheet.PROCESS_RESULTS to state.objects.toTypedArray()
                                )
                            ).show(supportFragmentManager, ProcessResultBottomSheet.TAG)
                        }

                        else -> return@collect
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionsVisibility.collect { event ->
                    event.getContentIfNotHandled()?.let {
                        binding.actionsContent.isInvisible = it.not()
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        with(binding) {
            btnSearch.setOnClickListener {
                takePicture()
            }
            btnPickFromGallery.setOnClickListener {
                mediaPickerActivityResult.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            }
        }
    }

    private fun takePicture() {
        viewModel.onStartCaptureImage()
        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                @androidx.camera.core.ExperimentalGetImage
                override fun onCaptureSuccess(image: ImageProxy) {
                    viewModel.onImageCaptured(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModel.onImageCaptureError(exception)
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!viewModel.isCameraPreviewHasInitialized) {
            cameraProviderFuture.cancel(true)
        }
    }

    override fun onResume() {
        super.onResume()
        permissionRequestActivityResult.launch(CAMERA_PERMISSION)
    }

    private fun onAllowActionClick() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION)) {
            permissionRequestActivityResult.launch(CAMERA_PERMISSION)
            return
        }
        kotlin.runCatching {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        }.onFailure {
            startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
        }
    }

    private fun startCameraPreview() {
        if (viewModel.isCameraPreviewHasInitialized) {
            viewModel.onCameraPreviewLoaded()
            return
        }

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
            kotlin.runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )
            }.onFailure { error ->
                viewModel.onLoadPreviewError(error)
                return@addListener
            }
            if (cameraProviderFuture.isDone) {
                viewModel.onCameraPreviewLoaded()
            }
        }, ContextCompat.getMainExecutor(this))
    }
}