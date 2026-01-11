package com.hskmaster.app.data

import android.content.Context
import com.hskmaster.app.model.ClozeQuestion
import kotlinx.serialization.json.Json

class ClozeLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadQuestions(context: Context, level: Int): List<ClozeQuestion> {
        return try {
            val fileName = "cloze/hsk${level}_cloze.json"
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            json.decodeFromString<List<ClozeQuestion>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
