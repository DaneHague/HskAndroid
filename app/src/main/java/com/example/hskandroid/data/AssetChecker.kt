package com.hskmaster.app.data

import android.content.Context

object AssetChecker {
    fun isHskLevelAvailable(context: Context, level: Int): Boolean {
        return try {
            context.assets.open("hsk$level.json").close()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getAvailableLevels(context: Context): List<Int> {
        return (1..7).filter { level ->
            isHskLevelAvailable(context, level)
        }
    }
}