package ru.example.lab07

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.example.lab07.ui.theme.Lab07Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Lab07Theme {
                LabScreen()
            }
        }
    }
}

@Composable
fun LabScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Состояния
    var text by rememberSaveable { mutableStateOf("Файл не выбран") }
    var note by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    // 1. Выбор файла (OpenDocument)
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                text = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            input.reader(Charsets.UTF_8).use { reader ->
                                val chars = CharArray(500)
                                val count = reader.read(chars)
                                if (count < 0) "Пустой файл" else String(chars, 0, count)
                            }
                        } ?: "Поток не открыт"
                    } catch (e: Exception) {
                        "Ошибка чтения"
                    }
                }
                status = "Файл выбран. Доступ сохранен."
            }
        } else {
            status = "Выбор отменен. Предыдущий результат сохранен."
        }
    }

    // 2. Экспорт заметки (CreateDocument)
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(note.toByteArray(Charsets.UTF_8))
                        }
                    } catch (e: Exception) {
                        // Обработка ошибки
                    }
                }
                status = "Заметка записана. Файл можно открыть после перезапуска."
            }
        } else {
            status = "Экспорт отменен."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Файлы и заметки", style = MaterialTheme.typography.headlineMedium)

        if (status.isNotEmpty()) {
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = { picker.launch(arrayOf("text/plain")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Выбрать файл (txt)")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Содержимое файла:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text)
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Заметка") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Button(
            onClick = { exporter.launch("note.txt") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Экспортировать заметку в файл")
        }

        Button(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.android.com"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    status = "Браузер не найден"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Открыть сайт (HTTPS)")
        }
    }
}