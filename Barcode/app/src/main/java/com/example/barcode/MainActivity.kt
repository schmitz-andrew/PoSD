package com.example.barcode

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.barcode.ui.theme.BarCodeTheme
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONObject


val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
var code: String? = null
var expDate: String? = null

const val TAG = "TEST_CODE"


data class Product(val name: String, val quantity: Int, val expireDate: String)

private val productsAtHome = listOf(
    Product("Product A", 2, "2024-05-30"),
    Product("Product B", 5, "2024-06-15")
)

private val productsInCart = listOf(
    Product("Product C", 1, ""),
    Product("Product B", 3, "")
)

class MainActivity : ComponentActivity() {

    val getPictureResult =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            run {
                Log.d(TAG, bitmap.toString())
                if (bitmap == null) {
                    Log.e(TAG, "no bitmap returned")
                } else {
                    // TODO consider rotation, check aspect ratio
                    recognizer.process(bitmap, 0)
                        .addOnSuccessListener { text ->
                            showOcrData(text)
                        }
                        .addOnCanceledListener {
                            Log.e(TAG, "ocr cancelled!")
                        }
                        .addOnFailureListener { err ->
                            run { Log.e(TAG, err.toString()) }
                        }
                }
            }
        }

    lateinit var scanner: GmsBarcodeScanner

    lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scanner = GmsBarcodeScanning.getClient(this)
        requestQueue = Volley.newRequestQueue(this)

        setContent {
            BarCodeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(this, items = productsAtHome)
                }
            }
        }
    }
}

val txtProdInfo = mutableStateOf("No barcode scanned")
val imgProdUrl = mutableStateOf("")
val txtOcrData = mutableStateOf("no ocr done")

fun showProductInfo(prodJson: JSONObject) {
    val prodString = prodJson.toString(2)
    val prodCode = prodJson.get("code")
    val prodObj = prodJson.getJSONObject("product")
    val prodName = prodObj.get("product_name")
    val prodImg = prodObj.get("image_small_url")

    Log.i(TAG, prodString)
    txtProdInfo.value = "Code: $prodCode\nProduct name: $prodName"

    if (prodImg.toString().isNotBlank()) {
        imgProdUrl.value = prodImg.toString()
    }
}

fun showError(e: Exception) {
    Log.e(TAG, e.toString())
    txtProdInfo.value = "ERROR: ${e.message}"
}

fun showOcrData(text: Text) {
    Log.d(TAG, text.text)
    // TODO filter out expiry date
    expDate = text.text
    txtOcrData.value = text.text
}

@Composable
fun ProductItem(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = product.name,
                modifier = Modifier.weight(1f),
            )
            Text(text = "Qty: ${product.quantity}")
            Text(text = "Expires: ${product.expireDate}")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { onDeleteClick(product) }) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Delete")
            }
            IconButton(onClick = { onAddToCartClick(product) }) {
                Icon(imageVector = Icons.Filled.ShoppingCart, contentDescription = "Add to Cart")
            }
        }
    }
}


// Implement click handler functions
fun onDeleteClick(product: Product) {
    // Remove the product from the list (update data)
    val index = productsAtHome.indexOf(product)
    if (index != -1) {
        productsAtHome.toMutableList().removeAt(index)
        // Update UI to reflect product removal (call a function to refresh the list)
    }
}

fun onAddToCartClick(product: Product) {
    // Add the product to the cart (handle cart logic)
}

@Composable
fun HomeButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("At Home")
    }
}

@Composable
fun CartButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("In Cart")
    }
}

@Composable
fun MainScreen(activity: MainActivity, modifier: Modifier = Modifier, items: List<Product>) {
    val prodInfo by txtProdInfo
    val prodImgUrl by imgProdUrl
    val ocrData by txtOcrData
    var currentListIndex by remember { mutableIntStateOf(0) }  // Initial index is 0 (first list)

    Column(modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Food Tracker",
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    try {
                        activity.getPictureResult.launch(null)
                    } catch (e: ActivityNotFoundException) {
                        // display error state to the user
                        Log.e(TAG, e.toString())
                    }
                }
            ) {
                Icon(imageVector = Icons.Filled.DateRange, contentDescription = "Expire Date")
            }
            IconButton(onClick = { /* Handle cart button click */ }) {
                Icon(imageVector = Icons.Filled.ShoppingCart, contentDescription = "Cart")
            }
            IconButton(onClick = { /* Handle plus button click */ }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Plus")
            }
            IconButton(
                onClick = {
                    activity.scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            run {
                                code = barcode.rawValue
                                if (code == null) {
                                    Log.e(TAG, "code is empty")
                                } else {
                                    txtProdInfo.value = "Waiting for product information..."
                                    Log.i(TAG, code.orEmpty())
                                    val dbUrl =
                                        "https://world.openfoodfacts.org/api/v2/product/${code.orEmpty()}.json"
                                    val request = JsonObjectRequest(
                                        Request.Method.GET, dbUrl, null,
                                        Response.Listener(::showProductInfo),
                                        Response.ErrorListener(::showError)
                                    )
                                    activity.requestQueue.add(request)
                                }
                            }
                        }
                        .addOnCanceledListener { Log.e(TAG, "cancelled!") }
                        .addOnFailureListener { err -> Log.e(TAG, err.toString()) }
                }
            ) {
                //TO-DO: Replace Star with QR-Code
                Icon(imageVector = Icons.Filled.Star, contentDescription = "QR Code")
            }
        }
        val onListChange = { newIndex: Int -> currentListIndex = newIndex }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HomeButton { onListChange(0) }
                CartButton { onListChange(1) }
            }

            when (currentListIndex) {
                0 -> LazyColumn(modifier = Modifier.weight(1f)) {
                    if (items.isEmpty()) {
                        item {
                            Text(text = "Your list is empty.", modifier = Modifier.fillMaxSize())
                        }
                    } else {
                        items(items = productsAtHome) { product ->
                            ProductItem(product = product)
                        }
                    }
                }
                1 -> LazyColumn(modifier = Modifier.weight(1f)) {
                    if (items.isEmpty()) {
                        item {
                            Text(text = "Your list is empty.", modifier = Modifier.fillMaxSize())
                        }
                    } else {
                        items(items = productsInCart) { product ->
                            ProductItem(product = product)
                        }
                    }
                }
                else -> {}  // Handle potential other list indices
            }
        }

        Text(prodInfo)
        if (prodImgUrl.isNotBlank()) {
            AsyncImage(
                model = prodImgUrl,
                contentDescription = "Image of product"/*, modifier = Modifier.fillMaxSize()*/
            )
        }
        Text(ocrData)
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BarCodeTheme {
        MainScreen(MainActivity(), modifier = Modifier, productsAtHome)
    }
}
