package com.example.dilson.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dilson.utils.TtsManager
import com.example.dilson.utils.SpeechRecognitionManager
import com.example.dilson.utils.ArabicUtils
import com.example.dilson.viewmodel.SrsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SrsReviewScreen(viewModel: SrsViewModel, navController: NavController? = null) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ttsManager = remember { TtsManager(context) }
    val speechManager = remember { SpeechRecognitionManager(context) }

    // speech settings
    var rate by remember { mutableStateOf(1.0f) }
    var pitch by remember { mutableStateOf(1.0f) }

    // permission launcher
    var micRequested by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micRequested = true
        if (!granted) {
            scope.launch { snackbarHostState.showSnackbar("Mikrofon izni reddedildi") }
        }
    }

    // apply current settings when changed
    val currentTts by rememberUpdatedState(ttsManager)
    LaunchedEffect(rate) { currentTts.setSpeechRate(rate) }
    LaunchedEffect(pitch) { currentTts.setPitch(pitch) }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
            speechManager.stopListening()
        }
    }

    val items by viewModel.dueItems.collectAsState(initial = emptyList())

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (items.isEmpty()) {
                Text("İncelenecek öğe yok")
                Button(onClick = { navController?.navigate("list") }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Geri")
                }
            } else {
                val srs = items.first()
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = srs.content.title ?: "(Başlıksız)")
                        Text(text = srs.content.text, modifier = Modifier.padding(top = 8.dp))
                        if (!srs.content.transliteration.isNullOrBlank()) {
                            Text(text = "Transliterasyon: ${srs.content.transliteration}", modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }

                // TTS controls: play / stop
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { ttsManager.speak(srs.content.text) }) { Text("Dinle") }
                    Button(onClick = { ttsManager.stop() }) { Text("Durdur") }
                    Button(onClick = {
                        // request permission if not requested/granted
                        if (!micRequested) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        else {
                            // start listening
                            speechManager.startListening(object : SpeechRecognitionManager.Callback {
                                override fun onResult(text: String) {
                                    val recognized = ArabicUtils.normalize(text)
                                    val target = ArabicUtils.normalize(srs.content.text)
                                    val ok = when {
                                        recognized == target -> "Tam eşleşme"
                                        recognized.contains(target) || target.contains(recognized) -> "Benzer"
                                        else -> "Farklı"
                                    }
                                    scope.launch { snackbarHostState.showSnackbar("Söylenen: $recognized — $ok") }
                                }

                                override fun onError(error: String) {
                                    scope.launch { snackbarHostState.showSnackbar(error) }
                                }
                            })
                        }
                    }) { Text("Mikrofon") }
                }

                // Rate and pitch sliders
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text("Hız: ${"%.2f".format(rate)}")
                    androidx.compose.material3.Slider(value = rate, onValueChange = { rate = it }, valueRange = 0.5f..2.0f)
                    Text("Perde: ${"%.2f".format(pitch)}")
                    androidx.compose.material3.Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.5f..2.0f)
                }

                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = {
                        viewModel.reviewKnown(srs)
                        scope.launch { snackbarHostState.showSnackbar("Biliyorum seçildi") }
                    }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("Biliyorum")
                    }
                    Button(onClick = {
                        viewModel.reviewUnsure(srs)
                        scope.launch { snackbarHostState.showSnackbar("Emin Değilim seçildi") }
                    }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("Emin Değilim")
                    }
                    Button(onClick = {
                        viewModel.reviewUnknown(srs)
                        scope.launch { snackbarHostState.showSnackbar("Bilmiyorum seçildi") }
                    }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("Bilmiyorum")
                    }
                }
            }
        }
    }
}
