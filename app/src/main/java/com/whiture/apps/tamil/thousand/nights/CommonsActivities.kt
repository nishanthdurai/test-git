package com.whiture.apps.tamil.thousand.nights

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.loopj.android.http.TextHttpResponseHandler
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.whiture.apps.tamil.thousand.nights.dialogs.*
import com.whiture.apps.tamil.thousand.nights.models.ArticleTag
import com.whiture.apps.tamil.thousand.nights.utils.ZipFileDownloader
import com.whiture.apps.tamil.thousand.nights.utils.ZipFileDownloaderListener
import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.entity.StringEntity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*

/**
 * method to get the custom objects (serializable) from intent
 */
fun <T : Serializable?> Activity.getSerializable(name: String, clazz: Class<T>): T {
    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        intent.getSerializableExtra(name, clazz)!!
    else
        intent.getSerializableExtra(name) as T
}

/**
 * method to inform the user with the dialog
 */
fun Activity.informUser(title: String, message: String, action: AppButton? = null, img:Int? = null,
                        isCancelable: Boolean = true) {
    CustomDialog(this).apply {
        show()
        setCancelable(isCancelable)
        setDialog(title, message, action, img = img)
    }
}

/**
 * method to inform the user with the dialog
 */
fun Activity.showMessage(title: String, message: String, isCancelable: Boolean = true,
                         action: () -> Unit) {
    MessageDialog(this).apply {
        show()
        setCancelable(isCancelable)
        setDialog(title, message, action)
    }
}

//dialog Common Listview
fun Activity.showCommonListDialog(header: String, titles: Array<String>, drawable: Drawable? =
    ContextCompat.getDrawable(this, R.drawable.bg_rect_rounded),
                                  listener: (Int) -> Unit) = CommonListViewDialog(this).apply {
    show()
    setDialog(header, titles, drawable!!, listener)
}

/**
 * function to download a zip file & extract the contents along with dialog
 */
fun Activity.downloadZipWithDialog(
    url: String, fileName: String, targetDir: String, fileLength: Int,
    title: String, message: String,
    successTitle: String, successMessage: String,
    failTitle: String, failMessage: String,
    completion: (Boolean)->Unit) = runOnUiThread {
    val dialog = ProgressBarDialog(this).apply {
        show()
        setDialog(title = title, message = message)
    }
    downloadZip(url, fileName, targetDir, fileLength, { percentage ->
        dialog.setProgress(percentage) }) { success ->
        dialog.dismiss()
        completion(success)
        showMessage(if(success) successTitle else failTitle,
            if(success) successMessage else failMessage) { }
    }
}

fun Activity.downloadZipWithDialog(zipFileName: String, targetDir: String, length: Int, url: String,
    dialogTitle: String, dialogMessage: String, successTitle: String, successMessage: String,
    failTitle: String, failMessage: String, finishActivityOnFail: Boolean,
                                   success: ()->Unit) = runOnUiThread {
    val dialog = ProgressBarDialog(this).apply {
        show()
        setDialog(title = dialogTitle, message = dialogMessage)
    }
    ZipFileDownloader(this, zipFileName, targetDir, length,
        listener = object: ZipFileDownloaderListener() {
            override fun zipFileDownloading(percentage: Int) {
                dialog.setProgress(percentage)
            }

            override fun zipFileDownloadCompleted() {
                dialog.dismiss()
                showMessage(successTitle, successMessage){ success() }
            }

            override fun zipFileFailed() {
                runOnUiThread{
                    dialog.dismiss()
                    showMessage(failTitle, failMessage) {
                        if (finishActivityOnFail) this@downloadZipWithDialog.finish()
                    }
                }
            }
        }, isExternalDir = true).execute(url)
}

/**
 * function to download a zip file & extract the contents
 */
fun Activity.downloadZip(url: String, fileName: String, targetDir: String,
                         fileLength: Int, progress: (Int)->Unit, completion: (Boolean)->Unit) {
    ZipFileDownloader(this, fileName, targetDir, fileLength,
        listener = object: ZipFileDownloaderListener() {
            override fun zipFileDownloading(percentage: Int) {
                runOnUiThread { progress(percentage) }
            }

            override fun zipFileDownloadCompleted() {
                runOnUiThread{ completion(true) }
            }

            override fun zipFileFailed() {
                runOnUiThread{ completion(false) }
            }
        }, isExternalDir = true).execute(url)
}

