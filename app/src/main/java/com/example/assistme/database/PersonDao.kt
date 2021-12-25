package com.example.assistme.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PersonDao {

    @Insert
    fun insert(person: Person)

    @Query("DELETE FROM person WHERE embedding = :inputEmbedding")
    fun deleteByEmbedding(inputEmbedding: FloatArray)

    @Query("DELETE FROM person")
    fun deleteAll()

    @Query("SELECT * FROM person WHERE embedding = :queryEmbedding")
    fun getPerson(queryEmbedding: FloatArray): Person

    @Query("SELECT * FROM person")
    fun getAll(): List<Person>
}