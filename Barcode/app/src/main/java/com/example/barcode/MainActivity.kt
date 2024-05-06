package com.example.barcode

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.barcode.ui.theme.BarCodeTheme
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.concurrent.Executors


val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
var code: String? = null
var expDate: String? = null

class MyUrlRequestCallback: UrlRequest.Callback() {

    private var reqString = ""
    public var json: JSONObject? = null

    override fun onRedirectReceived(
        request: UrlRequest?,
        info: UrlResponseInfo?,
        newLocationUrl: String?
    ) {
        request?.followRedirect()
    }

    override fun onResponseStarted(request: UrlRequest?, info: UrlResponseInfo?) {
        // deal with headers and status code TODO
        request?.read(ByteBuffer.allocateDirect(102400))
    }

    override fun onReadCompleted(
        request: UrlRequest?,
        info: UrlResponseInfo?,
        byteBuffer: ByteBuffer?
    ) {
        reqString = reqString.plus(byteBuffer?.array()?.toString(Charset.forName("utf-8")))
        Log.d("henlo", reqString)
        byteBuffer?.clear()
        request?.read(byteBuffer)
    }

    override fun onSucceeded(request: UrlRequest?, info: UrlResponseInfo?) {
        Log.i("henlo", request.toString())
        Log.i("henlo", info.toString())
        json = JSONObject("{".plus(reqString.substringAfter('{')))
        //TODO("Not yet implemented onSucceeded")
    }

    override fun onFailed(request: UrlRequest?, info: UrlResponseInfo?, error: CronetException?) {
        Log.e("henlo", error.toString())
        //TODO("Not yet implemented onFailed")
    }

    override fun onCanceled(request: UrlRequest?, info: UrlResponseInfo?) {
        Log.e("henlo", "request cancelled")
        reqString = ""
    }
}


class MainActivity : ComponentActivity() {

    public val getPictureResult = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
        bitmap -> run {
            Log.d("henlo", bitmap.toString())
            if (bitmap == null) {
                Log.e("henlo", "no bitmap returned")
            }
            else {
                // TODO consider rotation
                recognizer.process(bitmap, 0)
                    .addOnSuccessListener {
                        text -> run {
                            Log.d("henlo", text.text)
                            // TODO filter out expiry date
                            expDate = text.text
                        }
                    }
                    .addOnCanceledListener {
                        Log.e("henlo", "ocr cancelled!")
                    }
                    .addOnFailureListener {
                        err -> run {Log.e("henlo", err.toString())}
                    }
            }
        }
    }

    public lateinit var scanner: GmsBarcodeScanner

    public lateinit var cronet: CronetEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scanner = GmsBarcodeScanning.getClient(this)
        cronet = CronetEngine.Builder(this).build()

        setContent {
            BarCodeTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting(this)
                }
            }
        }
    }

}

@Composable
fun Greeting(activity: MainActivity, modifier: Modifier = Modifier) {
    Column (modifier) {
        Button(
            onClick = {
                try {
                    activity.getPictureResult.launch(null)
                } catch (e: ActivityNotFoundException) {
                    // display error state to the user
                    Log.e("henlo", e.toString())
                }
            }
        ) {
            Text("Scan expiry date")
        }
        Button(
            onClick = {
                activity.scanner.startScan()
                    .addOnSuccessListener { barcode -> run {
                        code = barcode.rawValue
                        if (code == null) {
                            Log.e("henlo", "code is empty")
                        }
                        else {
                            Log.i("henlo", code.orEmpty())
                            val db_url = "https://world.openfoodfacts.org/api/v2/product/${code.orEmpty()}.json"
                            val cb = MyUrlRequestCallback()
                            val builder = activity.cronet.newUrlRequestBuilder(
                                db_url,
                                cb,
                                Executors.newSingleThreadExecutor()
                            )
                            val request = builder.build()
                            request.start()
                            while (!request.isDone || cb.json == null) {
                                Log.d("henlo", "in progress")
                            }
                            Log.i("henlo", cb.json!!.toString(2))
                        }
                    }}
                    .addOnCanceledListener { Log.e("henlo", "cancelled!") }
                    .addOnFailureListener { err -> Log.e("henlo", err.toString()) }
            }
        ) {
            Text("Scan barcode")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BarCodeTheme {
        Greeting(MainActivity())
    }
}