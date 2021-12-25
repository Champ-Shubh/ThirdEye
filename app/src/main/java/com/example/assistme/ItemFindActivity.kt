package com.example.assistme

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.Size
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.airbnb.lottie.LottieAnimationView
import com.google.common.util.concurrent.ListenableFuture
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ItemFindActivity : AppCompatActivity() {

    private lateinit var previewFind: PreviewView
    lateinit var txtFindHeader: TextView
    lateinit var animProgress: LottieAnimationView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var textToSpeech: TextToSpeech
    lateinit var itemAnalyser: ItemAnalyser
    lateinit var itemName: String
    var itemPicID: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_item_find)

        itemName = intent.getStringExtra("itemName").toString()
        itemPicID = intent.getIntExtra("itemPicID", R.drawable.third_eye)

        Log.e("ItemFindActivity", "Inside onCreate ItemFindActivity")

        animProgress = findViewById(R.id.anim_progress)
        txtFindHeader = findViewById(R.id.txt_find_header)
        txtFindHeader.text = resources.getString(R.string.item_find_text, itemName)

        textToSpeech = TextToSpeech(this){
            if(it != TextToSpeech.ERROR){
                textToSpeech.language = Locale.getDefault()
            }
        }

        previewFind = findViewById(R.id.preview_find)
        cameraExecutor = Executors.newSingleThreadExecutor()
        itemAnalyser = ItemAnalyser(this, itemName, txtFindHeader, animProgress, textToSpeech)

        startCameraPreview()
    }

    private fun startCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        },
            ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider){
        val preview: Preview = Preview.Builder().build()
        val builder = CameraSelector.Builder()

        val sharedPreferences = applicationContext.getSharedPreferences("ThirdEyeSharedPref", MODE_PRIVATE)
        val isFrontCamSelected = sharedPreferences.getBoolean("isFrontCamSelected",false)

        if(isFrontCamSelected)
            builder.requireLensFacing(CameraSelector.LENS_FACING_FRONT)
        else
            builder.requireLensFacing(CameraSelector.LENS_FACING_BACK)

        val cameraSelector = builder.build()

        preview.setSurfaceProvider(previewFind.surfaceProvider)

        val imageFrameAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(480, 640))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageFrameAnalysis.setAnalyzer(cameraExecutor, itemAnalyser)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageFrameAnalysis)
        }
        catch (e: Exception){
            Log.e("ItemFindActivity", "Camera Binding failed !!")
        }

    }

    override fun onStop() {
        super.onStop()
        textToSpeech.shutdown()
        cameraExecutor.shutdown()
    }

}