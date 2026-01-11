package com.hskmaster.app.model

import kotlinx.serialization.Serializable

@Serializable
data class SimpleHskWord(
    val id: Int,
    val chinese: String,
    val pinyin: String,
    val english: String
)