package com.example.dilson.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dilson.viewmodel.CulturalContentViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CulturalContentForm(viewModel: CulturalContentViewModel, navController: NavController? = null) {
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("ar") }
    var transliteration by remember { mutableStateOf("") }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Başlık (isteğe bağlı)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Metin (zorunlu)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            OutlinedTextField(
                value = transliteration,
                onValueChange = { transliteration = it },
                label = { Text("Transliterasyon (isteğe bağlı)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        viewModel.insert(title.ifBlank { null }, text, language, transliteration.ifBlank { null })
                    }
                },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Kaydet")
            }
        }

        // show snackbar on successful insert and navigate back
        LaunchedEffect(viewModel) {
            viewModel.insertResult.collectLatest { id ->
                if (id != null && id > 0) {
                    snackbarHostState.showSnackbar("Kaydedildi (id=$id)")
                    navController?.popBackStack()
                }
            }
        }
    }
}
