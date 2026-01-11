package com.hskmaster.app.model

import kotlinx.serialization.Serializable

@Serializable
data class ClozeQuestion(
    val id: Int,
    val sentenceWithBlank: String,  // "我___吃苹果"
    val fullSentence: String,       // "我喜欢吃苹果"
    val pinyin: String,             // "wǒ xǐ huan chī píng guǒ"
    val english: String,            // "I like to eat apples"
    val correctAnswer: String,      // "喜欢"
    val correctPinyin: String,      // "xǐ huan"
    val options: List<String>       // ["想", "喜欢", "要", "会"]
)
