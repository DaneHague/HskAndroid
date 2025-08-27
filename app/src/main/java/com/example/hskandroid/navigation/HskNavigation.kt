package com.example.hskandroid.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hskandroid.data.VocabularyLoader
import com.example.hskandroid.model.HskWord
import com.example.hskandroid.ui.GameSelectionScreen
import com.example.hskandroid.ui.LevelSelectionScreen
import com.example.hskandroid.ui.ListeningGameScreen
import com.example.hskandroid.ui.MatchingGameScreen
import com.example.hskandroid.ui.QuizGameScreen
import com.example.hskandroid.ui.WritingPracticeScreen
import com.example.hskandroid.ui.LearningLogScreen
import com.example.hskandroid.ui.DictionaryScreen
import com.example.hskandroid.ui.WordStatisticsScreen
import com.example.hskandroid.ui.TestScreen

sealed class Screen(val route: String) {
    object LevelSelection : Screen("level_selection")
    object GameSelection : Screen("game_selection/{level}") {
        fun createRoute(level: Int) = "game_selection/$level"
    }
    object MatchingGame : Screen("matching_game/{level}") {
        fun createRoute(level: Int) = "matching_game/$level"
    }
    object QuizGame : Screen("quiz_game/{level}") {
        fun createRoute(level: Int) = "quiz_game/$level"
    }
    object WritingPractice : Screen("writing_practice/{level}") {
        fun createRoute(level: Int) = "writing_practice/$level"
    }
    object ListeningGame : Screen("listening_game/{level}") {
        fun createRoute(level: Int) = "listening_game/$level"
    }
    object LearningLog : Screen("learning_log")
    object Dictionary : Screen("dictionary/{level}") {
        fun createRoute(level: Int) = "dictionary/$level"
    }
    object WordStatistics : Screen("word_statistics/{level}/{wordId}") {
        fun createRoute(level: Int, wordId: String) = "word_statistics/$level/$wordId"
    }
    object Test : Screen("test/{level}") {
        fun createRoute(level: Int) = "test/$level"
    }
}

@Composable
fun HskNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.LevelSelection.route,
        modifier = modifier
    ) {
        composable(Screen.LevelSelection.route) {
            LevelSelectionScreen(
                onLevelSelected = { level ->
                    navController.navigate(Screen.GameSelection.createRoute(level))
                },
                onLogPressed = {
                    navController.navigate(Screen.LearningLog.route)
                }
            )
        }
        
        composable(Screen.GameSelection.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            GameSelectionScreen(
                hskLevel = level,
                onGameSelected = { gameType ->
                    when (gameType) {
                        "matching" -> navController.navigate(Screen.MatchingGame.createRoute(level))
                        "quiz" -> navController.navigate(Screen.QuizGame.createRoute(level))
                        "writing" -> navController.navigate(Screen.WritingPractice.createRoute(level))
                        "listening" -> navController.navigate(Screen.ListeningGame.createRoute(level))
                        "dictionary" -> navController.navigate(Screen.Dictionary.createRoute(level))
                        "test" -> navController.navigate(Screen.Test.createRoute(level))
                    }
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.MatchingGame.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            val context = LocalContext.current
            var vocabulary by remember { mutableStateOf<List<HskWord>>(emptyList()) }
            
            LaunchedEffect(level) {
                vocabulary = VocabularyLoader().loadHskVocabulary(context, level)
            }
            
            if (vocabulary.isNotEmpty()) {
                MatchingGameScreen(
                    vocabulary = vocabulary,
                    hskLevel = level,
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable(Screen.QuizGame.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            val context = LocalContext.current
            var vocabulary by remember { mutableStateOf<List<HskWord>>(emptyList()) }
            val repository = remember { com.example.hskandroid.data.repository.LearningRepository(context) }
            
            LaunchedEffect(level) {
                vocabulary = VocabularyLoader().loadHskVocabulary(context, level)
            }
            
            if (vocabulary.isNotEmpty()) {
                QuizGameScreen(
                    vocabulary = vocabulary,
                    hskLevel = level,
                    repository = repository,
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable(Screen.WritingPractice.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            val context = LocalContext.current
            var vocabulary by remember { mutableStateOf<List<HskWord>>(emptyList()) }
            
            LaunchedEffect(level) {
                vocabulary = VocabularyLoader().loadHskVocabulary(context, level)
            }
            
            if (vocabulary.isNotEmpty()) {
                WritingPracticeScreen(
                    vocabulary = vocabulary,
                    hskLevel = level,
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable(Screen.ListeningGame.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            val context = LocalContext.current
            var vocabulary by remember { mutableStateOf<List<HskWord>>(emptyList()) }
            
            LaunchedEffect(level) {
                vocabulary = VocabularyLoader().loadHskVocabulary(context, level)
            }
            
            if (vocabulary.isNotEmpty()) {
                ListeningGameScreen(
                    vocabulary = vocabulary,
                    hskLevel = level,
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable(Screen.LearningLog.route) {
            LearningLogScreen(
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Dictionary.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            val context = LocalContext.current
            var vocabulary by remember { mutableStateOf<List<HskWord>>(emptyList()) }
            
            LaunchedEffect(level) {
                vocabulary = VocabularyLoader().loadHskVocabulary(context, level)
            }
            
            if (vocabulary.isNotEmpty()) {
                DictionaryScreen(
                    vocabulary = vocabulary,
                    hskLevel = level,
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onWordClick = { word ->
                        navController.navigate(Screen.WordStatistics.createRoute(level, word.simplified))
                    }
                )
            }
        }
        
        composable(Screen.WordStatistics.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            val wordId = backStackEntry.arguments?.getString("wordId") ?: ""
            val context = LocalContext.current
            var vocabulary by remember { mutableStateOf<List<HskWord>>(emptyList()) }
            
            LaunchedEffect(level) {
                vocabulary = VocabularyLoader().loadHskVocabulary(context, level)
            }
            
            val word = vocabulary.find { it.simplified == wordId }
            
            if (word != null) {
                WordStatisticsScreen(
                    word = word,
                    hskLevel = level,
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable(Screen.Test.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            
            TestScreen(
                hskLevel = level,
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
    }
}