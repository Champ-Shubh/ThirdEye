package com.example.assistme.database

import androidx.room.TypeConverter

class ConvertersDB {

    @TypeConverter
    fun fromFloatArray(inputArray: FloatArray?): String?{
        var output: String? = null
        if (inputArray != null) {
            output = inputArray.joinToString(prefix = "[", postfix = "]", separator = ",")
        }
        return output
    }

    @TypeConverter
    fun toFloatArray(inputString: String?): FloatArray?{
        if(inputString != null){
            val output = inputString.removeSurrounding("[", "]").split(",").map { it.toFloat() }
            return output.toFloatArray()
        }
        return null
    }
}