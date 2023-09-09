package com.baset.googlelens.view

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.baset.googlelens.R
import com.baset.googlelens.databinding.DialogProccessResultBinding
import com.baset.googlelens.util.MarginItemDecoration
import com.baset.googlelens.util.extractDimenPixelSize
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class ProcessResultBottomSheet : BottomSheetDialogFragment(R.layout.dialog_proccess_result) {
    companion object {
        const val TAG = "ProcessResultBottomSheet"
        const val PROCESS_RESULTS = "processResults"

        fun newInstance(bundle: Bundle? = null): ProcessResultBottomSheet {
            return ProcessResultBottomSheet().apply {
                arguments = bundle
            }
        }
    }

    private var _binding: DialogProccessResultBinding? = null
    private val binding: DialogProccessResultBinding
        get() = _binding!!
    private val viewModel by viewModels<ProcessResultBottomSheetViewModel>()
    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ProcessResultsAdapter {
            viewModel.onResultItemClick(it)
        }
    }
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetFrameLayout =
                (dialogInterface as BottomSheetDialog).findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheetFrameLayout?.let { fl ->
                bottomSheetBehavior = BottomSheetBehavior.from(fl)
                bottomSheetBehavior?.isHideable = true
            }
        }
        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.extractArguments(arguments)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = DialogProccessResultBinding.bind(view)
        binding.rvProcessResults.adapter = adapter
        binding.rvProcessResults.addItemDecoration(
            MarginItemDecoration(
                0,
                extractDimenPixelSize(R.dimen.margin_4),
                extractDimenPixelSize(R.dimen.margin_4),
                0
            )
        )
        binding.tvName.movementMethod = LinkMovementMethod.getInstance()
        observe()
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    when (it) {
                        ProcessResultUiState.NoResultFound -> {
                            binding.tvEmptyView.isVisible = true
                            binding.rvProcessResults.isVisible = false
                        }

                        ProcessResultUiState.ShowResults -> {
                            binding.tvEmptyView.isVisible = false
                            binding.rvProcessResults.isVisible = true
                        }

                        is ProcessResultUiState.SearchUrl -> {
                            startActivity(Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(it.url)
                            })
                        }

                        else -> return@collect
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.processResultStateFlow.collectLatest {
                    adapter.submitList(it)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}