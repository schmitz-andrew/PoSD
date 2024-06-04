package com.example.foodtracker.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodtracker.FoodTrackerApplication
import com.example.foodtracker.data.Product
import com.example.foodtracker.data.ProductDao
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update


enum class LIST { Home, Cart }

data class MainUiState(
    val txtProdInfo: String = "no barcode scanned",
    val imgProdUrl: String = "",
    val txtOcrData: String  = "no ocr done",

    var currentList: LIST = LIST.Home
)

data class ProductDetailsUiState(
    val id: Int,
    val name: String,
    val quantity: Int,
    val expiryDate: String?,
    val inCart: Boolean = false
)


class MainViewModel(private val productDao: ProductDao) : ViewModel() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun switchToCartList() {
        _uiState.update { it.copy(currentList = LIST.Cart) }
    }

    fun switchToHomeList() {
        _uiState.update { it.copy(currentList = LIST.Home) }
    }

    fun getProductsAtHome(): Flow<List<ProductDetailsUiState>> = productDao.getProductsAtHome().map {
        it.map { product -> product.toProductDetailsUiState()}
    }

    suspend fun insertProductAtHome(name: String, quantity: Int, expiryDate: String?) {
        productDao.insert(Product(0, name, quantity, expiryDate, false))
    }

    fun getProductsInCart(): Flow<List<ProductDetailsUiState>> = productDao.getProductsInCart().map {
        it.map { product -> product.toProductDetailsUiState() }
    }

    suspend fun removeProduct(id: Int) {
        val product = productDao.getProductById(id).firstOrNull()
        if (product != null) {
            productDao.remove(product)
        }
    }

    suspend fun moveProductToCart(id: Int) {
        val product = productDao.getProductById(id).firstOrNull()
        if (product != null) {
            productDao.update(product.copy(inCart = true))
        }
    }

    suspend fun moveProductToHome(id: Int) {
        val product = productDao.getProductById(id).firstOrNull()
        if (product != null) {
            productDao.update(product.copy(inCart = false))
        }
    }

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

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as FoodTrackerApplication)
                MainViewModel(application.database.productDao())
            }
        }
    }
}

/**
 * Extension on [Product] entity to convert it into [ProductDetailsUiState].
 * Currently the types are the same, but maybe we need more information in UI later.
 */
fun Product.toProductDetailsUiState() = ProductDetailsUiState(id, name, quantity, expiryDate, inCart)

