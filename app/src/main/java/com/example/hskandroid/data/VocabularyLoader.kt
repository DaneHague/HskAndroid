package com.example.hskandroid.data

import android.content.Context
import com.example.hskandroid.model.HskWord
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class VocabularyLoader {
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    fun loadHskVocabulary(context: Context, level: Int): List<HskWord> {
        return try {
            val fileName = "hsk$level.json"
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            json.decodeFromString<List<HskWord>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun loadHsk1Vocabulary(context: Context): List<HskWord> {
        return loadHskVocabulary(context, 1)
    }
}