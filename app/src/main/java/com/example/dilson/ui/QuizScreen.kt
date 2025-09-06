package com.example.dilson.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dilson.quiz.QuizViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(viewModel: QuizViewModel, navController: NavController? = null) {
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val questions by viewModel.questions.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val score by viewModel.score.collectAsState()
    val completed by viewModel.completed.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadQuestions(10, 4)
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (questions.isEmpty()) {
                Text("Quiz için yeterli içerik yok")
                Button(onClick = { navController?.navigate("list") }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Geri")
                }
                return@Column
            }

            if (completed) {
                Text("Quiz tamamlandı! Skor: $score / ${questions.size}")
                Button(onClick = { viewModel.loadQuestions() }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Tekrar Oyna")
                }
                Button(onClick = { navController?.navigate("list") }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Geri")
                }
                return@Column
            }

            val q = questions[currentIndex]
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = q.correct.title ?: "(Başlıksız)")
                    Text(text = q.correct.text, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                q.options.forEachIndexed { idx, option ->
                    Button(onClick = {
                        viewModel.answerSelected(idx)
                        scope.launch { snackbarHostState.showSnackbar(if (option.id == q.correct.id) "Doğru" else "Yanlış") }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(option.text)
                    }
                }
            }

            Text("Skor: $score")
        }
    }
}
