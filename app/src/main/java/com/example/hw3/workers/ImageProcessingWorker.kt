package com.example.hw3.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hw3.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageProcessingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val imageUri = inputData.getString("image_uri") ?: return@withContext Result.failure()
            val resolver = applicationContext.contentResolver
            resolver.openInputStream(Uri.parse(imageUri)).use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val processedBitmap = convertToBlackAndWhite(bitmap)
                val outputUri = saveBitmapToFile(processedBitmap, applicationContext)
                showFinishedNotification(outputUri, applicationContext)
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("ImageProcessingWorker", "Work failed", e)
            Result.failure()
        }
    }
}


//黑白化
fun convertToBlackAndWhite(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    val bwBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = bitmap.getPixel(x, y)
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            val grayLevel = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            val gray = (grayLevel shl 16) + (grayLevel shl 8) + grayLevel
            bwBitmap.setPixel(x, y, gray)
        }
    }
    return bwBitmap
}


private fun saveBitmapToFile(bitmap: Bitmap, context: Context): Uri {
    val filename = "processed_image.png"
    val outputDir = context.cacheDir
    val outputFile = File(outputDir, filename)

    FileOutputStream(outputFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    return Uri.fromFile(outputFile)
}

private fun showFinishedNotification(uri: Uri, context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notificationId = 1

    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = uri
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    val notification = NotificationCompat.Builder(context, "channel_id")
        .setContentTitle("Image Processing Complete")
        .setContentText("Tap to view the processed image")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(pendingIntent)
        .addAction(NotificationCompat.Action.Builder(0, "View Image", pendingIntent).build())
        .setAutoCancel(true)
        .build()

    notificationManager.notify(notificationId, notification)
}