package com.example.assistme

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.airbnb.lottie.LottieAnimationView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.*
import java.util.concurrent.ExecutorService

class ItemAnalyser(var context: Context, var objectName: String, var txtItemFindHeader: TextView, var lottieAnim: LottieAnimationView, var textToSpeech: TextToSpeech): ImageAnalysis.Analyzer {

    lateinit var imageLabeler: ImageLabeler
    private var detectedFlag = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val imageBitmap = BitmapUtils.imageToBitmap(image.image!!, image.imageInfo.rotationDegrees)
        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()

        val objectDetector = ObjectDetection.getClient(options)

        val imageFrame = InputImage.fromMediaImage(
            image.image!!,
            image.imageInfo.rotationDegrees
        )

        //Perform Object detection
        objectDetector.process(imageFrame)
            .addOnSuccessListener { detectedObjects ->
                //Pass on the list of detected objects to image labeler
                checkInImage(imageBitmap, detectedObjects)
            }
            .addOnFailureListener {
                Log.e("ItemAnalyser", "Error in Item Detection !!")
            }
            .addOnCompleteListener {
                image.close()
            }
    }

    private fun checkInImage(bitmap: Bitmap, objects: List<DetectedObject>) {
        for (obj in objects) {
            val bounds = obj.boundingBox
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                bounds.left,
                bounds.top,
                bounds.width(),
                bounds.height()
            )
            //val croppedBitmap = BitmapUtils.cropRectFromBitmap(bitmap, bounds)

            val image = InputImage.fromBitmap(croppedBitmap, 0)

            //Perform Image Labeling on the cropped part of image
            imageLabeler.process(image)
                .addOnSuccessListener { labels ->
                    for (label in labels){
                        if(label.text.equals(objectName, ignoreCase = true) && !detectedFlag){
                            detectedFlag = true

                            Toast.makeText(context, "$objectName found :)", Toast.LENGTH_SHORT).show()

                            txtItemFindHeader.text = "$objectName found :)"
                            textToSpeech.speak("$objectName found", TextToSpeech.QUEUE_ADD, null, "FOUND_ITEM")

                            if(lottieAnim.isAnimating)
                                lottieAnim.pauseAnimation()

                            lottieAnim.setAnimation(R.raw.tick_successful)
                            lottieAnim.playAnimation()

                            performVibration()
                        }
                    }

                }
        }

    }

    //Helper function to perform vibration
    private fun performVibration(){
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        else
            vibrator.vibrate(200)
    }
}