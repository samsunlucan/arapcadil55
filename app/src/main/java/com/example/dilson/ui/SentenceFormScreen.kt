package com.example.dilson.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dilson.content.Sentence
import com.example.dilson.viewmodel.SentenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceFormScreen(viewModel: SentenceViewModel, navController: NavController?) {
    var text by remember { mutableStateOf("") }
    var transliteration by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var hasDiacritics by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Cümle (Arapça)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = transliteration, onValueChange = { transliteration = it }, label = { Text("Transliterasyon") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(value = translation, onValueChange = { translation = it }, label = { Text("Çeviri") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Kategori") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

        RowForDiacritics(hasDiacritics) { hasDiacritics = it }

        Button(onClick = {
            if (text.isNotBlank()) {
                viewModel.insert(Sentence(text = text, transliteration = transliteration.ifBlank { null }, translation = translation.ifBlank { null }, category = category.ifBlank { null }, hasDiacritics = hasDiacritics))
                navController?.popBackStack()
            }
        }, modifier = Modifier.padding(top = 12.dp)) {
            Text("Kaydet")
        }
    }
}

