package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LocalScripts
import com.example.data.ScriptEntity
import com.example.data.ScriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScriptRepository

    // Search and filtering state
    val searchQuery = MutableStateFlow("")
    val categoryFilter = MutableStateFlow("Semua") // "Semua", "Delta", "Codex", "Universal", "Custom"

    // Raw script lists from db
    private val _dbScripts = MutableStateFlow<List<ScriptEntity>>(emptyList())

    // Filtered scripts combined state
    val filteredScripts: StateFlow<List<ScriptEntity>>

    // Editing states
    private val _selectedScript = MutableStateFlow<ScriptEntity?>(null)
    val selectedScript: StateFlow<ScriptEntity?> = _selectedScript

    val isEditing = MutableStateFlow(false)

    // Temp variables for creating or quick editing holding area
    val tempTitle = MutableStateFlow("")
    val tempCode = MutableStateFlow("")
    val tempCategory = MutableStateFlow("Custom")
    val tempDescription = MutableStateFlow("")

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ScriptRepository(database.scriptDao())

        // Combine database flows with search query and filter
        filteredScripts = combine(
            repository.allScripts,
            searchQuery,
            categoryFilter
        ) { scripts, query, category ->
            var result = scripts

            // If empty database, check and pre-populate on separate launch
            if (scripts.isEmpty()) {
                prepopulateDatabase()
            }

            if (category != "Semua") {
                result = result.filter { it.category.equals(category, ignoreCase = true) }
            }

            if (query.isNotEmpty()) {
                result = result.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.code.contains(query, ignoreCase = true)
                }
            }
            result
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private fun prepopulateDatabase() {
        viewModelScope.launch {
            val scripts = repository.allScripts.first()
            if (scripts.isEmpty()) {
                LocalScripts.list.forEach { script ->
                    repository.insert(
                        ScriptEntity(
                            title = script.title,
                            code = script.code,
                            category = script.category,
                            description = script.description
                        )
                    )
                }
            }
        }
    }

    fun startNewScript() {
        _selectedScript.value = null
        tempTitle.value = "Script Baru"
        tempCode.value = "-- Tulis script Roblox Lua Anda di sini\nprint(\"Hello World from Delta!\")\n"
        tempCategory.value = "Custom"
        tempDescription.value = "Script buatan sendiri"
        isEditing.value = true
    }

    fun startEditingScript(script: ScriptEntity) {
        _selectedScript.value = script
        tempTitle.value = script.title
        tempCode.value = script.code
        tempCategory.value = script.category
        tempDescription.value = script.description
        isEditing.value = true
    }

    fun cancelEditing() {
        _selectedScript.value = null
        isEditing.value = false
    }

    fun saveScript() {
        viewModelScope.launch {
            val current = _selectedScript.value
            if (current == null) {
                // Create new
                repository.insert(
                    ScriptEntity(
                        title = tempTitle.value.ifEmpty { "Script Tanpa Nama" },
                        code = tempCode.value,
                        category = tempCategory.value,
                        description = tempDescription.value,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } else {
                // Update existing
                repository.insert(
                    current.copy(
                        title = tempTitle.value.ifEmpty { current.title },
                        code = tempCode.value,
                        category = tempCategory.value,
                        description = tempDescription.value,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            isEditing.value = false
            _selectedScript.value = null
        }
    }

    fun toggleFavorite(script: ScriptEntity) {
        viewModelScope.launch {
            repository.insert(script.copy(isFavorite = !script.isFavorite))
        }
    }

    fun deleteScript(script: ScriptEntity) {
        viewModelScope.launch {
            repository.delete(script)
            if (_selectedScript.value?.id == script.id) {
                _selectedScript.value = null
                isEditing.value = false
            }
        }
    }

    fun importScriptText(title: String, code: String) {
        tempTitle.value = title
        tempCode.value = code
        tempCategory.value = "Custom"
        tempDescription.value = "Script berhasil diimpor dari file lokal"
        isEditing.value = true
    }
}
