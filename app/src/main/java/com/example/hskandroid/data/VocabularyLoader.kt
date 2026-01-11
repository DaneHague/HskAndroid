package com.hskmaster.app.data

import android.content.Context
import com.hskmaster.app.model.SimpleHskWord
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class VocabularyLoader {
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    fun loadHskVocabulary(context: Context, level: Int): List<SimpleHskWord> {
        return try {
            val fileName = "hsk$level.json"
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            json.decodeFromString<List<SimpleHskWord>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun loadHsk1Vocabulary(context: Context): List<SimpleHskWord> {
        return loadHskVocabulary(context, 1)
    }
}