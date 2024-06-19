package com.example.foodtracker.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.ui.theme.FoodTrackerTheme
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch


const val TAG = "TEST_CODE"


class MainActivity : ComponentActivity() {

    val viewModel: MainViewModel by viewModels(factoryProducer = { MainViewModel.factory })

    /*val getPictureResult =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            run {
                viewModel.parseDateFromImage(
                    bitmap,
                    { Log.e(TAG, "ocr cancelled") },
                    { err -> run { Log.e(TAG, err.toString()) } }
                )
            }
        }*/

    lateinit var scanner: GmsBarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scanner = GmsBarcodeScanning.getClient(this)

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
            Column {
                Text(text = product.name)
                Text(text = "Qty: ${product.quantity}")
                if (!product.inCart) {
                    Text(text = "Expires: ${product.expiryDate ?: "unknown"}")
                }
            }
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
 * This composable is a dropdown menu which contains the numbers 1-50.
 */
@Composable
fun ScrollableNumberDropdown(
    label: String,
    currentValue: Int,
    onValueChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    TextButton(onClick = { expanded = !expanded }) {
        Row {
            Text("$label: $currentValue")
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select $label")
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxHeight(0.5f)
    ) {
        (1..50).forEach { number ->
            DropdownMenuItem(onClick = {
                expanded = false
                onValueChanged(number)
            }, text = {
                Text(text = number.toString())
            })
        }
    }
}


/***
 * This composable function represents the popup window to add/modify an item of a list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemPopup(
    onDismissRequest: () -> Unit,
    onConfirmationRequest: (String, Int, String) -> Unit,
    pName: String,
) {
    //A val to keep track of the date
    val dateState = rememberDatePickerState()
    var expiryDate by rememberSaveable {
        mutableStateOf(millisToString(dateState.selectedDateMillis))
    }
    var name by rememberSaveable { mutableStateOf(pName) }
    var showDatePickerDialog by rememberSaveable { mutableStateOf(false) }
    var quantity by rememberSaveable { mutableIntStateOf(1) }
    var isFlipped by rememberSaveable { mutableStateOf((true)) }
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Box(
            Modifier.background(color = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                ScrollableNumberDropdown(
                    label = "Quantity",
                    currentValue = quantity,
                    onValueChanged = { newValue -> quantity = newValue }
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = isFlipped,
                        onClick = { isFlipped = true },
                        shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                    ) {
                        Text("Expiry Date")
                    }
                    SegmentedButton(
                        selected = !isFlipped,
                        onClick = { isFlipped = false },
                        shape = RoundedCornerShape(bottomEndPercent = 50, topEndPercent = 50)
                    ) {
                        Text("Bought Date")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = expiryDate)
                    IconButton(onClick = { showDatePickerDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "Choose a date"
                        )
                    }
                }
                Text(text = "Add weeks:")
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { expiryDate = addWeeksToDate(expiryDate, 1) },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(text = "1")
                    }
                    OutlinedButton(
                        onClick = { expiryDate = addWeeksToDate(expiryDate, 2) },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(text = "2")
                    }
                    OutlinedButton(
                        onClick = { expiryDate = addWeeksToDate(expiryDate, 3) }
                    ) {
                        Text(text = "3")
                    }
                }
                if (showDatePickerDialog) {
                    DatePickerDialog(
                        onDismissRequest = {
                            showDatePickerDialog = false
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    expiryDate = millisToString(dateState.selectedDateMillis)
                                    showDatePickerDialog = false
                                }
                            ) {
                                Text("Select")
                            }
                        },
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
                            quantity,
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

@Composable
fun MainScreen(activity: MainActivity, modifier: Modifier = Modifier) {

    val coroutineScope = rememberCoroutineScope()

    val uiState by activity.viewModel.uiState.collectAsStateWithLifecycle(
        lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    )
    val (prodInfo, prodImgUrl, ocrData, showAddItemPopup, currentList) = uiState

    val productsAtHome by activity.viewModel.getProductsAtHome().collectAsState(emptyList())
    val productsInCart by activity.viewModel.getProductsInCart().collectAsState(emptyList())

    Column(modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Food Tracker",
                modifier = Modifier.weight(2f)
            )
            /*IconButton(
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
            }*/
            IconButton(onClick = activity.viewModel::showAddItemPopup) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Plus")
            }
            if (showAddItemPopup) {
                AddItemPopup(
                    onDismissRequest = { activity.viewModel.hideAddItemPopup() },
                    onConfirmationRequest = { name, quantity, expiryDate ->
                        coroutineScope.launch {
                            activity.viewModel.insertProductAtHome(
                                name,
                                quantity,
                                expiryDate
                            )
                            activity.viewModel.hideAddItemPopup()
                        }
                    },
                    pName = prodInfo
                )
            }
            IconButton(
                onClick = {
                    activity.scanner.startScan()
                        .addOnSuccessListener(activity.viewModel::fetchFoodFacts)
                        .addOnCanceledListener { Log.e(TAG, "cancelled!") }
                        .addOnFailureListener { err -> Log.e(TAG, err.toString()) }
                }
            ) {
                //TODO: Replace Star with QR-Code
                Icon(imageVector = Icons.Filled.AddCircle, contentDescription = "QR Code")
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
/*
        Text(prodInfo)
        if (prodImgUrl.isNotBlank()) {
            AsyncImage(
                model = prodImgUrl,
                contentDescription = "Image of product"/*, modifier = Modifier.fillMaxSize()*/
            )
        }
        Text(ocrData)*/
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FoodTrackerTheme {
        MainScreen(MainActivity(), modifier = Modifier)
    }
}
