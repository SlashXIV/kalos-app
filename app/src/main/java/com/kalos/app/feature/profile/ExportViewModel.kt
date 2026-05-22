package com.kalos.app.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.export.BackupExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ExportState {
    object Idle : ExportState
    object Writing : ExportState
    data class Success(val uri: Uri) : ExportState
    data class Error(val message: String) : ExportState
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exporter: BackupExporter,
) : ViewModel() {

    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state

    fun onUriSelected(uri: Uri) {
        _state.value = ExportState.Writing
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = exporter.export(uri).fold(
                onSuccess = { ExportState.Success(uri) },
                onFailure = { ExportState.Error(it.message ?: "Erreur inconnue") },
            )
        }
    }

    fun resetState() {
        _state.value = ExportState.Idle
    }
}
