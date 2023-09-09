package com.baset.googlelens.model

import android.os.Parcelable
import com.google.mlkit.vision.label.ImageLabel
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class ImageLabelModel(
    val uuid: String = UUID.randomUUID().toString(),
    val text: String?,
    val confidence: Float,
    val index: Int
) : Parcelable {
    val formattedConfidence: Int
        get() = (confidence * 100).toInt()

    companion object {
        fun mapFrom(imageLabel: ImageLabel): ImageLabelModel {
            return ImageLabelModel(
                UUID.randomUUID().toString(),
                imageLabel.text,
                imageLabel.confidence,
                imageLabel.index
            )
        }
    }
}