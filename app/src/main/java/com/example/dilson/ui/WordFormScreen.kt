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
import com.example.dilson.content.Word
import com.example.dilson.viewmodel.WordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordFormScreen(viewModel: WordViewModel, navController: NavController?) {
    var text by remember { mutableStateOf("") }
    var transliteration by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var hasDiacritics by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Kelime (Arapça)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = transliteration, onValueChange = { transliteration = it }, label = { Text("Transliterasyon") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(value = meaning, onValueChange = { meaning = it }, label = { Text("Anlamı") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Kategori") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

        RowForDiacritics(hasDiacritics) { hasDiacritics = it }

        Button(onClick = {
            if (text.isNotBlank()) {
                viewModel.insert(Word(text = text, transliteration = transliteration.ifBlank { null }, meaning = meaning.ifBlank { null }, category = category.ifBlank { null }, hasDiacritics = hasDiacritics))
                navController?.popBackStack()
            }
        }, modifier = Modifier.padding(top = 12.dp)) {
            Text("Kaydet")
        }
    }
}

@Composable
fun RowForDiacritics(hasDiacritics: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("Harekeli / Haraksiz")
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
        Switch(checked = hasDiacritics, onCheckedChange = onChange)
    }
}

