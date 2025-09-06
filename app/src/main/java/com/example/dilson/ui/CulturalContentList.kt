package com.example.dilson.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dilson.advanced.CultureContent
import com.example.dilson.viewmodel.CulturalContentViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete

@Composable
fun CulturalContentList(viewModel: CulturalContentViewModel, navController: NavController) {
    val items by viewModel.allItems.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { navController.navigate("form") }, modifier = Modifier.padding(bottom = 8.dp)) {
                Text("Yeni Kültürel İçerik Ekle")
            }
            Row {
                Button(onClick = { navController.navigate("srs") }, modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)) {
                    Text("SRS İncele")
                }
                Button(onClick = { navController.navigate("points") }, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("Puanlar")
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item: CultureContent ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.title ?: "(Başlıksız)", style = MaterialTheme.typography.titleMedium)
                            Text(text = item.text.take(200), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        }
                        IconButton(onClick = { if (item.id != 0) viewModel.deleteById(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil")
                        }
                    }
                }
            }
        }
    }
}