fun Activity.setStatusBarColor(context: Context, color: Int) {
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.statusBarColor = ContextCompat.getColor(context, color)
}

//method to show shared view
fun Activity.showShareDialog(view: View, header: String) {
    createBitmapFromView(view)?.let { bitmap ->
        SharePreviewDialog(this, bitmap).apply {
            show()
            setDialog(header)
        }
    }
}

// Show the ad banner in the given activity layout
fun Activity.showAdBanner(layout: ViewGroup) {
    // Create layout params for the alignment of ad
    val outMetrics = DisplayMetrics()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display?.getRealMetrics(outMetrics)
    }
    else {
        val display = windowManager.defaultDisplay
        display.getMetrics(outMetrics)
    }

    val density = outMetrics.density
    var adWidthPixels = layout.width.toFloat()
    if (adWidthPixels == 0f) {
        adWidthPixels = outMetrics.widthPixels.toFloat()
    }
    val adWidth = (adWidthPixels / density).toInt()
    val adHeight = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    val adView = AdView(this).apply {
        adUnitId = AdIdBanner
        setAdSize(adHeight)
        loadAd(AdRequest.Builder().build())
    }
}

//dialog promotional app from notification
fun Activity.showPromoDialog(appTitle: String?, appDescription: String?,
                              appImgUrl: String?, okBtnHandler: () -> Unit) {
    if (appTitle != null && appDescription != null && appImgUrl != null) {
        PromoAppDialog(this, appTitle, appDescription, appImgUrl).apply {
            show()
            setDialog(okBtnHandler)
        }
    }
    else {
        openPlayStoreChannel()
    }
}

fun Activity.createBitmapFromView(view: View): Bitmap? = Bitmap.createBitmap(view.width, view.height,
    Bitmap.Config.ARGB_8888).apply {
    val canvas = Canvas(this)
    view.background = ContextCompat.getDrawable(applicationContext, R.drawable.bg_rect_rounded)
    val background = view.background
    background?.draw(canvas)
    view.draw(canvas)
}

fun Activity.getBitmapFromView(view: View, width: Int, height: Int): Bitmap
        = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
    view.draw(Canvas(this))
}

fun Activity.getImageUri(bitmap: Bitmap): Uri? {
    var uri: Uri? = null
    try {
        val currentMillis = System.currentTimeMillis()
        val file = File(this.cacheDir, "$currentMillis.png")
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.flush()
        fileOutputStream.close()
        file.setReadable(true, false)
        uri = Uri.fromFile(file)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".fileprovider", file)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return uri
}

// method to load the json file from the internal dir and returns it as the JSON Object
fun Activity.loadJSONObject(name: String): JSONObject? {
    val storeFile = File("${this.filesDir.absolutePath}/$name")
    if (storeFile.exists()) {
        val data = FileInputStream(storeFile).bufferedReader().use { it.readText() }
        try {
            return JSONObject(data)
        }
        catch (e: JSONException) { e.printStackTrace() }
    }
    return null
}

// method to load the json file from the internal dir and returns it as the JSON Object
fun Activity.loadJSONArray(name: String): JSONArray? {
    val storeFile = File("${this.filesDir.absolutePath}/$name")
    if (storeFile.exists()) {
        val data = FileInputStream(storeFile).bufferedReader().use { it.readText() }
        try {
            return JSONArray(data)
        }
        catch (e: JSONException) { e.printStackTrace() }
    }
    return null
}

fun Activity.facebookIntent(): Intent {
    var uri = Uri.parse("https://www.facebook.com/tamilandroid")
    try {
        val applicationInfo = packageManager.getApplicationInfo("com.facebook.katana", 0)
        if (applicationInfo.enabled) {
            uri = Uri.parse("fb://facewebmodal/f?href=https://www.facebook.com/tamilandroid")
        }
    } catch (ignored: PackageManager.NameNotFoundException) {
    }
    return Intent(Intent.ACTION_VIEW, uri)
}

// method to load a file from the assets dir and returns it as JSON Object
fun Activity.loadJSONObjectFromAssets(file: String): JSONObject? {
    try {
        return JSONObject(assets.open(file).bufferedReader().use { it.readText() })
    }
    catch (e: JSONException) { e.printStackTrace() }
    return null
}

