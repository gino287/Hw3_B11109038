package com.example.hw3

//import android.graphics.Bitmap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                Button(onClick = {
                    imageUri?.let { uri ->
                        val inputStream: InputStream? =
                            contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        processedBitmap = convertToBlackAndWhite(bitmap)
                        // 你可以将 processedBitmap 显示在屏幕上，或者保存到文件系统
                    }
                }) {
                    Text("Convert to Black and White")
                }
            }
        }
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