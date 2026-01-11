package com.hskmaster.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HskWord(
    val simplified: String,
    val radical: String? = null,
    val frequency: Int? = null,
    val pos: List<String> = emptyList(),
    val forms: List<Form> = emptyList()
)

@Serializable
data class Form(
    val traditional: String,
    val transcriptions: Transcriptions,
    val meanings: List<String> = emptyList(),
    val classifiers: List<String> = emptyList()
)

@Serializable
data class Transcriptions(
    val pinyin: String,
    val numeric: String? = null,
    val wadegiles: String? = null,
    val bopomofo: String? = null,
    val romatzyh: String? = null
)