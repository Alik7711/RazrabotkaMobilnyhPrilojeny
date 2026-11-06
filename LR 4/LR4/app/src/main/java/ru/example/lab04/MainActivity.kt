package ru.example.lab04

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var dark by rememberSaveable { mutableStateOf(false) }
            MaterialTheme(
                colorScheme = if (dark) darkColorScheme() else lightColorScheme()
            ) {
                LabScreen(dark, onDarkChange = { dark = it })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LabScreen(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val sample = stringResource(R.string.note_sample)
    var note by rememberSaveable { mutableStateOf(sample) }
    var expanded by remember { mutableStateOf(false) }
    var contextExpanded by remember { mutableStateOf(false) }
    val messages = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = requireNotNull(
        context.getSystemService(ClipboardManager::class.java)
    )

    fun closeMenus() {
        expanded = false
        contextExpanded = false
    }

    fun clearNote() {
        note = ""
        closeMenus()
        Log.d("Lab04", "clear")
        scope.launch {
            messages.showSnackbar(context.getString(R.string.message_cleared))
        }
    }

    fun restoreNote() {
        note = sample
        closeMenus()
        Log.d("Lab04", "restore")
        scope.launch {
            messages.showSnackbar(context.getString(R.string.message_restored))
        }
    }

    fun copyNote() {
        if (note.isEmpty()) return
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                context.getString(R.string.clipboard_label),
                note
            )
        )
        closeMenus()
        Log.d("Lab04", "copy")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            scope.launch {
                messages.showSnackbar(context.getString(R.string.message_copied))
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(messages) }) { insets ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.screen_title),
                style = MaterialTheme.typography.headlineSmall
            )

            Box {
                Text(
                    text = if (note.isEmpty())
                        stringResource(R.string.note_empty) else note,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClickLabel = stringResource(R.string.note_actions),
                            onLongClickLabel = stringResource(R.string.note_actions),
                            onClick = {
                                closeMenus()
                                contextExpanded = true
                            },
                            onLongClick = {
                                closeMenus()
                                contextExpanded = true
                            }
                        )
                        .padding(16.dp)
                )
                NoteMenu(
                    expanded = contextExpanded,
                    onDismiss = { closeMenus() },
                    canClear = note.isNotEmpty(),
                    onClear = { clearNote() },
                    onRestore = { restoreNote() },
                    onCopy = { copyNote() }
                )
            }

            Text(stringResource(R.string.dark_theme))
            Switch(checked = dark, onCheckedChange = onDarkChange)

            Box {
                Button(onClick = {
                    closeMenus()
                    expanded = true
                }) {
                    Text(stringResource(R.string.actions))
                }
                NoteMenu(
                    expanded = expanded,
                    onDismiss = { closeMenus() },
                    canClear = note.isNotEmpty(),
                    onClear = { clearNote() },
                    onRestore = { restoreNote() },
                    onCopy = { copyNote() }
                )
            }
        }
    }
}

@Composable
fun NoteMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    canClear: Boolean,
    onClear: () -> Unit,
    onRestore: () -> Unit,
    onCopy: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_copy)) },
            enabled = canClear,
            onClick = onCopy
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_clear)) },
            enabled = canClear,
            onClick = onClear
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_restore)) },
            onClick = onRestore
        )
    }
}