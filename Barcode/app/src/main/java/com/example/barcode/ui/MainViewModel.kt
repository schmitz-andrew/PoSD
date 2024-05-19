package com.example.barcode.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


data class MainUiState(
    val txtProdInfo: String = "no barcode scanned",
    val imgProdUrl: String = "",
    val txtOcrData: String  = "no ocr done"
)


class MainViewModel : ViewModel() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private fun showOcrData(text: Text) {
        Log.d(TAG, text.text)
        //expDate = text.text
        _uiState.update { currentState -> currentState.copy(
            txtOcrData = text.text
        ) }
    }

    fun parseDateFromImage(
        bitmap: Bitmap?, onCanceled: () -> Unit, onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, bitmap.toString())
        if (bitmap == null) {
            Log.e(TAG, "no bitmap returned")
        } else {
            val isPortrait = bitmap.height > bitmap.width;
            // TODO consider rotation, retry +180 if it dsnt work
            recognizer.process(bitmap, if (isPortrait) 0 else 90)
                .addOnSuccessListener(::showOcrData)
                .addOnCanceledListener(onCanceled)
                .addOnFailureListener(onFailure)
        }
        // TODO find date string(s), maybe using regex
    }

    // todo
}