package ru.example.lab08

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

// Создаем DataStore ОДИН РАЗ на уровне файла
val Context.settings by preferencesDataStore(name = "settings")

val NAME_KEY = stringPreferencesKey("name")
val LARGE_TEXT_KEY = booleanPreferencesKey("large_text")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LabScreen()
                }
            }
        }
    }
}

@Composable
fun LabScreen() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()

    // --- Настройки (DataStore) ---
    var name by rememberSaveable { mutableStateOf("") }
    var largeText by rememberSaveable { mutableStateOf(false) }
    var fontSize by rememberSaveable { mutableFloatStateOf(16f) }
    var isSettingsLoaded by remember { mutableStateOf(false) }
    var settingsStatus by remember { mutableStateOf("Загрузка...") }

    // --- Заметка (Файл) ---
    var noteText by rememberSaveable { mutableStateOf("") }
    var noteStatus by remember { mutableStateOf("") }

    // --- Диалог удаления ---
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Чтение при старте
    LaunchedEffect(Unit) {
        try {
            val prefs = context.settings.data.first()
            name = prefs[NAME_KEY] ?: ""
            largeText = prefs[LARGE_TEXT_KEY] ?: false
            isSettingsLoaded = true
            settingsStatus = "Готово"
        } catch (_: IOException) {
            settingsStatus = "Ошибка чтения"
        }

        try {
            withContext(Dispatchers.IO) {
                val file = File(context.filesDir, "notes.txt")
                if (file.exists()) {
                    noteText = file.readText(Charsets.UTF_8)
                }
            }
        } catch (_: IOException) {
            noteStatus = "Ошибка чтения файла"
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить заметку?") },
            text = { Text("Действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                File(context.filesDir, "notes.txt").delete()
                            }
                            noteText = ""
                            noteStatus = "Файл удален"
                        } catch (_: IOException) {
                            noteStatus = "Ошибка удаления"
                        }
                    }
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Настройки", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            enabled = isSettingsLoaded,
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Крупный текст", modifier = Modifier.weight(1f))
            Switch(
                checked = largeText,
                onCheckedChange = { newValue ->
                    largeText = newValue
                    scope.launch {
                        try {
                            context.settings.edit { it[LARGE_TEXT_KEY] = newValue }
                        } catch (_: IOException) { settingsStatus = "Ошибка записи" }
                    }
                }
            )
        }

        Slider(
            value = fontSize,
            onValueChange = { fontSize = it },
            valueRange = 12f..32f,
            enabled = largeText
        )
        Text("Размер текста: ${fontSize.toInt()} sp")

        Button(
            enabled = isSettingsLoaded,
            onClick = {
                scope.launch {
                    try {
                        context.settings.edit { it[NAME_KEY] = name.trim() }
                        settingsStatus = "Настройки сохранены"
                    } catch (_: IOException) { settingsStatus = "Ошибка записи" }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить настройки")
        }
        Text(settingsStatus, color = MaterialTheme.colorScheme.primary)

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Заметка (Файл)", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Текст заметки") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    scope.launch {
                        noteStatus = "Сохранение..."
                        try {
                            withContext(Dispatchers.IO) {
                                File(context.filesDir, "notes.txt").writeText(noteText, Charsets.UTF_8)
                            }
                            noteStatus = "Заметка сохранена"
                        } catch (_: IOException) { noteStatus = "Ошибка записи файла" }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Сохранить файл")
            }

            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) {
                Text("Удалить")
            }
        }
        Text(noteStatus, color = MaterialTheme.colorScheme.primary)
    }
}