package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ScriptEntity
import com.example.ui.MainViewModel
import com.example.ui.components.LuaHighlighter
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val scripts by viewModel.filteredScripts.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditing.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf<ScriptEntity?>(null) }
    var showPresetInfoDialog by remember { mutableStateOf(false) }

    // Launcher for selecting a local Lua / Text file from phone storage
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val contentResolver = context.contentResolver
                contentResolver.openInputStream(it)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { reader -> reader.readText() }
                    
                    // Simple query to retrieve the actual file display name
                    var fileName = "Script Impor"
                    val cursor = contentResolver.query(it, null, null, null, null)
                    cursor?.use { c ->
                        val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0 && c.moveToFirst()) {
                            fileName = c.getString(nameIndex)
                                .replace(".lua", "")
                                .replace(".txt", "")
                        }
                    }
                    
                    viewModel.importScriptText(fileName, content)
                    Toast.makeText(context, "Script '$fileName' berhasil dimuat!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isEditing) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFD0BCFF), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Δ",
                                    color = Color(0xFF381E72),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Column {
                                Text(
                                    text = "Delta Script Saver",
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Simpan & Generator Roblox Lua",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCAC4D0)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showPresetInfoDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Panduan Executor",
                                tint = Color(0xFFD0BCFF)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1C1B1F),
                        titleContentColor = Color.White
                    )
                )
            }
        },
        floatingActionButton = {
            if (!isEditing) {
                FloatingActionButton(
                    onClick = { viewModel.startNewScript() },
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72),
                    modifier = Modifier.testTag("add_script_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Script")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1C1B1F))
                .padding(innerPadding)
        ) {
            if (isEditing) {
                // Renders the immersive Code Editor interface
                LuaEditor(viewModel = viewModel)
            } else {
                // Renders the main scripts directory dashboard
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Executor Status Chip
                    Card(
                        shape = RoundedCornerShape(50.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        border = BorderStroke(1.dp, Color(0xFF49454F)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Green pulse dot
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF4ADE80), RoundedCornerShape(50.dp))
                                )
                                Text(
                                    text = "Executor Linked: Delta v3.1",
                                    color = Color(0xFFCAC4D0),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "READY TO INJECT",
                                color = Color(0xFFD0BCFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Search Bar and Local Import Trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text("Cari script...", color = Color.Gray, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search icon",
                                    tint = Color.Gray
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedContainerColor = Color(0xFF2B2930),
                                unfocusedContainerColor = Color(0xFF2B2930)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 56.dp)
                                .testTag("search_script_input")
                        )

                        // Import from Local File Button
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2B2930),
                                contentColor = Color(0xFFD0BCFF)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("import_local_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send, // Used as import indicator
                                contentDescription = "Impor Script",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Impor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Category Pill Tabs
                    val categories = listOf("Semua", "Delta", "Codex", "Universal", "Custom")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            val isSelected = categoryFilter == category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(
                                        if (isSelected) Color(0xFFD0BCFF) else Color(0xFF2B2930)
                                    )
                                    .clickable { viewModel.categoryFilter.value = category }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F),
                                        shape = RoundedCornerShape(30.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .testTag("category_tab_$category")
                            ) {
                                Text(
                                    text = category,
                                    color = if (isSelected) Color(0xFF381E72) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Scripts List View
                    if (scripts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "Empty",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "Tidak ada script ditemukan",
                                    color = Color.LightGray,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Klik '+' untuk tambah atau buat file baru",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scripts_list_container")
                        ) {
                            items(scripts, key = { it.id }) { script ->
                                ScriptCard(
                                    script = script,
                                    onEdit = { viewModel.startEditingScript(script) },
                                    onDelete = {
                                        viewModel.deleteScript(script)
                                        Toast.makeText(context, "Script dihapus!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFavorite = { viewModel.toggleFavorite(script) },
                                    onExport = { showExportDialog = script }
                                )
                            }
                        }
                    }
                }
            }

            // Export Dialog showing Options (Raw Copy vs Executor Writefile)
            showExportDialog?.let { script ->
                ExportDialog(
                    script = script,
                    onDismiss = { showExportDialog = null }
                )
            }

            // Helper Info Dialog
            if (showPresetInfoDialog) {
                ExecutorInfoDialog(onDismiss = { showPresetInfoDialog = false })
            }
        }
    }
}

