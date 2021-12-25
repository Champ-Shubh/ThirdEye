package com.example.assistme.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized

@Database(entities = [Person::class], version = 1)
@TypeConverters(ConvertersDB::class)
abstract class PersonDatabase : RoomDatabase() {

    abstract fun personDao() : PersonDao

    companion object {
        private var INSTANCE : PersonDatabase? = null
        @InternalCoroutinesApi
        fun getDatabase(context: Context): PersonDatabase? {
            if(INSTANCE == null){
                synchronized(PersonDatabase::class.java){
                    INSTANCE = Room.databaseBuilder(context.applicationContext, PersonDatabase::class.java, "person.db").build()
                }
            }
            return INSTANCE
        }
    }
}