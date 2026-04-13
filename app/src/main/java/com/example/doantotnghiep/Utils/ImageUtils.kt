package com.example.doantotnghiep.Utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    fun compressImage(context: Context, uri: Uri, maxWidth: Int = 1024, maxHeight: Int = 1024, quality: Int = 80): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var inSampleSize = 1
            if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = inSampleSize }
            val compressedStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(compressedStream, null, decodeOptions)
            compressedStream?.close()

            val file = File(context.cacheDir, "temp_img_${System.currentTimeMillis()}.jpg")
            val out = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.flush()
            out.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}