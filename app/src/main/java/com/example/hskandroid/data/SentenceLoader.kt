package com.hskmaster.app.data

import android.content.Context
import com.hskmaster.app.model.HskSentence
import kotlinx.serialization.json.Json

class SentenceLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadSentences(context: Context, level: Int): List<HskSentence> {
        return try {
            val fileName = "sentences/hsk${level}_sentences.json"
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            json.decodeFromString<List<HskSentence>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
