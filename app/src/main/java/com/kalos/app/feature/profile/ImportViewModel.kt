package com.kalos.app.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.export.BackupImporter
import com.kalos.app.core.export.KalosBackup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ImportState {
    object Idle : ImportState
    data class ConfirmRequired(val backup: KalosBackup) : ImportState
    object Importing : ImportState
    object Success : ImportState
    data class Error(val message: String) : ImportState
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importer: BackupImporter,
) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    fun onUriSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = importer.readAndValidate(uri).fold(
                onSuccess = { ImportState.ConfirmRequired(it) },
                onFailure = { ImportState.Error(it.message ?: "Fichier invalide ou illisible") },
            )
        }
    }

    fun onConfirmImport(backup: KalosBackup) {
        _state.value = ImportState.Importing
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = importer.import(backup).fold(
                onSuccess = { ImportState.Success },
                onFailure = { ImportState.Error(it.message ?: "Erreur lors de l'import") },
            )
        }
    }

    fun onCancelImport() {
        _state.value = ImportState.Idle
    }

    fun resetState() {
        _state.value = ImportState.Idle
    }
}