// method to load a file from the assets dir and returns it as JSON Array
fun Activity.loadJSONArrayFromAssets(file: String): JSONArray? {
    try {
        return JSONArray(assets.open(file).bufferedReader().use { it.readText() })
    }
    catch (e: JSONException) { e.printStackTrace() }
    return null
}

fun Activity.mailNow() = startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
    putExtra(Intent.EXTRA_EMAIL, arrayOf("sudhakar.kanakaraj@outlook.com"))
    putExtra(Intent.EXTRA_SUBJECT, "Reg: 1001 Nights Suggestions") // TODO: update the subject
    putExtra(Intent.EXTRA_TEXT, "Hi Sudhakar,\n\n")
    selector = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:") } }, "Send feedback.."))

// method to load a file from the internal dir and returns it as String
fun Context.loadTextInternal(file: String): String = File(
    "${this.filesDir.absolutePath}/$file").let {
    if (it.exists()) FileInputStream(it).bufferedReader().use { charset -> charset.readText() } else "" }

// method to load a file from the assets dir and returns it as String
fun Context.loadTextAssets(file: String): String =
    assets.open(file).bufferedReader().use { it.readText() }

// method to load a file from the internal dir and returns it as String
fun Context.loadBMLInternal(bookId: Int, sectionId: Int): Array<String> = FileInputStream(File(
    "${this.filesDir.absolutePath}/$bookId/section$sectionId.bml")).bufferedReader()
    .readLines().toTypedArray()

// method to load a file from the assets dir and returns it as String
fun Context.loadBMLAssets(sectionId: Int): Array<String> =
    assets.open("section$sectionId.bml").bufferedReader().readLines().toTypedArray()

fun pixelsToSp (context: Context, px: Float) =
    px/context.resources.displayMetrics.scaledDensity

fun Activity.openFaceBook () = runOnUiThread {
    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/tamilandroid")).apply {
        flags = FLAG_ACTIVITY_NEW_TASK
        if (this.resolveActivity(packageManager) != null) {
            startActivity(this)
        }
    }
}

fun Activity.color(color: Int) = ContextCompat.getColor(this, color)

fun Activity.dpToPixel(dp: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
}

// presence of notch display can be detected, if the height of the status bar is more than 24dp,
// if so we must remove top margin(Inset) to our view from the entire screen height
fun Activity.getNotchDisplayHeight(): Int = resources.getIdentifier("status_bar_height", "dimen",
    "android").let { r -> if (r > 0) resources.getDimensionPixelSize(r) else 0 }.let { h ->
    if (h > dpToPixel(24.0f)) h else 0 }

/**
 * method to open the play store page
 */
fun Activity.openPlayStore(appId: String) = runOnUiThread {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId"))
    intent.flags = FLAG_ACTIVITY_NEW_TASK
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    }
    else { // try to open it as a web page
        Intent(Intent.ACTION_VIEW, Uri.parse(
            "https://play.google.com/store/apps/details?id=$appId")).apply {
            flags = FLAG_ACTIVITY_NEW_TASK
            if (this.resolveActivity(packageManager) != null) {
                startActivity(this)
            }
        }
    }
}

// method to open the play store page
fun Activity.openPlayStoreChannel() = runOnUiThread {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
        "market://search?q=pub:Sudhakar_Kanakaraj"))
    intent.flags = FLAG_ACTIVITY_NEW_TASK
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    }
    else { // try to open it as a web page
        Intent(Intent.ACTION_VIEW, Uri.parse(
            "https://play.google.com/store/apps/developer?id=Sudhakar_Kanakaraj")).apply {
            flags = FLAG_ACTIVITY_NEW_TASK
            if (this.resolveActivity(packageManager) != null) {
                startActivity(this)
            }
        }
    }
}

fun Activity.showYoutube(id: String) = startActivity(Intent(Intent.ACTION_VIEW,
    Uri.parse("https://www.youtube.com/watch?v=$id")))

fun Activity.showYoutubeChannel() = startActivity(Intent(Intent.ACTION_VIEW,
    Uri.parse("https://www.youtube.com/channel/UC3Vzvfd4dJUZfxXHiQCGX5A")))

// method to open the play store page
fun Activity.openHttpPage(url: String) = runOnUiThread {
    Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER).apply {
        data = Uri.parse(url)
        startActivity(this)
    }
}

