package com.example.assistme.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.AsyncTask
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.assistme.FrameAnalyser
import com.example.assistme.MainAppActivity
import com.example.assistme.R
import com.example.assistme.database.Person
import com.example.assistme.database.PersonDatabase
import com.example.assistme.databinding.FragmentFaceBinding
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


/**
 * A simple [Fragment] subclass.
 * Use the [FaceFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FaceFragment : Fragment() {

    lateinit var previewView: View
    lateinit var frameAnalyser: FrameAnalyser
    private var _binding: FragmentFaceBinding? = null
    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    lateinit var cameraExecutor: ExecutorService
    private lateinit var textToSpeech: TextToSpeech
    var personDatabase: PersonDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    @InternalCoroutinesApi
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_face, container, false)

        previewView = view.findViewById(R.id.previewView)
        _binding = FragmentFaceBinding.bind(view)
        cameraExecutor = Executors.newSingleThreadExecutor()

        Log.e("FaceFragment", "onCreateView FaceFragment")

        textToSpeech = TextToSpeech(activity as Context){
            if(it != TextToSpeech.ERROR)
                textToSpeech.language = Locale.getDefault()
        }

        // Initialize frame analyser here
        frameAnalyser = FrameAnalyser(activity as Context, this, textToSpeech)

        personDatabase = PersonDatabase.getDatabase(activity as Context)!!
        frameAnalyser.faceList = loadKnownFacesData()

        startCameraPreview()

        return view
    }

    private fun startCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(activity as Context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        },
                ContextCompat.getMainExecutor(activity as Context))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder().build()
        val builder = CameraSelector.Builder()

        val sharedPreferences = requireActivity().applicationContext.getSharedPreferences("ThirdEyeSharedPref", MODE_PRIVATE)
        val isFrontCamSelected = sharedPreferences.getBoolean("isFrontCamSelected",false)

        if(isFrontCamSelected)
            builder.requireLensFacing(CameraSelector.LENS_FACING_FRONT)
        else
            builder.requireLensFacing(CameraSelector.LENS_FACING_BACK)

        val cameraSelector = builder.build()

        preview.setSurfaceProvider(_binding!!.previewView.surfaceProvider)

        val imageFrameAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(480, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
        imageFrameAnalysis.setAnalyzer(cameraExecutor, frameAnalyser)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageFrameAnalysis)
        }
        catch (e: Exception){
            Log.e("FaceFragment", "Camera Binding failed !!")
        }

    }

    //Helper function to load known faces data from the DB
    private fun loadKnownFacesData() : ArrayList<Pair<String, FloatArray>> {
        val knownFaces = GetDataFromDB(this).execute().get()

        val knownList = ArrayList<Pair<String, FloatArray>>()
        for(p in knownFaces){
            val embedding = p.faceEmbedding
            val nameAndRelation = p.personName + "#" + p.relationToUser

            knownList.add(Pair(nameAndRelation,embedding))
        }

        Log.e("FaceFragment", "Known faces loaded successfully")

        return knownList
    }

    private class GetDataFromDB(var context: FaceFragment) : AsyncTask<Void, Void, List<Person>>(){
        @SuppressLint("UseRequireInsteadOfGet")
        override fun doInBackground(vararg p0: Void?): List<Person> {
            return context.personDatabase!!.personDao().getAll()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        textToSpeech.stop()
        textToSpeech.shutdown()
        cameraExecutor.shutdown()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         */
        @JvmStatic
        fun newInstance() =
                FaceFragment().apply {
                    arguments = Bundle().apply {

                    }
                }
    }
}