package com.baset.googlelens.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.baset.googlelens.databinding.ItemProcessResultBinding
import com.baset.googlelens.model.ImageLabelModel

class ProcessResultsAdapter(private val listener: ProcessResultListener) :
    ListAdapter<ImageLabelModel, ProcessResultsAdapter.ProcessResultViewHolder>(DiffUtilCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcessResultViewHolder {
        return ProcessResultViewHolder(
            ItemProcessResultBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            listener
        )
    }

    override fun onBindViewHolder(holder: ProcessResultViewHolder, position: Int) {
        holder.onBind(getItem(holder.adapterPosition))
    }

    class ProcessResultViewHolder(
        private val binding: ItemProcessResultBinding,
        private val listener: ProcessResultListener
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(model: ImageLabelModel) {

            binding.root.setOnClickListener {
                listener.onResultItemClick(model)
            }

            binding.root.text = buildString {
                append(model.text)
                append(" ")
                append(model.formattedConfidence)
                append("%")
            }
        }
    }

    private class DiffUtilCallback : DiffUtil.ItemCallback<ImageLabelModel>() {
        override fun areItemsTheSame(oldItem: ImageLabelModel, newItem: ImageLabelModel): Boolean {
            return oldItem.uuid == newItem.uuid
        }

        override fun areContentsTheSame(
            oldItem: ImageLabelModel,
            newItem: ImageLabelModel
        ): Boolean {
            return oldItem == newItem
        }

    }

    fun interface ProcessResultListener {
        fun onResultItemClick(model: ImageLabelModel)
    }
}