// method to save the http get JSON to given file
fun Activity.saveHttpGetJSON(url: String, folder: String, fileName: String,
    onCompletion: (Boolean) -> Unit) {
    httpGetJSON(url) { _, _, jsonObject, jsonArray ->
        fun writeToFile(content: String) {
            // create folder if any
            val dir = if (folder.isNotBlank()) {
                val targetDir = File(filesDir, folder)
                targetDir.mkdirs()
                targetDir
            }
            else {
                filesDir
            }
            val file = File(dir, fileName)
            file.appendText(content)
        }

        when {
            jsonObject != null -> {
                writeToFile(jsonObject.toString())
                onCompletion(true)
            }
            jsonArray != null -> {
                writeToFile(jsonArray.toString())
                onCompletion(true)
            }
            else -> {
                onCompletion(false)
            }
        }
    }
}

// get JSON from http get call
fun Activity.httpGetJSON(url: String, responder: (Boolean, Int, JSONObject?, JSONArray?) -> Unit) {
    AsyncHttpClient().let {
        it.setMaxRetriesAndTimeout(2, 0)
        it.get(this, url, object: JsonHttpResponseHandler() {

            override fun onSuccess(
                statusCode: Int, headers: Array<out Header>?,
                response: JSONArray?
            ) {
                responder(true, statusCode, null, response)
            }

            override fun onSuccess(
                statusCode: Int, headers: Array<out Header>?,
                response: JSONObject?
            ) {
                responder(true, statusCode, response, null)
            }

            override fun onSuccess(
                statusCode: Int, headers: Array<out Header>?,
                responseString: String?
            ) {
                responder(true, statusCode, null, null)
            }

            override fun onFailure(
                statusCode: Int, headers: Array<out Header>?,
                responseString: String?, throwable: Throwable?
            ) {
                Log.d("WHILOGS", "HTTP Error: Code: $statusCode, Response: $responseString")
                Log.e("WHILOGS", "HTTP Error", throwable)
                responder(false, statusCode, null, null)
            }

            override fun onFailure(
                statusCode: Int, headers: Array<out Header>?,
                throwable: Throwable?, errorResponse: JSONArray?
            ) {
                Log.d("WHILOGS", "HTTP Error: Code: $statusCode, Response: $errorResponse")
                Log.e("WHILOGS", "HTTP Error", throwable)
                responder(false, statusCode, null, null)
            }

            override fun onFailure(
                statusCode: Int, headers: Array<out Header>?,
                throwable: Throwable?, errorResponse: JSONObject?
            ) {
                Log.d("WHILOGS", "HTTP Error: Code: $statusCode, Response: $errorResponse")
                Log.e("WHILOGS", "HTTP Error", throwable)
                responder(false, statusCode, null, null)
            }
        })
    }
}

// get text from http get call
fun Activity.httpGetText(url: String, responder: (Boolean, Int, String?) -> Unit) {
    AsyncHttpClient().let {
        it.setMaxRetriesAndTimeout(2, 0)
        it.get(this, url, object: TextHttpResponseHandler() {

            override fun onSuccess(
                statusCode: Int, headers: Array<out Header>?,
                responseString: String?
            ) {
                responder(true, statusCode, responseString)
            }

            override fun onFailure(
                statusCode: Int, headers: Array<out Header>?,
                responseString: String?, throwable: Throwable?
            ) {
                Log.d("WHILOGS", "HTTP Error: Code: $statusCode, Response: $responseString")
                Log.e("WHILOGS", "HTTP Error", throwable)
                responder(false, statusCode, null)
            }
        })
    }
}

