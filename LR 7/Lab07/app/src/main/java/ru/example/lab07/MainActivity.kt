package ru.example.lab07

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LabScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun LabScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Состояния экрана, сохраняемые при повороте
    var text by rememberSaveable { mutableStateOf("Файл не выбран") }
    var note by rememberSaveable { mutableStateOf("") }

    // Контракт для ЧТЕНИЯ файла (OpenDocument)
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                text = try {
                    withContext(Dispatchers.IO) {
                        // Попытка взять persistable permission на случай сохранения Uri
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: SecurityException) { }

                        context.contentResolver.openInputStream(uri)?.use { input ->
                            input.reader(Charsets.UTF_8).use { reader ->
                                val chars = CharArray(500)
                                val count = reader.read(chars)
                                if (count < 0) "Пустой файл" else String(chars, 0, count)
                            }
                        } ?: "Поток не открыт"
                    }
                } catch (e: IOException) {
                    "Ошибка чтения: ${e.message}"
                } catch (e: SecurityException) {
                    "Доступ отозван"
                }
            }
        }
        // Если uri == null (отмена), ничего не делаем — текст остается прежним
    }

    // Контракт для ЭКСПОРТА файла (CreateDocument)
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        // Сохраняем разрешение на запись
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        } catch (_: SecurityException) { }

                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.writer(Charsets.UTF_8).use { writer ->
                                writer.write(note)
                            }
                        }
                    }
                    // Успех показываем ТОЛЬКО после закрытия потоков (блок withContext завершен)
                    Toast.makeText(context, "Заметка успешно экспортирована!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Кнопка выбора файла
        Button(
            onClick = { picker.launch(arrayOf("text/plain")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Выбрать файл (txt)")
        }

        // Отображение прочитанного текста
        Text(
            text = "Содержимое файла:\n$text",
            style = MaterialTheme.typography.bodyMedium
        )

        // Поле ввода заметки
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Заметка") },
            modifier = Modifier.fillMaxWidth()
        )

        // Кнопка экспорта заметки (Самостоятельное задание)
        Button(
            onClick = { exporter.launch("note_export.txt") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Экспортировать заметку в файл")
        }

        // Кнопка открытия сайта
        Button(
            onClick = {
                val url = "https://developer.android.com"
                if (!url.startsWith("https://")) {
                    text = "Ошибка: разрешены только HTTPS-адреса"
                    return@Button
                }
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: ActivityNotFoundException) {
                    text = "Браузер не найден"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Открыть сайт (HTTPS)")
        }
    }
}