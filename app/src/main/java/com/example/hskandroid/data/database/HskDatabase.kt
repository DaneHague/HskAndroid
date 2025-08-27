package com.example.hskandroid.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LearningRecord::class],
    version = 1,
    exportSchema = false
)
abstract class HskDatabase : RoomDatabase() {
    abstract fun learningRecordDao(): LearningRecordDao
    
    companion object {
        @Volatile
        private var INSTANCE: HskDatabase? = null
        
        fun getDatabase(context: Context): HskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HskDatabase::class.java,
                    "hsk_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}