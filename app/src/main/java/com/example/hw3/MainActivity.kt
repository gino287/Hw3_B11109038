package com.example.hw3

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.example.hw3.ui.theme.Hw3Theme
import com.example.hw3.workers.ImageProcessingWorker

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //權限請求，用於請求通知權限
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    //如果權限未被授予，顯示提示訊息，並關閉應用
                    Toast.makeText(
                        this@MainActivity,
                        "需要權限才能使用此程式",
                        Toast.LENGTH_SHORT
                    ).show()

                    this.finishAffinity()
                }
            }
        //發起權限請求
        requestPermissionLauncher.launch(
            Manifest.permission.POST_NOTIFICATIONS
        )

        setContent {
            Hw3Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ImagePicker()//調用用於選擇和顯示圖片的組件
                }
            }
        }
    }


    //選擇圖片和處理圖片
    @Composable
    fun ImagePicker() {
        //儲存選擇的圖片uri狀態
        var imageUri by remember { mutableStateOf<Uri?>(null) }
        //儲存處理後圖片的點陣圖狀態
        var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
        //圖片選擇
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            imageUri = uri
            processedBitmap = null  // 選擇新圖片時，清理之前的處理結果
        }

        Box(modifier = Modifier.fillMaxSize()) {

            // 使用 AsyncImage 顯示選中的圖片
            imageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
            //底部按鈕行，有選擇圖片及處理圖片
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),//增加底部距離
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                // 打開相簿選擇圖片的按鈕
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text(
                        text = "選擇圖片",
                        fontSize = 20.sp,
                    )
                }

                //處理圖片的按鈕，啟動後於後台workers處理工作
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        imageUri?.let { uri ->
                            val data: Data = Data.Builder().putString("image_uri", uri.toString())
                                .build()
                            val request =
                                OneTimeWorkRequest.Builder(ImageProcessingWorker::class.java)
                                    .setInputData(data)
                                    .build()
                            WorkManager.getInstance(applicationContext).enqueue(request)
                        }
                    }) {
                    Text(
                        "進行黑白化處理",
                        fontSize = 20.sp,
                    )
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        Hw3Theme {
            ImagePicker()
        }
    }
}