package com.dosevia.app

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dosevia.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val notes = state.notes

    var showAddDialog  by remember { mutableStateOf(false) }
    var editingNote    by remember { mutableStateOf<Note?>(null) }
    var searchQuery    by remember { mutableStateOf("") }

    val gradient = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))

    val filteredNotes = remember(notes, searchQuery) {
        val filtered = if (searchQuery.isBlank()) notes
        else notes.filter { it.content.contains(searchQuery, ignoreCase = true) }
        filtered.sortedByDescending { it.createdAt.time }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF0FB))) {
        val isTablet = maxWidth >= 480.dp
        val padH     = if (isTablet) 32.dp else 16.dp
        val titleSp  = if (isTablet) 22.sp  else 18.sp
        val lblSp    = if (isTablet) 15.sp  else 13.sp
        val subSp    = if (isTablet) 13.sp  else 11.sp

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .padding(top = 8.dp)
                    .padding(horizontal = padH, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White,
                                modifier = Modifier.size(if (isTablet) 28.dp else 22.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text("Notes", fontSize = titleSp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${notes.size} note${if (notes.size != 1) "s" else ""}",
                                fontSize = subSp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                    // FAB-style add button in header
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showAddDialog = true }
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White,
                            modifier = Modifier.size(22.dp))
                    }
                }
            }

            // ── Search bar ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = padH, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("Search notes…", fontSize = lblSp, color = Color(0xFF9CA3AF)) },
                    leadingIcon   = {
                        Icon(Icons.Default.Search, null, tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp))
                    },
                    trailingIcon  = if (searchQuery.isNotBlank()) {{
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = Color(0xFF9CA3AF))
                        }
                    }} else null,
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PinkPrimary,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor   = Color(0xFFFFF0FB),
                        unfocusedContainerColor = Color(0xFFF9FAFB)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Notes list ───────────────────────────────────────────
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(PinkPrimary.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Description, null,
                                tint = PinkPrimary.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp))
                        }
                        Text(
                            if (searchQuery.isNotBlank()) "No notes found" else "No notes yet",
                            fontSize = (lblSp.value * 1.15f).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )
                        Text(
                            if (searchQuery.isNotBlank()) "Try a different search term"
                            else "Tap + to add a note about\nyour pill-taking experience",
                            fontSize = subSp,
                            color = Color(0xFF9CA3AF)
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors  = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                shape   = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Your First Note")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = padH, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCard(
                            note    = note,
                            onEdit  = { editingNote = note; showAddDialog = true },
                            onDelete = { viewModel.deleteNote(note.id) },
                            lblSp   = lblSp,
                            subSp   = subSp
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── Add / Edit dialog ─────────────────────────────────────────────
    if (showAddDialog) {
        NoteDialog(
            editing   = editingNote,
            onDismiss = { showAddDialog = false; editingNote = null },
            onSave    = { content, date, time ->
                if (editingNote != null) {
                    viewModel.editNote(editingNote!!.id, content)
                } else {
                    viewModel.addNote(content, date, time)
                }
                showAddDialog = false
                editingNote = null
            }
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    lblSp: androidx.compose.ui.unit.TextUnit,
    subSp: androidx.compose.ui.unit.TextUnit
) {
    val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(PinkPrimary.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null,
                                tint = PinkPrimary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(dateFmt.format(note.date), fontSize = (subSp.value * 0.9f).sp,
                                color = PinkDark, fontWeight = FontWeight.Medium)
                        }
                    }
                    // Time chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(OrangeAccent.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null,
                                tint = OrangeAccent, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(note.time, fontSize = (subSp.value * 0.9f).sp,
                                color = Color(0xFFB45309), fontWeight = FontWeight.Medium)
                        }
                    }
                }
                // Edit / delete
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null,
                            tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null,
                            tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(note.content, fontSize = lblSp, color = Color(0xFF374151))
            Spacer(Modifier.height(6.dp))
            Text(
                "Updated ${SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault()).format(note.updatedAt)}",
                fontSize = (subSp.value * 0.85f).sp,
                color = Color(0xFFD1D5DB)
            )
        }
    }
}

@Composable
private fun NoteDialog(
    editing: Note?,
    onDismiss: () -> Unit,
    onSave: (String, Date, String) -> Unit
) {
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val now     = Date()

    var content by remember(editing) { mutableStateOf(editing?.content ?: "") }
    var date    by remember(editing) { mutableStateOf(editing?.date ?: now) }
    var time    by remember(editing) { mutableStateOf(editing?.time ?: timeFmt.format(now)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Title row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (editing != null) Icons.Default.Edit else Icons.Default.Add,
                            null, tint = Color.White, modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (editing != null) "Edit Note" else "Add Note",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = PinkPrimary
                    )
                }

                // Note content
                OutlinedTextField(
                    value         = content,
                    onValueChange = { content = it },
                    label         = { Text("How are you feeling?") },
                    placeholder   = { Text("Any side effects or observations…") },
                    minLines      = 4,
                    maxLines      = 8,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PinkPrimary,
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6B7280))
                    ) { Text("Cancel") }

                    Button(
                        onClick  = {
                            if (content.isNotBlank()) onSave(content.trim(), date, time)
                        },
                        enabled  = content.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                    ) {
                        Text(if (editing != null) "Update" else "Save", color = Color.White)
                    }
                }
            }
        }
    }
}