// get JSON from http post call
fun Activity.httpPostJSON(url: String, jsonData: JSONObject,
    responder: (Boolean, Int, JSONObject?, JSONArray?) -> Unit) {
    AsyncHttpClient().let {
        it.setMaxRetriesAndTimeout(2, 0)
        it.post(this, url, StringEntity(jsonData.toString()),
            "application/json", object: JsonHttpResponseHandler() {

                override fun onSuccess(statusCode: Int, headers: Array<out Header>?,
                                       response: JSONArray?) {
                    responder(true, statusCode, null, response)
                }

                override fun onSuccess(statusCode: Int, headers: Array<out Header>?,
                                       response: JSONObject?) {
                    responder(true, statusCode, response, null)
                }

                override fun onSuccess(statusCode: Int, headers: Array<out Header>?,
                                       responseString: String?) {
                    responder(true, statusCode, null, null)
                }

                override fun onFailure(statusCode: Int, headers: Array<out Header>?, responseString: String?,
                                       throwable: Throwable?) {
                    Log.d("WHILOGS", "HTTP Error: Code: $statusCode, Response: $responseString")
                    Log.e("WHILOGS", "HTTP Error", throwable)
                    responder(false, statusCode, null, null)
                }

                override fun onFailure(statusCode: Int, headers: Array<out Header>?,
                    throwable: Throwable?, errorResponse: JSONArray?) {
                    Log.d("WHILOGS", "HTTP Error: Code: $statusCode, Response: $errorResponse")
                    Log.e("WHILOGS", "HTTP Error", throwable)
                    responder(false, statusCode, null, null)
                }

                override fun onFailure(statusCode: Int, headers: Array<out Header>?, throwable: Throwable?,
                                       errorResponse: JSONObject?) {
                    Log.d("WHILOGS", "HTTP Error: Code: $statusCode, Response: $errorResponse")
                    Log.e("WHILOGS", "HTTP Error", throwable)
                    responder(false, statusCode, null, null)
                }
            })
    }
}

// for replacing image on the button press event - shows the pressed image for 10th of a second
fun ImageView.buttonPress(drawable: Int, drawablePressed: Int, handler: () -> Unit) {
    this.setImageResource(drawablePressed)
    Handler(Looper.getMainLooper()).postDelayed({
        this.setImageResource(drawable)
        handler()
    }, 100)
}

// for adding animation on the button press event
fun ImageView.buttonPress(context: Context, handler: () -> Unit) {
    val onClickAnimation = AnimationUtils.loadAnimation(context, R.anim.on_click)
    onClickAnimation.setAnimationListener(object: Animation.AnimationListener {
        override fun onAnimationRepeat(p0: Animation?) {}
        override fun onAnimationEnd(p0: Animation?) {
            handler()
        }

        override fun onAnimationStart(p0: Animation?) {}
    })
    setOnClickListener { startAnimation(onClickAnimation) }
}

// for adding animation on the button press event
fun ImageView.buttonLitePress(context: Context, handler: () -> Unit) {
    val onClickAnimation = AnimationUtils.loadAnimation(context, R.anim.on_click_lite)
    onClickAnimation.setAnimationListener(object: Animation.AnimationListener {
        override fun onAnimationRepeat(p0: Animation?) {}
        override fun onAnimationEnd(p0: Animation?) {
            handler()
        }

        override fun onAnimationStart(p0: Animation?) {}
    })
    setOnClickListener { startAnimation(onClickAnimation) }
}

// for adding animation on the view press event
fun View.clickAnimation(context: Context, handler: () -> Unit) {
    val onClickAnimation = AnimationUtils.loadAnimation(context, R.anim.on_click)
    onClickAnimation.setAnimationListener(object: Animation.AnimationListener {
        override fun onAnimationRepeat(p0: Animation?) {}
        override fun onAnimationEnd(p0: Animation?) {
            handler()
        }
        override fun onAnimationStart(p0: Animation?) {}
    })
    setOnClickListener { startAnimation(onClickAnimation) }
}

// for adding animation on the view press event
fun View.liteClickAnimation(context: Context, handler: () -> Unit) {
    val onClickAnimation = AnimationUtils.loadAnimation(context, R.anim.on_click_lite)
    onClickAnimation.setAnimationListener(object: Animation.AnimationListener {
        override fun onAnimationRepeat(p0: Animation?) {}
        override fun onAnimationEnd(p0: Animation?) {
            handler()
        }

        override fun onAnimationStart(p0: Animation?) {}
    })
    setOnClickListener { startAnimation(onClickAnimation) }
}

fun Button.buttonPress(context: Context, handler: () -> Unit) {
    val onClickAnimation = AnimationUtils.loadAnimation(context, R.anim.button_click)
    onClickAnimation.setAnimationListener(object: Animation.AnimationListener {
        override fun onAnimationRepeat(p0: Animation?) {}
        override fun onAnimationEnd(p0: Animation?) {
            handler()
        }

        override fun onAnimationStart(p0: Animation?) {}
    })
    setOnClickListener { startAnimation(onClickAnimation) }
}

