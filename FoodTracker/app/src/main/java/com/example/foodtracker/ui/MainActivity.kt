package com.example.foodtracker.ui

import android.content.ActivityNotFoundException
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.foodtracker.ui.theme.FoodTrackerTheme
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.Integer.parseInt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


var code: String? = null

const val TAG = "TEST_CODE"


class MainActivity : ComponentActivity() {

    val viewModel: MainViewModel by viewModels(factoryProducer = { MainViewModel.factory })

    val getPictureResult =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            run {
                viewModel.parseDateFromImage(
                    bitmap,
                    { Log.e(TAG, "ocr cancelled") },
                    { err -> run { Log.e(TAG, err.toString()) } }
                )
            }
        }

    lateinit var scanner: GmsBarcodeScanner

    lateinit var requestQueue: RequestQueue

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                // TODO update ui elements?
//            }
//        }

        scanner = GmsBarcodeScanning.getClient(this)
        requestQueue = Volley.newRequestQueue(this)

        setContent {
            FoodTrackerTheme {
                // A surface container using the 'background' color from the theme
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(this, Modifier.padding(it))
                }
            }
        }
    }
}

val txtProdInfo = mutableStateOf("No barcode scanned")
val imgProdUrl = mutableStateOf("")

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

/***
 * This composable is the representation of a item in a list.
 * It contains the presentation of the product but also two buttons to ether remove or move the item to the other list.
 */
@Composable
fun ProductItem(
    product: ProductDetailsUiState,
    onAddToCart: (ProductDetailsUiState) -> Unit,
    onMoveToHome: (ProductDetailsUiState) -> Unit,
    onRemoveClick: (ProductDetailsUiState) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        //Item information representation
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = product.name,
                modifier = Modifier.weight(1f),
            )
            Text(text = "Qty: ${product.quantity}")
            Text(text = "Expires: ${product.expiryDate ?: "unknown"}")
        }
        //The buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            //Ether a button to move to the cart or to the home list
            if (!product.inCart) {
                IconButton(onClick = { onAddToCart(product) }) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = "Add to Cart"
                    )
                }
            } else {
                IconButton(onClick = { onMoveToHome(product) }) {
                    Icon(imageVector = Icons.Filled.Home, contentDescription = "Move to Home List")
                }
            }
            //The remove button
            IconButton(onClick = { onRemoveClick(product) }) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Delete")
            }
        }
    }
}


