package com.hskmaster.app.model

import kotlinx.serialization.Serializable

@Serializable
data class HskSentence(
    val id: Int,
    val chinese: String,           // Full Chinese sentence: "我喜欢吃苹果"
    val pinyin: String,            // Full pinyin: "wǒ xǐ huan chī píng guǒ"
    val english: String,           // Translation: "I like to eat apples"
    val words: List<SentenceWord>  // Individual words for shuffling
)

@Serializable
data class SentenceWord(
    val chinese: String,           // "喜欢"
    val pinyin: String,            // "xǐ huan"
    val position: Int              // Original position in sentence (0-indexed)
)
