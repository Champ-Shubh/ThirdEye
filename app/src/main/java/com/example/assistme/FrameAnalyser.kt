package com.example.assistme

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.assistme.database.Person
import com.example.assistme.fragment.FaceFragment
import com.example.assistme.model.FaceNetModel
import com.example.assistme.model.Models
import com.google.android.material.textfield.TextInputLayout
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt
import kotlin.math.pow

class FrameAnalyser(
        private var context: Context,
        private var faceFragment: FaceFragment,
        private var textToSpeech: TextToSpeech
) : ImageAnalysis.Analyzer {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .build()
    private val detector = FaceDetection.getClient(realTimeOpts)

    // You may the change the models here.
    // Default is Models.FACENET ; Quantized models are faster
    private val model = FaceNetModel(context, Models.FACENET_QUANTIZED, false)  //useGPU is throwing error so = false

    private val nameScoreHashmap = HashMap<String, ArrayList<Float>>()
    private var subject = FloatArray(model.embeddingDim)

    private var prevPerson = ""

    // Used to determine whether the incoming frame should be dropped or processed.
    private var isProcessing = false

    //Used to determine whether a pop up is already active or not
    private var isPopUpActive = false

    // Store the face embeddings in a ( String , FloatArray ) ArrayList.
    // Where String -> name of the person and FloatArray -> Embedding of the face.
    var faceList = ArrayList<Pair<String, FloatArray>>()

    // Use any one of the two metrics, "cosine" or "l2"
    private val metricToBeUsed = "cosine"

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {

        // If the previous frame is still being processed or input popup is active, then skip this frame
        if (isProcessing || isPopUpActive) {
            //Log.e("FrameAnalyser", "isProcessing = $isProcessing")
            image.close()
            return
        } else {
            isProcessing = true

            // Rotated bitmap for the FaceNet model
            val frameBitmap =
                BitmapUtils.imageToBitmap(image.image!!, image.imageInfo.rotationDegrees)

            val inputImage =
                InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)

            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    CoroutineScope(Dispatchers.Default).launch {
                        runModel(faces, frameBitmap)
                    }
                }
                .addOnCompleteListener {
                    image.close()
                }
        }
    }

    private suspend fun runModel(faces: List<Face>, cameraFrameBitmap: Bitmap) {
        withContext(Dispatchers.Default) {
            for (face in faces) {
                try {
                    // Crop the frame using face.boundingBox.
                    // Convert the cropped Bitmap to a ByteBuffer.
                    // Finally, feed the ByteBuffer to the FaceNet model.
                    val croppedBitmap =
                        BitmapUtils.cropRectFromBitmap(cameraFrameBitmap, face.boundingBox)
                    subject = model.getFaceEmbedding(croppedBitmap)

                    if(faceList.size == 0){
                        //Add the first face to DB
                        createInputPopUp(subject)
                    }

                    // Perform clustering ( grouping )
                    // Store the clusters in a HashMap. Here, the key would represent the 'name'
                    // of that cluster and ArrayList<Float> would represent the collection of all
                    // L2 norms/ cosine distances.
                    for (i in 0 until faceList.size) {
                        // If this cluster ( i.e an ArrayList with a specific key ) does not exist,
                        // initialize a new one.
                        if (nameScoreHashmap[faceList[i].first] == null) {
                            // Compute the L2 norm and then append it to the ArrayList.
                            val p = ArrayList<Float>()
                            if (metricToBeUsed == "cosine") {
                                p.add(cosineSimilarity(subject, faceList[i].second))
                            } else {
                                p.add(L2Norm(subject, faceList[i].second))
                            }
                            nameScoreHashmap[faceList[i].first] = p
                        }
                        // If this cluster exists, append the L2 norm/cosine score to it.
                        else {
                            if (metricToBeUsed == "cosine") {
                                nameScoreHashmap[faceList[i].first]?.add(
                                    cosineSimilarity(
                                        subject,
                                        faceList[i].second
                                    )
                                )
                            } else {
                                nameScoreHashmap[faceList[i].first]?.add(
                                    L2Norm(
                                        subject,
                                        faceList[i].second
                                    )
                                )
                            }
                        }
                    }

                    // Compute the average of all scores norms for each cluster.
                    val avgScores =
                        nameScoreHashmap.values.map { scores -> scores.toFloatArray().average() }
                    //Logger.log( "Average score for each user : $nameScoreHashmap" )
                    Log.e("FrameAnalyser", "Average score for each user : $nameScoreHashmap")

                    val names = nameScoreHashmap.keys.toTypedArray()
                    nameScoreHashmap.clear()

                    // Calculate the minimum L2 distance from the stored average L2 norms.
                    var bestScoreUserName: String = ""
                        if (metricToBeUsed == "cosine") {
                        // In case of cosine similarity, choose the highest value.
                        if (avgScores.maxOrNull()!! > model.model.cosineThreshold) {
                            bestScoreUserName = names[avgScores.indexOf(avgScores.maxOrNull()!!)]
                        } else {
                            bestScoreUserName = "Unknown"
                            //Add this unknown to database
                            createInputPopUp(newEmbedding = subject)
                        }
                    } else {
                        // In case of L2 norm, choose the lowest value.
                        if (avgScores.minOrNull()!! > model.model.l2Threshold) {
                            bestScoreUserName = "Unknown"
                            //Add this unknown to database
                            createInputPopUp(newEmbedding = subject)
                        } else {
                            bestScoreUserName = names[avgScores.indexOf(avgScores.minOrNull()!!)]
                        }
                    }

                    var relationToUser = bestScoreUserName.substring(bestScoreUserName.indexOf('#')+1)
                    bestScoreUserName = bestScoreUserName.substring(0, bestScoreUserName.indexOf('#'))

                    //Ignore this
                    if(relationToUser.equals("Myself",ignoreCase = true))
                        relationToUser = "Self"

                    if(bestScoreUserName.isNotEmpty() && !bestScoreUserName.equals("FakeMe") && !bestScoreUserName.equals(prevPerson, ignoreCase = true)) {
                        //Here comes the result of the face recognition
                        Log.e("FrameAnalyser", "Person identified as $bestScoreUserName")
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Person is $bestScoreUserName, your $relationToUser", Toast.LENGTH_SHORT).show()
                        }

                        textToSpeech.speak("Person is your $relationToUser, $bestScoreUserName", TextToSpeech.QUEUE_ADD, null, "PERSON_DETECTED")
                        prevPerson = bestScoreUserName
                    }
                } catch (e: Exception) {
                    // If any exception occurs with this box and continue with the next boxes.
                    Log.e("FrameAnalyser", "Exception in FrameAnalyser : ${e.message}")
                    continue
                }
            }
            withContext(Dispatchers.Main) {
                isProcessing = false
            }
        }
    }

    // Compute the L2 norm of ( x2 - x1 )
    private fun L2Norm(x1: FloatArray, x2: FloatArray): Float {
        return sqrt(x1.mapIndexed { i, xi -> (xi - x2[i]).pow(2) }.sum())
    }


    // Compute the cosine of the angle between x1 and x2.
    private fun cosineSimilarity(x1: FloatArray, x2: FloatArray): Float {
        val mag1 = sqrt(x1.map { it * it }.sum())
        val mag2 = sqrt(x2.map { it * it }.sum())
        val dot = x1.mapIndexed { i, xi -> xi * x2[i] }.sum()
        return dot / (mag1 * mag2)
    }


    private class InsertFaceIntoDB(val newPerson: Person, val faceFragmentContext: FaceFragment) : AsyncTask<Void, Void, Boolean>(){
        override fun doInBackground(vararg p0: Void?): Boolean {
            faceFragmentContext.personDatabase!!.personDao().insert(newPerson)
            return true
        }

    }


    private fun createInputPopUp(newEmbedding: FloatArray) {
        //For debugging
        Log.e("FrameAnalyser", "Inside createInputPopUp")
        isPopUpActive = true

        val layoutInflater = LayoutInflater.from(context)
        val customView = layoutInflater.inflate(R.layout.popup_input_dialog, null)

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Unknown Person")

        //val nameInput = EditText(context)
        //nameInput.hint = "Format :- Name#Relation"

        builder.setView(customView)
        val nameInput = customView.findViewById<TextInputLayout>(R.id.et_name_input)
        val relationInput = customView.findViewById<TextInputLayout>(R.id.et_relation_input)

        builder.setPositiveButton("Ok") { _, _ ->
            isPopUpActive = false

            /*
            val input = nameInput.text.toString()
            val name = input.substring(0, input.indexOf('#'))
            val relation = input.substring(input.indexOf('#') + 1)
             */

            val name = nameInput.editText!!.text.toString()
            val relation = relationInput.editText!!.text.toString()

            faceList.add(Pair(name, subject))   //Update faceList while also inserting into DB

            val newPerson = Person(newEmbedding, name, relation)

            if (!InsertFaceIntoDB(newPerson, faceFragment).execute().get())
                Log.e("FrameAnalyser", "Error inserting into DB")
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            Log.e("FrameAnalyser", "Clicked CANCEL onCreateInputPopUp")
            isPopUpActive = false
            dialog.cancel()
        }

        Handler(Looper.getMainLooper()).post {
            builder.create()
            builder.show()
        }

        //builder.create()
        //builder.show()
    }

    
}