/***
 * This composable function represents the popup window to add/modify an item of a list.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemPopup(
    onDismissRequest: () -> Unit,
    onConfirmationRequest: (String, String, String) -> Unit
) {

    //A function to convert milliseconds to local date.
    fun convertMillisToLocalDate(millis: Long): LocalDate {
        return Instant
            .ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    //A function to convert milliseconds to local date with format.
    fun convertMillisToLocalDateWithFormatter(
        date: LocalDate,
        dateTimeFormatter: DateTimeFormatter
    ): LocalDate {
        //Convert the date to a long in millis using a date form mater.
        val dateInMillis = LocalDate.parse(date.format(dateTimeFormatter), dateTimeFormatter)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        //Convert the millis to a localDate object.
        return Instant
            .ofEpochMilli(dateInMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    //A function to convert a date to a string.
    fun dateToString(date: LocalDate): String {
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM, yyyy", Locale.getDefault())
        val dateInMillis = convertMillisToLocalDateWithFormatter(date, dateFormatter)
        return dateFormatter.format(dateInMillis)
    }

    //A val to keep track of the date
    val dateState = rememberDatePickerState()
    val millisToLocalDate = dateState.selectedDateMillis?.let {
        convertMillisToLocalDate(it)
    }
    val expiryDate = millisToLocalDate?.let {
        dateToString(millisToLocalDate)
    } ?: ""
    var name by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("") }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { onDismissRequest() }) {
        Box(Modifier.background(color = Color.White)) {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity") }
                )
                if (showDatePickerDialog) {
                    DatePickerDialog(
                        onDismissRequest = {
                            showDatePickerDialog = false
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showDatePickerDialog = false
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    ) {
                        DatePicker(
                            state = dateState,
                            showModeToggle = true
                        )
                    }
                }

                Row {
                    TextButton(onClick = {
                        onConfirmationRequest(
                            name,
                            quantityText,
                            expiryDate
                        )
                    }) {
                        Text("Confirm")
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(activity: MainActivity, modifier: Modifier = Modifier) {

    val coroutineScope = rememberCoroutineScope()

    val uiState by activity.viewModel.uiState.collectAsStateWithLifecycle(
        lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    )
    val (prodInfo, prodImgUrl, ocrData, currentList) = uiState
    var showAddItemPopup by rememberSaveable { mutableStateOf(false) }

    val productsAtHome by activity.viewModel.getProductsAtHome().collectAsState(emptyList())
    val productsInCart by activity.viewModel.getProductsInCart().collectAsState(emptyList())

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
                Icon(imageVector = Icons.Filled.DateRange, contentDescription = "Expiry Date")
            }
            IconButton(onClick = { showAddItemPopup = true }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Plus")
            }
            if (showAddItemPopup) {
                AddItemPopup(
                    onDismissRequest = { showAddItemPopup = false },
                    onConfirmationRequest = { name, quantity, expiryDate ->
                        coroutineScope.launch {
                            val quantityInt = quantity.toIntOrNull()
                            if (quantityInt != null) {
                                activity.viewModel.insertProductAtHome(
                                    name,
                                    quantityInt,
                                    expiryDate
                                )
                                showAddItemPopup = false // Dismiss popup after adding
                            }
                        }
                    }
                )
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
                //TODO: Replace Star with QR-Code
                Icon(imageVector = Icons.Filled.Star, contentDescription = "QR Code")
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { activity.viewModel.switchToHomeList() },
                    enabled = currentList != LIST.Home
                ) {
                    Text("At Home")
                }
                Button(
                    onClick = { activity.viewModel.switchToCartList() },
                    enabled = currentList != LIST.Cart
                ) {
                    Text("In Cart")
                }
            }

            when (currentList) {
                LIST.Home -> LazyColumn(modifier = Modifier.weight(1f)) {
                    if (productsAtHome.isEmpty()) {
                        item(0) {
                            Text(
                                text = "You don't have any food at home.",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        items(productsAtHome.size, { productsAtHome[it].id }) { index ->
                            val product = productsAtHome[index]
                            ProductItem(
                                product,
                                onAddToCart = {
                                    coroutineScope.launch {
                                        activity.viewModel.moveProductToCart(it.id)
                                    }
                                },
                                onMoveToHome = {
                                    coroutineScope.launch {
                                        activity.viewModel.moveProductToHome(it.id)
                                    }
                                },
                                onRemoveClick = {
                                    coroutineScope.launch {
                                        activity.viewModel.removeProduct(it.id)
                                    }
                                }
                            )
                        }
                    }
                }

                LIST.Cart -> LazyColumn(modifier = Modifier.weight(1f)) {
                    if (productsInCart.isEmpty()) {
                        item(0) {
                            Text(
                                text = "Your shopping list is empty.",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        items(productsInCart.size, { productsInCart[it].id }) { index ->
                            val product = productsInCart[index]
                            ProductItem(
                                product,
                                onAddToCart = {
                                    coroutineScope.launch {
                                        activity.viewModel.moveProductToCart(it.id)
                                    }
                                },
                                onMoveToHome = {
                                    coroutineScope.launch {
                                        activity.viewModel.moveProductToHome(it.id)
                                    }
                                },
                                onRemoveClick = {
                                    coroutineScope.launch {
                                        activity.viewModel.removeProduct(it.id)
                                    }
                                }
                            )
                        }
                    }
                }
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

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FoodTrackerTheme {
        MainScreen(MainActivity(), modifier = Modifier)
    }
}
