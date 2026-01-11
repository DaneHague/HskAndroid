package com.hskmaster.app.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.hskmaster.app.data.VocabularyLoader
import com.hskmaster.app.model.SimpleHskWord
import com.hskmaster.app.ui.GameSelectionScreen
import com.hskmaster.app.ui.LevelSelectionScreen
import com.hskmaster.app.ui.ListeningGameScreen
import com.hskmaster.app.ui.MatchingGameScreen
import com.hskmaster.app.ui.QuizGameScreen
import com.hskmaster.app.ui.WritingPracticeScreen
import com.hskmaster.app.ui.LearningLogScreen
import com.hskmaster.app.ui.DictionaryScreen
import com.hskmaster.app.ui.WordStatisticsScreen
import com.hskmaster.app.ui.TestScreen
import com.hskmaster.app.ui.TestSelectionScreen
import com.hskmaster.app.ui.TestResultDetailScreen
import com.hskmaster.app.ui.SentenceBuilderScreen
import com.hskmaster.app.ui.SpeedChallengeScreen
import com.hskmaster.app.ui.FillBlankScreen
import com.hskmaster.app.data.SentenceLoader
import com.hskmaster.app.data.ClozeLoader
import com.hskmaster.app.model.HskSentence
import com.hskmaster.app.model.ClozeQuestion

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
    object SentenceBuilder : Screen("sentence_builder/{level}") {
        fun createRoute(level: Int) = "sentence_builder/$level"
    }
    object SpeedChallenge : Screen("speed_challenge/{level}") {
        fun createRoute(level: Int) = "speed_challenge/$level"
    }
    object FillBlank : Screen("fill_blank/{level}") {
        fun createRoute(level: Int) = "fill_blank/$level"
    }
    object LearningLog : Screen("learning_log")
    object Dictionary : Screen("dictionary/{level}") {
        fun createRoute(level: Int) = "dictionary/$level"
    }
    object WordStatistics : Screen("word_statistics/{level}/{wordId}") {
        fun createRoute(level: Int, wordId: String) = "word_statistics/$level/$wordId"
    }
    object TestSelection : Screen("test_selection/{level}") {
        fun createRoute(level: Int) = "test_selection/$level"
    }
    object Test : Screen("test/{level}?testPath={testPath}") {
        fun createRoute(level: Int, testPath: String) = "test/$level?testPath=${java.net.URLEncoder.encode(testPath, "UTF-8")}"
    }
    object TestResultDetail : Screen("test_result/{attemptId}") {
        fun createRoute(attemptId: Long) = "test_result/$attemptId"
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
                        "sentence_builder" -> navController.navigate(Screen.SentenceBuilder.createRoute(level))
                        "speed_challenge" -> navController.navigate(Screen.SpeedChallenge.createRoute(level))
                        "fill_blank" -> navController.navigate(Screen.FillBlank.createRoute(level))
                        "dictionary" -> navController.navigate(Screen.Dictionary.createRoute(level))
                        "test" -> navController.navigate(Screen.TestSelection.createRoute(level))
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
            var vocabulary by remember { mutableStateOf<List<SimpleHskWord>>(emptyList()) }
            
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
            var vocabulary by remember { mutableStateOf<List<SimpleHskWord>>(emptyList()) }
            val repository = remember { com.hskmaster.app.data.repository.LearningRepository(context) }
            
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
            var vocabulary by remember { mutableStateOf<List<SimpleHskWord>>(emptyList()) }
            
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
            var vocabulary by remember { mutableStateOf<List<SimpleHskWord>>(emptyList()) }
            
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

        composable(Screen.SentenceBuilder.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            val context = LocalContext.current
            var sentences by remember { mutableStateOf<List<HskSentence>>(emptyList()) }
            val repository = remember { com.hskmaster.app.data.repository.LearningRepository(context) }

            LaunchedEffect(level) {
                sentences = SentenceLoader().loadSentences(context, level)
            }

            if (sentences.isNotEmpty()) {
                SentenceBuilderScreen(
                    sentences = sentences,
                    hskLevel = level,
                    repository = repository,
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Screen.SpeedChallenge.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            val context = LocalContext.current
            var vocabulary by remember { mutableStateOf<List<SimpleHskWord>>(emptyList()) }
            val repository = remember { com.hskmaster.app.data.repository.LearningRepository(context) }

            LaunchedEffect(level) {
                vocabulary = VocabularyLoader().loadHskVocabulary(context, level)
            }

            if (vocabulary.isNotEmpty()) {
                SpeedChallengeScreen(
                    vocabulary = vocabulary,
                    hskLevel = level,
                    repository = repository,
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Screen.FillBlank.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            val context = LocalContext.current
            var questions by remember { mutableStateOf<List<ClozeQuestion>>(emptyList()) }
            val repository = remember { com.hskmaster.app.data.repository.LearningRepository(context) }

            LaunchedEffect(level) {
                questions = ClozeLoader().loadQuestions(context, level)
            }

            if (questions.isNotEmpty()) {
                FillBlankScreen(
                    questions = questions,
                    hskLevel = level,
                    repository = repository,
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
            var vocabulary by remember { mutableStateOf<List<SimpleHskWord>>(emptyList()) }
            
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
                        navController.navigate(Screen.WordStatistics.createRoute(level, word.chinese))
                    }
                )
            }
        }
        
        composable(Screen.WordStatistics.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            val wordId = backStackEntry.arguments?.getString("wordId") ?: ""
            val context = LocalContext.current
            var vocabulary by remember { mutableStateOf<List<SimpleHskWord>>(emptyList()) }
            
            LaunchedEffect(level) {
                vocabulary = VocabularyLoader().loadHskVocabulary(context, level)
            }
            
            val word = vocabulary.find { it.chinese == wordId }
            
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
        
        composable(Screen.TestSelection.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            
            TestSelectionScreen(
                hskLevel = level,
                onTestSelected = { testPath ->
                    navController.navigate(Screen.Test.createRoute(level, testPath))
                },
                onAttemptSelected = { attemptId ->
                    navController.navigate(Screen.TestResultDetail.createRoute(attemptId))
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.Test.route,
            arguments = listOf(
                navArgument("testPath") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
            val testPath = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("testPath") ?: "",
                "UTF-8"
            )
            
            TestScreen(
                hskLevel = level,
                testPath = testPath,
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.TestResultDetail.route) { backStackEntry ->
            val attemptId = backStackEntry.arguments?.getString("attemptId")?.toLongOrNull() ?: 0L
            
            TestResultDetailScreen(
                attemptId = attemptId,
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
    }
}