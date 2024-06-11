package com.example.hw3.workers

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hw3.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
//創建通知頻道
private val channel = NotificationChannel("Hw3", "Hw3", NotificationManager.IMPORTANCE_DEFAULT)

//dowork方法執行後台任務
class ImageProcessingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            //從輸入數據獲取Uri
            val imageUri = inputData.getString("image_uri") ?: return@withContext Result.failure()
            val resolver = applicationContext.contentResolver
            resolver.openInputStream(Uri.parse(imageUri)).use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                //將圖片執行黑白化處理
                val processedBitmap = convertToBlackAndWhite(bitmap)
                //將處理後的圖片保存至圖庫
                val outputUri = saveImageToGallery(applicationContext, processedBitmap)
                //顯示處理完成的通知
                showFinishedNotification(outputUri, applicationContext)
                Result.success()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}


//黑白化處理
fun convertToBlackAndWhite(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    //創建一個點陣圖
    val bwBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = bitmap.getPixel(x, y)
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            //取出rgb的亮度套用轉灰階的公式
            val grayLevel = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            val gray = (grayLevel shl 16) + (grayLevel shl 8) + grayLevel
            //設置每個像素點的灰度
            bwBitmap.setPixel(x, y, gray)
        }
    }
    //回傳點陣圖
    return bwBitmap
}

//保存圖片到圖庫
private fun saveImageToGallery(context: Context, bitmap: Bitmap): Uri {
    //使用ContentValues設置圖片屬性
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "outputImage.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            "Pictures/${context.getString(R.string.app_name)}"
        )
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    //插入圖片到系統圖庫
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    //保存圖片
    uri?.let {
        resolver.openOutputStream(it).use { outputStream ->
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }
        //更新圖片狀態
        values.clear()
        //更新圖庫中的圖片信息
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(it, values, null, null)
    }
    //返回保存的uri
    return uri!!
}

//顯示處理完成後的通知
private fun showFinishedNotification(uri: Uri, context: Context) {
    val builder = NotificationCompat.Builder(context, "Hw3")
    val imageIntent = Intent(Intent.ACTION_VIEW)
    imageIntent.setDataAndType(uri, "image/*")
    //創建一個pendingIntent
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        imageIntent,
        PendingIntent.FLAG_IMMUTABLE
    )
    imageIntent.setPackage("com.google.android.apps.photos")
    imageIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    //構件通知
    val notification: Notification =
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("圖片已處理好")
            .setContentText("點擊以開啟")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(NotificationCompat.Action.Builder(0, "觀看圖片", pendingIntent).build())
            .build()

    //獲取系統的manager服務
    val manager =
        context.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
    manager.notify(0, notification)

}