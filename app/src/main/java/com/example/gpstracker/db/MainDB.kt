package com.example.gpstracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database (entities = [TrackItem::class], version = 1)
abstract class MainDB: RoomDatabase() {
    abstract fun getDao(): Dao

    companion object{
        @Volatile
        var INSTANCE: MainDB? = null
        fun getDatabase(context: Context): MainDB{
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MainDB::class.java,
                    "GpsTrackerDb"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}