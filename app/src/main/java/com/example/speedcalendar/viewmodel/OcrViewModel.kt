package com.example.speedcalendar.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.features.ai.ocr.OcrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OcrViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OcrRepository(application)

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitializing = true, error = null) }
            val initialized = repository.ensureInitialized()
            _uiState.update {
                it.copy(
                    isInitializing = false,
                    isReady = initialized,
                    error = if (initialized) null else "OCR 引擎初始化失败"
                )
            }
        }
    }

    fun recognize(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            val result = runCatching { repository.recognize(bitmap) }
            _uiState.update { state ->
                state.copy(
                    isProcessing = false,
                    lastResult = if (result.getOrNull()?.success == true) result.getOrNull()!!.text else state.lastResult,
                    error = result.getOrNull()?.error ?: result.exceptionOrNull()?.message
                )
            }
            bitmap.recycle()
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.release()
    }
}

data class OcrUiState(
    val isInitializing: Boolean = false,
    val isProcessing: Boolean = false,
    val isReady: Boolean = false,
    val lastResult: String = "",
    val error: String? = null
)