// method to make the text view with clickable texts
fun TextView.setTextClickable(text: String, lineBreaker: String, words: Array<String>,
                              click: (String) -> Unit) {
    fun setClickable(text: String) = object: ClickableSpan() {
        override fun onClick(view: View) {
            click(text)
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.color = Color.BLUE
        }
    }

    this.setText(SpannableStringBuilder(text), TextView.BufferType.SPANNABLE)
    val spannableText = this.text as Spannable
    val texts = text.split(lineBreaker)
    if (texts.isNotEmpty()) {
        repeat(texts.size) { i ->
            if (words.contains(texts[i].lowercase())) {
                val startingIndex = text.indexOf(texts[i])
                spannableText.setSpan(
                    ForegroundColorSpan(Color.BLUE), startingIndex,
                    startingIndex + texts[i].length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableText.setSpan(
                    UnderlineSpan(), startingIndex,
                    startingIndex + texts[i].length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableText.setSpan(
                    setClickable(texts[i]), startingIndex,
                    startingIndex + texts[i].length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        this.movementMethod = LinkMovementMethod.getInstance()
        this.text = spannableText.trim()
    }
}

fun Fragment?.runOnUiThread(action: () -> Unit) {
    this ?: return
    if (!isAdded) return // Fragment not attached to an Activity
    activity?.runOnUiThread(action)
}

fun ComponentActivity.isActive(): Boolean = (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))

// method to download the zip file
fun Activity.downloadZipFile(fileName: String, targetDir: String, fileSizeInKBs: Int,
    title: String, message: String, url: String, onSuccess: () -> Unit, onFailure: () -> Unit) {

    val dialog = ProgressBarDialog(this).apply {
        show()
        setDialog(title = title, message = message)
    }

    ZipFileDownloader(this, fileName, targetDir, fileSizeInKBs,
        listener = object: ZipFileDownloaderListener() {
            override fun zipFileDownloading(percentage: Int) {
                dialog.setProgress(percentage)
            }

            override fun zipFileDownloadCompleted() {
                dialog.dismiss()
                showMessage("பதிவிறக்கம் முடிந்தது", "காலண்டர் தகவல்கள் பதிவிறக்கம் " +
                            "வெற்றிகரமாக நிறைவுற்றது.") { }
                onSuccess()
            }

            override fun zipFileFailed() {
                runOnUiThread {
                    dialog.dismiss()
                    showMessage("பதிவிறக்கம் தோல்வி அடைந்தது",
                        "தயவு செய்து தங்கள் மொபைலில் இன்டர்நெட்டை சரிசெய்து, மீண்டும் முயற்சிக்கவும்.") { }
                    onFailure()
                }
            }
        }, isExternalDir = true).execute(url)
}

fun Activity.openHTMLFromDownloadedFolder(assetFilePath: String, isPdfShared: Boolean = false,
                                          sharePdfUrl: String = "") = startActivity(Intent(
    this, HTMLActivity::class.java).apply {
    putExtra("url", "file:///${filesDir.absolutePath}/$assetFilePath")
    putExtra("IS_SHARE_VISIBLE", isPdfShared)
    putExtra("SHARE_PDF_URL", sharePdfUrl)
})

fun Activity.openHTMLFromAssets(assetFilePath: String) = startActivity(Intent(this,
    HTMLActivity::class.java).apply { putExtra("HTML_ASSETS", assetFilePath) })

//share text
fun Activity.shareText(shareText: String) {
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, shareText +
                "\n${resources.getString(R.string.share_message)}")
        type = "text/plain"
    }, "பகிருங்கள்"))
}

//share text
fun Activity.shareLink(shareText: String) {
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }, "பகிருங்கள்"))
}

// set the click listener for the list view which has both title and items
fun ListView.setItemTitleClickListener(items: Array<Pair<String, String>>, titleIndices: Array<Int>,
    listener: (String, String) -> Unit) {
    setOnItemClickListener { _, _, index, _ ->
        if (!titleIndices.contains(index)) {
            listener(items[index].first, items[index].second)
        }
    }
}

/**
 * method to ask the user with two different options
 */
fun Activity.askUser(title: String, message: String, option1: AppButton, option2: AppButton,
                     isCancelable: Boolean = true) {
    val dialog = CustomDialog(this)
    dialog.show()
    dialog.setDialog(title, message, option1, option2)
    dialog.setCancelable(isCancelable)
}

