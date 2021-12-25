package com.example.assistme.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "person")
data class Person(@PrimaryKey @ColumnInfo(name = "embedding") val faceEmbedding: FloatArray,
                  @ColumnInfo(name = "name") val personName: String,
                  @ColumnInfo(name = "relation") val relationToUser: String)