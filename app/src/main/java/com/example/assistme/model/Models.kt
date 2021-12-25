package com.example.assistme.model

class Models {

    companion object{

        val FACENET = ModelInfo(
                "FaceNet" ,
                "facenet.tflite" ,
                0.4f ,
                10f ,
                128 ,
                160
        )

        val FACENET_QUANTIZED = ModelInfo(
                "FaceNet Quantized" ,
                "facenet_int_quantized.tflite" ,
                0.4f ,
                10f ,
                128 ,
                160
        )
    }
}