/**
 * method to show the help us dialog
 */
fun Activity.showHelpUs(rateNowAction: () -> Unit,
    mailNowAction: () -> Unit,
    moreAppsAction: () -> Unit) {
    val dialog = HelpUsDialog(this)
    dialog.show()
    dialog.setDialog(rateNowAction, mailNowAction, moreAppsAction)
}

/**
 * method to show the loading dialog indefinitely
 */
fun Activity.showLoading() = LoadingDialog(this).apply { show() }

// Show the ad banner in the given activity layout
fun Activity.showBanner(layout: ViewGroup) {
    // Create layout params for the alignment of ad
    val display = windowManager.defaultDisplay
    val outMetrics = DisplayMetrics()
    display.getMetrics(outMetrics)

    val density = outMetrics.density
    var adWidthPixels = layout.width.toFloat()
    if (adWidthPixels == 0f) {
        adWidthPixels = outMetrics.widthPixels.toFloat()
    }
    val adWidth = (adWidthPixels / density).toInt()
    val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    AdView(this).apply {
        setAdSize(adSize)
        adUnitId = AdIdBanner
        loadAd(AdRequest.Builder().build())
        layout.addView(this)
    }
}

fun<T> Activity.open(cls: Class<T>, extras: Map<String, Any> = emptyMap()) {
    startActivity(Intent(this, cls).apply {
        for ((k, v) in extras) {
            when (v) {
                is Int -> putExtra(k, v)
                is Long -> putExtra(k, v)
                is String -> putExtra(k, v)
                is Boolean -> putExtra(k, v)
            }
        }
    })
}

fun Activity.showImage(url: String, imageView: ImageView, isCache: Boolean = true) {
    ImageLoader.getInstance().also {
        if (!it.isInited) it.init(ImageLoaderConfiguration.Builder(this).build())
        it.displayImage(url, imageView, DisplayImageOptions.Builder()
            .cacheInMemory(isCache).cacheOnDisk(isCache)
            .bitmapConfig(Bitmap.Config.ARGB_8888).showImageOnFail(R.drawable.no_image)
            .build())
    }
}

fun Activity.divideTags(tags: Array<ArticleTag>): Array<Array<ArticleTag>> {
    // get screen width
    val outMetrics = DisplayMetrics()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display?.getRealMetrics(outMetrics) else
        windowManager.defaultDisplay?.getMetrics(outMetrics)
    // subtract margins
    val screenWidthInSp: Float = outMetrics.widthPixels / resources.displayMetrics.scaledDensity -
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics)
    val paint = Paint()
    paint.textSize = 18f
    val result = mutableListOf<Array<ArticleTag>>()
    val temp = mutableListOf<ArticleTag>()
    var size = 0f
    for (tag in tags) {
        val width = paint.measureText(tag.name)
        if (size + width > screenWidthInSp || temp.size >= 3) { // max 3 per each
            result.add(temp.toTypedArray())
            temp.clear()
            temp.add(tag)
            size = width
        }
        else {
            temp.add(tag)
            size += width
        }
    }
    if (temp.isNotEmpty()) result.add(temp.toTypedArray())
    return result.toTypedArray()
}

fun Activity.setStatusBarColor(color: Int) {
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.statusBarColor = ContextCompat.getColor(this, color)
}

/**
 * method to show the progress bar dialog
 */
fun Activity.showProgress(title: String, message: String) = ProgressBarDialog(this).apply {
    show()
    setDialog(title, message)
}

// show the rate now dialog
fun Activity.showRateNowDialog() {
    val app = application as App
    RateNowDialog(this).apply {
        show()
        setDialog(rateNowHandler = {
            openPlayStore(packageName)
            app.setUserRated()
        }, rateLaterHandler = {
            app.resetTotalTimesPlayed()
        }, notRatingHandler = {
            app.setUserDenied()
        })
    }
}

fun Activity.showSearchDialog(hint: String? = null, desc: String? = null, action: (String) -> Unit) {
    SearchDialog(this).apply {
        show()
        setDialog(hint = hint, desc = desc, handler = action)
    }
}

/**
 * method to inform the user with the dialog
 */
fun Fragment.showMessage(title: String, message: String,
                         action: () -> Unit) = MessageDialog(requireActivity()).apply {
    show()
    setDialog(title, message, action)
}

