package com.example.assistme.fragment

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.assistme.R
import com.example.assistme.SceneAnalyser
import com.example.assistme.databinding.FragmentExploreBinding
import com.google.common.util.concurrent.ListenableFuture
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * A simple [Fragment] subclass.
 * Use the [ExploreFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExploreFragment : Fragment() {

    private lateinit var previewExplore: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var sceneAnalyser: SceneAnalyser
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var textToSpeech: TextToSpeech
    private var _binding: FragmentExploreBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_explore, container, false)

        previewExplore = view.findViewById(R.id.previewExplore)
        _binding = FragmentExploreBinding.bind(view)

        textToSpeech = TextToSpeech(activity as Context){
            if(it != TextToSpeech.ERROR)
                textToSpeech.language = Locale.getDefault()
        }

        val speechListener = object : UtteranceProgressListener() {
            override fun onStart(p0: String?) {
                Log.v("ExploreFragment", "Started Speech Progress Listener")
            }

            override fun onDone(p0: String?) {
                Log.v("ExploreFragment", "onDone Speech Progress Listener")
            }

            override fun onError(p0: String?) {
                Log.e("ExploreFragment", "Error inside Speech Progress Listener")
            }

        }
        textToSpeech.setOnUtteranceProgressListener(speechListener)

        sceneAnalyser = SceneAnalyser(activity as Context, textToSpeech)
        cameraExecutor = Executors.newSingleThreadExecutor()

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

    private fun bindPreview(cameraProvider: ProcessCameraProvider){
        val preview: Preview = Preview.Builder().build()
        val builder = CameraSelector.Builder()

        val sharedPreferences = requireActivity().applicationContext.getSharedPreferences("ThirdEyeSharedPref", Context.MODE_PRIVATE)
        val isFrontCamSelected = sharedPreferences.getBoolean("isFrontCamSelected",false)

        if(isFrontCamSelected)
            builder.requireLensFacing(CameraSelector.LENS_FACING_FRONT)
        else
            builder.requireLensFacing(CameraSelector.LENS_FACING_BACK)

        val cameraSelector = builder.build()

        preview.setSurfaceProvider(_binding!!.previewExplore.surfaceProvider)

        val imageFrameAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(480, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

        imageFrameAnalysis.setAnalyzer(cameraExecutor, sceneAnalyser)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageFrameAnalysis)
        }
        catch (e: Exception){
            Log.e("ExploreFragment", "Camera Binding failed !!")
        }

    }

    override fun onStop() {
        super.onStop()
        cameraExecutor.shutdown()

        textToSpeech.stop()
        textToSpeech.shutdown()

        Log.e("ExploreFragment", "onStop ExploreFragment")
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         *
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ExploreFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}