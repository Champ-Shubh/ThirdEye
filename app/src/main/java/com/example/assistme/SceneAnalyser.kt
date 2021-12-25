package com.example.assistme

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.*

class SceneAnalyser(var context: Context, var textToSpeech: TextToSpeech) : ImageAnalysis.Analyzer {

    lateinit var imageLabeler: ImageLabeler
    private var prevItem = ""
    private var excludeList = arrayListOf("Cool","Fun", "Petal")

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {

        if(textToSpeech.isSpeaking){
            image.close()
            return
        }

        val imageBitmap = BitmapUtils.imageToBitmap(image.image!!, image.imageInfo.rotationDegrees)
        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE) //Try removing .enableMultipleObjects() if many objects are getting detected each frame
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
                    labelInImage(imageBitmap, detectedObjects)
                }
                .addOnFailureListener {
                    Log.e("SceneAnalyser", "Error in Object Detection part")
                }
                .addOnCompleteListener {
                    image.close()
                }
    }

    private fun labelInImage(bitmap: Bitmap, objects: List<DetectedObject>) {
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
                        var bestConf = 0.1f
                        var bestConfItem = ""

                        var objectList = ""
                        for (label in labels) {
                            val text = label.text
                            val confidence = label.confidence

                            if (confidence > bestConf) {
                                bestConfItem = text
                                bestConf = confidence
                            }

                            if (confidence >= 0.8)
                                objectList += "..$text|$confidence|.."
                        }

                        if(objectList.isNotEmpty())
                            Log.e("SceneAnalyser", "Detected object are $objectList")

                        Log.e("SceneAnalyser", "Best conf item is ---- $bestConfItem")

                        if(bestConfItem != prevItem && bestConfItem.isNotEmpty() && !excludeList.contains(bestConfItem)) {
                            textToSpeech.speak(bestConfItem, TextToSpeech.QUEUE_ADD, null, "EXPLORE_OBJECT")
                            prevItem = bestConfItem
                        }
                    }
        }
    }

}