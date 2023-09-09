package com.baset.googlelens.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.baset.googlelens.model.ImageLabelModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class ImageLabeling(private val context: Context) {

    interface ProcessCallback {
        fun onSuccess(result: List<ImageLabelModel>)
        fun onError(e: Exception)
    }

    private val imageLabeler by lazy { ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS) }

    @ExperimentalGetImage
    fun processImage(imageProxy: ImageProxy, callback: ProcessCallback) {
        val image = imageProxy.image ?: return
        imageLabeler.process(
            InputImage.fromMediaImage(
                image,
                imageProxy.imageInfo.rotationDegrees
            )
        ).addOnSuccessListener {
            callback.onSuccess(it.map { imageLabel -> ImageLabelModel.mapFrom(imageLabel) })
        }.addOnFailureListener {
            callback.onError(it)
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }

    fun processImage(uri: Uri, callback: ProcessCallback) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            callback.onError(e)
            return
        }

        imageLabeler.process(image).addOnSuccessListener {
            callback.onSuccess(it.map { imageLabel -> ImageLabelModel.mapFrom(imageLabel) })
        }.addOnFailureListener {
            callback.onError(it)
        }
    }

    fun processImage(bitmap: Bitmap, callback: ProcessCallback) {
        val image = try {
            InputImage.fromBitmap(bitmap, 0)
        } catch (e: Exception) {
            callback.onError(e)
            return
        }

        imageLabeler.process(image).addOnSuccessListener {
            callback.onSuccess(it.map { imageLabel -> ImageLabelModel.mapFrom(imageLabel) })
        }.addOnFailureListener {
            callback.onError(it)
        }
    }
}