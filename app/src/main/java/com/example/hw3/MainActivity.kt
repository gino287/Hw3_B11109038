package com.example.hw3

import android.Manifest
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
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.hw3.ui.theme.Hw3Theme
import java.io.InputStream

class MainActivity : ComponentActivity() {
    private val channel = NotificationChannel("Hw3", "Hw3", NotificationManager.IMPORTANCE_DEFAULT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    Toast.makeText(
                        this@MainActivity,
                        "需要權限才能使用此程式",
                        Toast.LENGTH_SHORT
                    ).show()

                    this.finishAffinity()
                }
            }
        requestPermissionLauncher.launch(
            Manifest.permission.POST_NOTIFICATIONS
        )

        setContent {
            Hw3Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ImagePicker()
                }
            }
        }
    }


    //讀取圖片
    @Composable
    fun ImagePicker() {
        var imageUri by remember { mutableStateOf<Uri?>(null) }
        var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            imageUri = uri
            processedBitmap = null  // 选择新图片时，清除之前的处理结果
        }

        Box(modifier = Modifier.fillMaxSize()) {

            // 使用 AsyncImage 显示选中的图片
            if (processedBitmap != null) {
                Image(
                    bitmap = processedBitmap!!.asImageBitmap(),
                    contentDescription = "Processed Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            } else {
                imageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),//增加底部距離
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 按钮用于打开相册
                Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text("Select Image from Gallery")
                }

                // 添加一个按钮用于黑白化处理


                val builder = Notification.Builder(this@MainActivity, "Hw3")
                Button(onClick = {
                    imageUri?.let { uri ->
                        val inputStream: InputStream? =
                            contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        processedBitmap = convertToBlackAndWhite(bitmap)
//                        // 你可以将 processedBitmap 显示在屏幕上，或者保存到文件系统
//                        val imageIntentUri =
//                            FileProvider.getUriForFile(
//                                this@MainActivity,
//                                this@MainActivity.applicationContext.packageName + ".provider",
//                                saveBitmapToFile(this@MainActivity, processedBitmap!!)
//                            )
                        val imageIntentUri = saveImageToGallery(this@MainActivity,
                            processedBitmap!!)
                        val imageIntent = Intent(Intent.ACTION_VIEW)
                        imageIntent.setDataAndType(imageIntentUri, "image/*")
                        val pendingIntent = PendingIntent.getActivity(
                            this@MainActivity,
                            0,
                            imageIntent,
                            PendingIntent.FLAG_IMMUTABLE
                        )
                        imageIntent.setPackage("com.google.android.apps.photos")
                        imageIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                        runOnUiThread {
                            val notification: Notification =
                                builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                                    .setContentTitle("Hw3")
                                    .setContentText("圖片已處理好，點擊以開啟")
                                    .setAutoCancel(true)
                                    .setContentIntent(pendingIntent)
                                    .build()


                            val manager =
                                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                            manager.createNotificationChannel(channel)
                            manager.notify(0, notification)
                        }
                    }
                }) {
                    Text("Convert to Black and White")
                }
            }
        }
    }

    //儲存圖片檔案

    private fun saveImageToGallery(context: Context, bitmap: Bitmap,) :Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "RRRRRRRR.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/${context.getString(R.string.app_name)}"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(it, values, null, null)
        }

        return uri!!
    }

    private fun convertToBlackAndWhite(bitmap: Bitmap): Bitmap {
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

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        Hw3Theme {
            ImagePicker()
        }
    }
}