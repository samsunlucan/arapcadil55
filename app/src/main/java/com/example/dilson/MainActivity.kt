package com.example.dilson

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dilson.data.AppDatabase
import com.example.dilson.repository.AdvancedFeaturesRepository
import com.example.dilson.repository.ContentRepository
import com.example.dilson.srs.SrsRepository
import com.example.dilson.ui.CulturalContentForm
import com.example.dilson.ui.CulturalContentList
import com.example.dilson.ui.QuizScreen
import com.example.dilson.ui.SrsReviewScreen
import com.example.dilson.ui.WordFormScreen
import com.example.dilson.ui.WordListScreen
import com.example.dilson.ui.SentenceFormScreen
import com.example.dilson.ui.SentenceListScreen
import com.example.dilson.ui.PointsScreen
import com.example.dilson.ui.theme.DilSonTheme
import com.example.dilson.viewmodel.CulturalContentViewModel
import com.example.dilson.viewmodel.SrsViewModel
import com.example.dilson.viewmodel.WordViewModel
import com.example.dilson.quiz.QuizRepository
import com.example.dilson.quiz.QuizViewModel
import com.example.dilson.gamification.PointRepository
import com.example.dilson.gamification.PointViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: CulturalContentViewModel
    private lateinit var srsViewModel: SrsViewModel
    private lateinit var quizViewModel: QuizViewModel
    private lateinit var wordViewModel: WordViewModel
    private lateinit var sentenceViewModel: com.example.dilson.viewmodel.SentenceViewModel
    private lateinit var pointViewModel: PointViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Database -> Repository -> ViewModel
        val db = AppDatabase.getInstance(applicationContext)
        val repo = AdvancedFeaturesRepository(db.culturalContentDao())
        val srsRepo = SrsRepository(db.srsDao())
        val quizRepo = QuizRepository(db.culturalContentDao())
        val contentRepo = ContentRepository(db.wordDao(), db.sentenceDao())
        val pointRepo = PointRepository(db.pointTransactionDao())

        val factory = CulturalContentViewModel.Factory(repo, srsRepo)
        viewModel = ViewModelProvider(this, factory).get(CulturalContentViewModel::class.java)

        val srsFactory = SrsViewModel.Factory(srsRepo, pointRepo)
        srsViewModel = ViewModelProvider(this, srsFactory).get(SrsViewModel::class.java)

        val quizFactory = QuizViewModel.Factory(quizRepo)
        quizViewModel = ViewModelProvider(this, quizFactory).get(QuizViewModel::class.java)

        val wordFactory = WordViewModel.Factory(contentRepo)
        wordViewModel = ViewModelProvider(this, wordFactory).get(WordViewModel::class.java)

        // create SentenceViewModel properly
        val sentenceFactory = com.example.dilson.viewmodel.SentenceViewModel.Factory(contentRepo)
        sentenceViewModel = ViewModelProvider(this, sentenceFactory).get(com.example.dilson.viewmodel.SentenceViewModel::class.java)

        val pointFactory = PointViewModel.Factory(pointRepo)
        pointViewModel = ViewModelProvider(this, pointFactory).get(PointViewModel::class.java)

        setContent {
            DilSonTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "list",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("list") {
                            CulturalContentList(viewModel = viewModel, navController = navController)
                        }
                        composable("form") {
                            CulturalContentForm(viewModel = viewModel, navController = navController)
                        }
                        composable("srs") {
                            SrsReviewScreen(viewModel = srsViewModel, navController = navController)
                        }
                        composable("quiz") {
                            QuizScreen(viewModel = quizViewModel, navController = navController)
                        }
                        composable("words") {
                            WordListScreen(viewModel = wordViewModel, navController = navController)
                        }
                        composable("word_form") {
                            WordFormScreen(viewModel = wordViewModel, navController = navController)
                        }
                        composable("sentences") {
                            SentenceListScreen(viewModel = sentenceViewModel, navController = navController)
                        }
                        composable("sentence_form") {
                            SentenceFormScreen(viewModel = sentenceViewModel, navController = navController)
                        }
                        composable("points") {
                            PointsScreen(viewModel = pointViewModel, navController = navController)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMain() {
    DilSonTheme {
        // Minimal preview UI
    }
}