@Composable
fun ScriptCard(
    script: ScriptEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onExport: () -> Unit
) {
    val categoryColor = when (script.category.uppercase()) {
        "DELTA" -> Color(0xFFD0BCFF)
        "CODEX" -> Color(0xFFE8DEF8)
        "UNIVERSAL" -> Color(0xFFFFB300)
        else -> Color(0xFFB0BEC5)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("script_item_card_${script.id}"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2B2930)
        ),
        border = BorderStroke(1.dp, Color(0xFF49454F)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title & Category Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = script.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(BorderStroke(0.5.dp, categoryColor.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = script.category,
                            color = categoryColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }

                Row {
                    // Star Favorite Icon
                    IconButton(onClick = onFavorite) {
                        Icon(
                            imageVector = if (script.isFavorite) Icons.Default.Star else Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = if (script.isFavorite) Color(0xFFFFD600) else Color.DarkGray
                        )
                    }
                    // Delete Button
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF5252)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = script.description.ifEmpty { "Tidak ada deskripsi" },
                color = Color.LightGray,
                fontSize = 13.sp,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Copy/Edit action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Edit Code Button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF49454F)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFCAC4D0)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Script", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Copy Installer export button
                Button(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gunakan / Copy", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun LuaEditor(viewModel: MainViewModel) {
    val context = LocalContext.current
    val tempTitle by viewModel.tempTitle.collectAsStateWithLifecycle()
    val tempCode by viewModel.tempCode.collectAsStateWithLifecycle()
    val tempCategory by viewModel.tempCategory.collectAsStateWithLifecycle()
    val tempDescription by viewModel.tempDescription.collectAsStateWithLifecycle()

    var codeFieldVal by remember {
        mutableStateOf(TextFieldValue(text = tempCode, selection = TextRange(tempCode.length)))
    }

    // Sync field backing input to viewModel on typing
    LaunchedEffect(codeFieldVal.text) {
        if (codeFieldVal.text != tempCode) {
            viewModel.tempCode.value = codeFieldVal.text
        }
    }

    // Helper syntax short-cut triggers
    val helperShortcuts = listOf(
        ShortcutKey("local", "local "),
        ShortcutKey("func", "function() \n\nend"),
        ShortcutKey("game", "game:GetService(\"\")"),
        ShortcutKey("workspace", "workspace"),
        ShortcutKey("wait", "task.wait(1)"),
        ShortcutKey("loadstring", "loadstring(game:HttpGet(''))()"),
        ShortcutKey("writefile", "writefile(\"script.lua\", [[]])"),
        ShortcutKey("print", "print(\"\")"),
        ShortcutKey("[[ ]]", "[[  ]]")
    )

    fun insertTextAtSelection(insertString: String) {
        val currentText = codeFieldVal.text
        val selection = codeFieldVal.selection
        val start = selection.min
        val end = selection.max
        val newText = currentText.substring(0, start) + insertString + currentText.substring(end)
        val newCursor = start + insertString.length
        codeFieldVal = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
    ) {
        // Core Editor Top Actions Custom Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { viewModel.cancelEditing() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2B2930),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Batal")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Batal")
            }

            Text(
                text = if (viewModel.selectedScript.value == null) "Buat Script Lua" else "Edit Script Lua",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Button(
                onClick = { viewModel.saveScript() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Simpan")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        }

        // Script Setup Form Fields (Collapsible/VerticalScroll)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .weight(1f)
        ) {
            // Script Metadata Box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                border = BorderStroke(1.dp, Color(0xFF49454F)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Informasi Script", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Title Text Field
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { viewModel.tempTitle.value = it },
                        label = { Text("Nama Script", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("editor_title_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description text box
                    OutlinedTextField(
                        value = tempDescription,
                        onValueChange = { viewModel.tempDescription.value = it },
                        label = { Text("Deskripsi Singkat", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("editor_desc_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Selection Options
                    Text("Kategori Executor:", color = Color.LightGray, fontSize = 12.sp)
                    val categories = listOf("Delta", "Codex", "Universal", "Custom")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = tempCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFD0BCFF) else Color(0xFF1F2735))
                                    .clickable { viewModel.tempCategory.value = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color(0xFF381E72) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Real-Time Syntax Coloring Editor Console
            Text(
                text = "Workspace Kode LUA:",
                color = Color(0xFFD0BCFF),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Shortcuts Panel insert bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(Color(0xFF2B2930), RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, Color(0xFF49454F)), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                helperShortcuts.forEach { sh ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF49454F))
                            .clickable { insertTextAtSelection(sh.insertValue) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sh.label,
                            color = Color(0xFFD0BCFF),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Code Text Field Console Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C1B1F))
                    .border(BorderStroke(1.dp, Color(0xFF49454F)), RoundedCornerShape(12.dp))
            ) {
                OutlinedTextField(
                    value = codeFieldVal,
                    onValueChange = { codeFieldVal = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color.White
                    ),
                    visualTransformation = { text ->
                        TransformedText(
                            text = LuaHighlighter.highlight(text.text),
                            offsetMapping = OffsetMapping.Identity
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("editor_code_textarea"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    placeholder = {
                        Text(
                            "-- Mulai koding di sini...",
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ExportDialog(
    script: ScriptEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // High fidelity writefile installer generator output
    val installerCode = """-- Script Terdaftar: ${script.title}
-- Sangat support untuk semua mobile executor (Delta, Codex, Vega X, dll)
-- Jalankan loader installer ini sekali untuk menyimpan script permanen!

local filename = "${script.title.replace(" ", "_").lowercase()}.lua"
local code = [[
${script.code}
]]

if writefile then
    writefile(filename, code)
    print("Script berhasil dipasang dan disimpan ke: workspace/" .. filename)
    -- Menjalankan file yang baru disimpan
    loadstring(readfile(filename))()
else
    -- Jika executor lawas tdk support writefile, tetep run via loadstring instan
    local func = loadstring(code)
    if func then func() else print("Gagal menjalankan script!") end
end
"""

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label disalin ke clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun shareScriptFile() {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, "${script.title}.lua")
            putExtra(Intent.EXTRA_TEXT, script.code)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "Ekspor file Delta Script"))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Gunakan Script Delta",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "ID: ${script.title}",
                    color = Color(0xFFD0BCFF),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Copy Original Raw Code
                Button(
                    onClick = { copyToClipboard(script.code, "Script Mentah") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1B1F), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFD0BCFF))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salin Kode Asli (Raw Lua)")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Copy Executor Installer Code [CRITICAL FOR WORK IN ALL EXECUTORS]
                Button(
                    onClick = { copyToClipboard(installerCode, "Loader Executor") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salin Script Installer (writefile)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Installer ini otomatis membuat file .lua di workspace executor saat ditaruh di Delta/Codex.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                androidx.compose.material3.HorizontalDivider(color = Color(0xFF49454F))

                Spacer(modifier = Modifier.height(14.dp))

                // Share file button
                OutlinedButton(
                    onClick = { shareScriptFile() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF)),
                    border = BorderStroke(1.dp, Color(0xFF49454F)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ekspor / Bagikan (.lua Text)")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_dialog_btn")) {
                    Text("Tutup", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ExecutorInfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Panduan Mobile Executor",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Aplikasi ini mempermudah manajemen script Roblox Lua Anda di HP agar bisa digerakkan ke berbagai Executor seperti Delta, Codex, Fluxus, Vega X, dll.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Cara Menggunakan di Game:", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("1. Pilih script yang Anda inginkan lalu klik tombol 'Gunakan / Copy'.", color = Color.LightGray, fontSize = 12.sp)
                Text("2. Klik 'Salin Script Installer (writefile)'.", color = Color.LightGray, fontSize = 12.sp)
                Text("3. Buka Roblox & aktifkan Executor pilihan Anda.", color = Color.LightGray, fontSize = 12.sp)
                Text("4. Paste dan klik 'Execute' / 'Run'.", color = Color.LightGray, fontSize = 12.sp)
                Text("5. File secara otomatis akan ter-simpan di direktori workspace executor dan langsung dijalankan setiap saat!", color = Color.LightGray, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(14.dp))

                Text("Fungsi file local:", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Anda bisa mengirim script berbentuk file lua atau mengimpor file kodingan lua yang didownload dari internet/browser.", color = Color.LightGray, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(20.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Saya Mengerti", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class ShortcutKey(val label: String, val insertValue: String)

@Composable
fun Greeting(name: String, modifier: Modifier = androidx.compose.ui.Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