// method to hide the keyboard from the currently focused view
fun Activity.hideKeyboard() = currentFocus?.let { view ->
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

// method to get the given drawable from the local files dir
fun Activity.getBgDrawable(imgName: String): Drawable? {
    try {
        return Drawable.createFromStream(FileInputStream(
            File("${filesDir.absolutePath}/$imgName")), null)
    }
    catch (e: Exception) {
        Log.e("WHILOGS", "Exception while loading the background drawable", e)
    }
    return null
}

// method to download assets file with progress bar displaying there
fun Activity.downloadAssets(url: String, file: String, folder: String,
                           size: Int, failHandler: (()->Unit)? = null,
                           handler: (()->Unit)? = null) = runOnUiThread {
    // dialog for showing the progress
    val dialog = ProgressBarDialog(this).apply {
        show()
        setDialog(title = resources.getString(R.string.please_wait),
            message = resources.getString(R.string.assets_downloading_message))
    }
    dialog.setProgress(0)
    // start the zip file downloader
    ZipFileDownloader(this, file, folder, size, object : ZipFileDownloaderListener() {
        override fun zipFileDownloading(percentage: Int) = runOnUiThread {
            dialog.setProgress(percentage)
        }

        override fun zipFileDownloadCompleted() = runOnUiThread {
            dialog.dismiss()
            showMessage(resources.getString(R.string.download_finished),
                resources.getString(R.string.assets_downloaded_message)) {
                handler?.let { handler -> handler() } }
        }

        override fun zipFileFailed() = runOnUiThread {
            dialog.dismiss()
            showMessage(resources.getString(R.string.download_failed),
                resources.getString(R.string.check_internet_message)) {
                failHandler?.let { handler -> handler() } }
        }
    }, isExternalDir = true).execute(url)
}

// method to hide the navigation bar
fun AppCompatActivity.hideSystemNavigationBar() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.let {
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.navigationBarColor = getColor(R.color.colorBlack)
            it.hide(WindowInsets.Type.systemBars())
        }
    }
    else {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
    }
}

// width and height of the screen
fun Activity.getWidthHeight(): Pair<Int, Int> {
    val height: Int
    val width: Int
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val metrics: WindowMetrics = this.getSystemService(
            WindowManager::class.java).currentWindowMetrics
        width = metrics.bounds.width()
        height = metrics.bounds.height()
    } else {
        val display = windowManager.defaultDisplay
        val metrics = if (display != null) {
            DisplayMetrics().also { display.getRealMetrics(it) }
        } else {
            Resources.getSystem().displayMetrics
        }
        width = metrics.widthPixels
        height = metrics.heightPixels
    }
    return Pair(width, height)
}

fun Activity.openLastReadPage(bookId: Int, sectionId: Int = 1, chapterId: Int = 1,
                              pageId: Int = 0) {
    val pref = getSharedPreferences(AppPref, Context.MODE_PRIVATE)
    startActivity(Intent(this, BookReaderActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(IntentBookId, pref.getInt(PrefLastReadBookId, bookId))
        putExtra(IntentSectionId, pref.getInt(PrefLastReadSectionId, sectionId))
        putExtra(IntentChapterId, pref.getInt(PrefLastReadChapterId, chapterId))
        putExtra(IntentPageId, pref.getInt(PrefLastReadPageId, pageId))
        putExtra(IntentFontSize, pref.getInt(PrefLastReadFontSize, DefaultFontSizeIndex))
        putExtra(IntentFontFace, pref.getInt(PrefLastReadFontFace, DefaultFontFaceIndex))
        putExtra(IntentBackgroundId, pref.getInt(PrefLastReadBackground, DefaultReadModeIndex))
        putExtra(IntentScreenOrientation, pref.getBoolean(PrefLastReadOrientation,
            DefaultScreenOrientation))
    })
}

fun Activity.openLastHeardPage() {
    val pref = getSharedPreferences(AppPref, Context.MODE_PRIVATE)
    startActivity(Intent(this, AudioPlayerActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(IntentBookId, pref.getInt(PrefLastHeardBookId, AC.bookId))
        putExtra(IntentAlbumId, pref.getInt(PrefLastHeardAlbumIndex, 0))
        putExtra(IntentAudioDuration, pref.getInt(PrefLastHeardAlbumDuration, 0))
    })
}


