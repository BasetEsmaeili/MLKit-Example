package com.baset.googlelens.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FileUtils @Inject constructor(@ApplicationContext private val context: Context) {
    enum class MediaType {
        MediaTypeImage,
        MediaTypeVideo,
        Unknown
    }

    fun getFirstAvailableVideoFrameFromVideo(uri: Uri): Bitmap? {
        kotlin.runCatching {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(context, uri)
            return mediaMetadataRetriever.frameAtTime
        }.onFailure {
            return null
        }
        return null
    }

    fun getMediaType(source: Uri): MediaType {
        val mediaTypeRaw = context.contentResolver.getType(source)
        if (mediaTypeRaw?.startsWith("image") == true)
            return MediaType.MediaTypeImage
        if (mediaTypeRaw?.startsWith("video") == true)
            return MediaType.MediaTypeVideo
        return MediaType.Unknown
    }
}