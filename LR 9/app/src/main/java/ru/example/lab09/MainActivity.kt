package ru.example.lab09

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.example.lab09.ui.theme.Lab09Theme

enum class DoneFilter(val label: String) {
    ALL("Все"),
    DONE("Выполненные"),
    UNDONE("Невыполненные")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Lab09Theme {
                LabScreen()
            }
        }
    }
}

@Composable
fun LabScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()

    var title by rememberSaveable { mutableStateOf("") }
    var newPriority by rememberSaveable { mutableIntStateOf(1) }
    var doneFilterName by rememberSaveable { mutableStateOf(DoneFilter.ALL.name) }
    var priorityFilter by rememberSaveable { mutableIntStateOf(0) } // 0 = все

    var rows by remember { mutableStateOf(emptyList<Task>()) }
    var counts by remember { mutableStateOf(emptyList<PriorityCount>()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    fun runDb(
        actionName: String,
        block: (TasksDb) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val done = when (doneFilterName) {
                    DoneFilter.DONE.name -> true
                    DoneFilter.UNDONE.name -> false
                    else -> null
                }
                val pr = priorityFilter.takeIf { it != 0 }

                val (newRows, newCounts) = withContext(Dispatchers.IO) {
                    TasksDb(context).use { db ->
                        block(db)
                        db.read(
                            doneFilter = done,
                            priorityFilter = pr
                        ) to db.countsByPriority()
                    }
                }

                rows = newRows
                counts = newCounts
                status = "$actionName. Записей: ${newRows.size}"
                onSuccess()
            } catch (e: Exception) {
                status = "Ошибка базы: ${e.message}"
            } finally {
                busy = false
            }
        }
    }

    // Загружаем данные при старте
    LaunchedEffect(Unit) {
        runDb("Загружено")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("ЛР9. SQLite: задачи", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Задача") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Приоритет новой задачи:", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 1..3) {
                FilterChip(
                    selected = newPriority == i,
                    onClick = { newPriority = i },
                    label = { Text("$i") }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !busy && title.isNotBlank(),
                onClick = {
                    runDb("Добавлено", { it.add(title.trim(), newPriority) }) {
                        title = ""
                    }
                }
            ) {
                Text("Добавить")
            }
            OutlinedButton(
                enabled = !busy,
                onClick = { runDb("Обновлено") }
            ) {
                Text("Обновить")
            }
        }

        // Фильтр выполнения
        Text("Фильтр выполнения:", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DoneFilter.entries.forEach { filter ->
                FilterChip(
                    selected = doneFilterName == filter.name,
                    onClick = {
                        doneFilterName = filter.name
                        runDb("Отфильтровано")
                    },
                    label = { Text(filter.label) }
                )
            }
        }

        // Фильтр приоритета
        Text("Фильтр приоритета:", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = priorityFilter == 0,
                onClick = {
                    priorityFilter = 0
                    runDb("Отфильтровано")
                },
                label = { Text("Все") }
            )
            for (i in 1..3) {
                FilterChip(
                    selected = priorityFilter == i,
                    onClick = {
                        priorityFilter = i
                        runDb("Отфильтровано")
                    },
                    label = { Text("$i") }
                )
            }
        }

        // Статус и статистика
        Text(status, color = MaterialTheme.colorScheme.primary)

        val countsStr = counts.joinToString(", ") { "${it.priority}: ${it.count}" }
        if (countsStr.isNotEmpty()) {
            Text("По приоритетам: $countsStr", style = MaterialTheme.typography.bodySmall)
        }

        // Список задач
        rows.forEach { task ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${task.id}: ${task.title}")
                        Text(
                            "Приоритет ${task.priority}; ${if (task.done) "выполнено" else "не выполнено"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Checkbox(
                        checked = task.done,
                        onCheckedChange = { checked ->
                            runDb("Обновлено", { it.updateDone(task.id, checked) })
                        }
                    )
                    TextButton(
                        onClick = { runDb("Удалено", { it.delete(task.id) }) }
                    ) {
                        Text("Удалить", color = Color.Red)
                    }
                }
            }
        }
    }
}