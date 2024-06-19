package com.example.foodtracker.ui

// import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.foodtracker.FoodTrackerApplication
import com.example.foodtracker.data.ExpiryEta
import com.example.foodtracker.data.Product
import com.google.mlkit.vision.barcode.common.Barcode
// import com.google.mlkit.vision.text.Text
// import com.google.mlkit.vision.text.TextRecognition
// import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.json.JSONObject


const val TAG = "FT_UI"


enum class LIST { Home, Cart }

data class MainUiState(
    val txtProdInfo: String = "no barcode scanned",
    val imgProdUrl: String = "",
    val txtOcrData: String  = "no ocr done",
    val showItemPopup: Boolean = false,

    val currentList: LIST = LIST.Home
)

data class ProductDetailsUiState(
    val id: Int,
    val name: String,
    val quantity: Int,
    val expiryDate: String?,
    val inCart: Boolean = false
)


class MainViewModel(application: FoodTrackerApplication) : ViewModel() {

    private val productDao = application.database.productDao()

    private val reminderRepository = application.reminderRepository

    private val requestQueue = Volley.newRequestQueue(application.applicationContext)

    // private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    private suspend fun scheduleReminders(id: Int) {
        reminderRepository.scheduleReminder(id, ExpiryEta.ONE_DAY)
        reminderRepository.scheduleReminder(id, ExpiryEta.ONE_WEEK)
    }

    private fun cancelReminders(product: Product) {
        if (!product.expiryDate.isNullOrBlank()) {
            reminderRepository.cancelReminder(
                product.id,
                product.name,
                product.expiryDate,
                ExpiryEta.ONE_DAY
            )
            reminderRepository.cancelReminder(
                product.id,
                product.name,
                product.expiryDate,
                ExpiryEta.ONE_WEEK
            )
        }
    }

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
        val product = Product(0, name, quantity, expiryDate, false)
        val rowid = productDao.insert(product)
        val productId = productDao.getIdFromRowid(rowid).first()
        scheduleReminders(productId)
    }

    fun getProductsInCart(): Flow<List<ProductDetailsUiState>> = productDao.getProductsInCart().map {
        it.map { product -> product.toProductDetailsUiState() }
    }

    suspend fun removeProduct(id: Int) {
        val product = productDao.getProductById(id).firstOrNull()
        if (product != null) {
            cancelReminders(product)
            productDao.remove(product)
        }
    }

    suspend fun moveProductToCart(id: Int) {
        val product = productDao.getProductById(id).firstOrNull()
        if (product != null) {
            cancelReminders(product)
            productDao.update(product.copy(inCart = true))
        }
    }

    suspend fun moveProductToHome(id: Int) {
        val product = productDao.getProductById(id).firstOrNull()
        if (product != null) {
            scheduleReminders(product.id)
            productDao.update(product.copy(inCart = false))
        }
    }

    /*
    private fun showOcrData(text: Text) {
        Log.d(TAG, text.text)
        //expDate = text.text
        _uiState.update { currentState -> currentState.copy(
            txtOcrData = text.text
        ) }
    }
     */

    fun showAddItemPopup() = _uiState.update { it.copy(showItemPopup = true) }

    fun hideAddItemPopup() = _uiState.update { it.copy(showItemPopup = false) }

    /*
    fun parseDateFromImage(
        bitmap: Bitmap?, onCanceled: () -> Unit, onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, bitmap.toString())
        if (bitmap == null) {
            Log.e(TAG, "no bitmap returned")
        } else {
            val isPortrait = bitmap.height > bitmap.width
            // TODO consider rotation, retry +180 if it doesn't work
            recognizer.process(bitmap, if (isPortrait) 0 else 90)
                .addOnSuccessListener(::showOcrData)
                .addOnCanceledListener(onCanceled)
                .addOnFailureListener(onFailure)
        }
        // TODO find date string(s), maybe using regex
    }
     */


    private fun showProductInfo(prodJson: JSONObject) {
        val prodString = prodJson.toString(2)
        val prodObj = prodJson.getJSONObject("product")
        val prodName = prodObj.get("product_name")
        val code = prodJson.get("code")
        val prodImg = prodObj.get("image_small_url")

        Log.i(TAG, prodString)
        _uiState.update { currentState -> currentState.copy(
            txtProdInfo = "$prodName",
            imgProdUrl = prodImg.toString().ifBlank { code.toString() },
            showItemPopup = true
        ) }
    }

    private fun showError(e: Exception) {
        Log.e(TAG, e.toString())
        _uiState.update { it.copy(txtProdInfo = "ERROR: ${e.message}") }
    }

    fun fetchFoodFacts(barcode: Barcode) {
        val code = barcode.rawValue
        if (code.isNullOrBlank()) {
            Log.e(TAG, "code is empty")
        }
        else {
            _uiState.update { currentState -> currentState.copy(
                txtProdInfo = "Waiting for product information..."
            ) }
            Log.i(TAG, code)
            val dbUrl = "https://world.openfoodfacts.org/api/v2/product/${code}.json"
            val request = JsonObjectRequest(
                Request.Method.GET, dbUrl, null,
                Response.Listener(::showProductInfo),
                Response.ErrorListener(::showError)
            )
            requestQueue.add(request)
        }
    }


    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as FoodTrackerApplication)
                MainViewModel(application)
            }
        }
    }
}

/**
 * Extension on [Product] entity to convert it into [ProductDetailsUiState].
 * Currently the types are the same, but maybe we need more information in UI later.
 */
fun Product.toProductDetailsUiState() = ProductDetailsUiState(id, name, quantity, expiryDate, inCart)

