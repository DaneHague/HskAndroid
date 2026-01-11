package com.hskmaster.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hskmaster.app.data.TestAttemptEntity
import com.hskmaster.app.data.TestAttemptDao
import com.hskmaster.app.data.Converters

@Database(
    entities = [LearningRecord::class, TestAttemptEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HskDatabase : RoomDatabase() {
    abstract fun learningRecordDao(): LearningRecordDao
    abstract fun testAttemptDao(): TestAttemptDao
    
    companion object {
        @Volatile
        private var INSTANCE: HskDatabase? = null
        
        fun getDatabase(context: Context): HskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HskDatabase::class.java,
                    "hsk_database"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}