package com.whiture.apps.tamil.thousand.nights

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.whiture.apps.tamil.thousand.nights.models.AlbumData
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ads parameters
// TODO: change this before publishing
const val isTestAds = false
val AdIdBanner: String
    get() = if (isTestAds) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-3095581080847461/9566783462"
val AdIdInterstitial: String
    get() = if (isTestAds) "ca-app-pub-3940256099942544/1033173712" else "ca-app-pub-3095581080847461/3001375115"

// logs
const val WHILOGS = "WHILOGS"

fun log(message: String) {
    Log.d(WHILOGS, message)
}

fun logE(e: Throwable?, message: String) {
    Log.e(WHILOGS, message, e)
}
typealias AC = AppConstants
typealias AppButton = Pair<String, ()->Unit>
typealias App = BookApplication

// User preferences constants
const val AppPref = "whiture.reader.pref"
const val PrefLatestUpdate = "PrefLatestUpdateV2"

// intent values
const val IntentBookId = "bookId"
const val IntentAlbumId = "albumId"
const val IntentAudioDuration = "albumDuration"
const val IntentSectionId = "sectionId"
const val IntentChapterId = "chapterId"
const val IntentPageId = "pageId"
const val IntentBackgroundId = "backgroundId"
const val IntentFontSize = "fontSize"
const val IntentFontFace = "fontFace"
const val IntentScreenOrientation = "screenOrientation"

// for rating popup
const val PrefAppCurrentVersionNo = "PrefAppCurrentVersionNoV2"
const val PrefRateUsHasUserRated = "PrefRateUsHasUserRatedV2"
const val PrefRateUsHasUserDenied = "PrefRateUsHasUserDeniedV2"
const val PrefRateUsTotalTimesOpened = "PrefRateUsTotalTimesOpenedV2"

//articles
const val PrefFavoriteArticles = "PrefFavoriteArticleIDsV2"
const val PrefArticleFontSize = "PrefArticleLastSetFontSizeV2"
const val PrefLikedArticles = "PrefLikedArticleIDsV2"
const val PrefDislikedArticles = "PrefDislikedArticleIDsV2"

// books
const val PrefDownloadedBookIDs = "PrefDownloadedBookIDsV2"
const val PrefLastOpenedBooks = "PrefLastOpenedBooksV2"
const val PrefLastHeardBooks = "PrefLastHeardBooksV2"
const val PrefUserShownGesturesHelp = "PrefUserShownGesturesHelpV2"

// media player
const val PrefUserRewardedTimeStamp = "PrefUserRewardedTimeStampV2"
const val PrefPlaybackSpeed = "PrefPlaybackSpeedV2"

// Book Store constants
// 0, 1, 2 -> Day, Night, Sepia
const val PrefLastReadBackground = "LastReadBackgroundV2"
// 0, 1, 2, 3, 4 -> Very Small, Small, Medium, Large, Xtra Large
const val PrefLastReadFontSize = "LastReadFontSizeV2"
// 0, 1, 2 -> System, Mylai, Comic etc
const val PrefLastReadFontFace = "LastReadFontFaceV2"
// portrait (false) / landscape (true)
const val PrefLastReadOrientation = "LastReadOrientationV2"
const val PrefBookmarks = "PrefBookmarksV2"
const val PrefAudiomarks = "PrefAudiomarksV2"

const val PrefLastReadBookId = "LastReadBookIdV2"
const val PrefLastReadSectionId = "LastReadSectionIdV2"
const val PrefLastReadChapterId = "LastReadChapterIdV2"
const val PrefLastReadPageId = "LastReadPageIdV2"

const val PrefLastHeardBookId = "PrefLastHeardBookIdV2"
const val PrefLastHeardAlbumIndex = "PrefLastHeardAlbumIndexV2"
const val PrefLastHeardAlbumDuration = "PrefLastHeardAlbumDurationV2"

// default settings
const val DefaultFontSizeIndex = 2
const val DefaultFontFaceIndex = 0
const val DefaultReadModeIndex = 1
const val DefaultScreenOrientation = false // portrait

// FCM
const val PrefFCMDeviceToken = "PrefFCMDeviceTokenV3"

/**
 * As the MediaService is handling the Media Player, the interface acts as a listener class for the service
 * An activity can implement this to listen for the media player events
 */
interface IAudioListener {
    fun bufferingStarted(data: AlbumData) // the initial buffering to start playing the video has started
    fun playStarted(data: AlbumData) // audio is being started playing
    fun playPaused(data: AlbumData) // audio is being paused
    fun playing(time: Long) // the audio is being played with given timeline, this method will be called every 1s
    fun playCompleted(data: AlbumData) // the audio has been completed
    fun error(data: AlbumData) // an error occurred while trying to play the audio
}

/**
 *  Type1 -> Title
 *  Type2 -> Title with Description
 *  Type3 -> Banner, Title with Description
 *  Type4 -> Thumbnail, Title with Description
 *  Type5 -> Horizontal scrollview
 *  Type6 -> Tag
 *  Type7 -> Category
 *  Type8 -> Keyword
 *  Type9 -> Author
 *  Type10 -> Loading
 *  Type11 -> List
 *  Type12 -> Title, Banner with Description
 *  Type13 -> Four image categories
 *  Type14 -> Title, img, Description
 *  Type15 -> Title, img, Audio - for now, not supporting remove it later
 *  Type16 -> Title, Youtube ID
 *  Type100 -> Article trends
 *  */
enum class ArticleType(val value: Int) {
    Type1(1), Type2(2), Type3(3), Type4(4),
    Type5(5), Type6(6), Type7(7), Type8(8),
    Type9(9), Type10(10), Type11(11), Type12(12), Type13(13),
    Type14(14), Type15(15), Type16(16), Type100(100);

    companion object {
        private val values = values()
        val loading: ArticleType get() = Type10
        fun of(value: Int) = values.firstOrNull { it.value == value } ?: Type10
    }
}

enum class PageLayout(val value: Int) {
    Day(0), Night(1), Sepia(2), DarkGrey(3),
    LiteGrey(4), Green(5), ClassicPaper(6);

    companion object {
        private val values = values()
        fun of(value: Int) = values.firstOrNull { it.value == value }
    }
}

// Class Extensions
fun jsonObject(properties: Map<String, Any>) = JSONObject().apply {
    for((k, a) in properties) {
        if (a is String || a is Int || a is Boolean) { this.put(k, a) }
    }
}

fun JSONArray.stringArray() = (0 until this.length()).map { this.getString(it) }.toTypedArray()
fun JSONArray.intArray() = (0 until this.length()).map { this.getInt(it) }.toTypedArray()
fun JSONArray.objectArray(): Array<JSONObject> = (0 until this.length()).map {
    this.getJSONObject(it) }.toTypedArray()
fun JSONArray.jsonArrayArray(): Array<JSONArray> = (0 until this.length()).map {
    this.getJSONArray(it) }.toTypedArray()
fun JSONArray.longArray() = (0 until this.length()).map { this.getLong(it) }.toTypedArray()

fun JSONObject.intArray(arg: String): Array<Int> = try {
    with(getJSONArray(arg)) { intArray() } } catch (e: Exception) { emptyArray() }
fun JSONObject.stringArray(arg: String): Array<String> = try {
    with(getJSONArray(arg)) { stringArray() } } catch (e:Exception) { emptyArray() }
fun JSONObject.objectArray(arg: String): Array<JSONObject> = try {
    with(getJSONArray(arg)) { objectArray() } } catch (e: Exception) { emptyArray() }
fun JSONObject.longArray(arg: String): Array<Long> = try {
    with(getJSONArray(arg)) { longArray() } } catch (e: Exception) { emptyArray() }

fun JSONObject.stringValue(arg: String, default: String = ""): String = try {
    this.getString(arg) } catch(e: Exception) { default }

fun JSONObject.splitValues(attrName: String, splitter: String): Array<String>? = try {
    this.getString(attrName).split(splitter).toTypedArray() } catch (e: Exception) { null }

fun JSONObject.splitIntValues(attrName: String, splitter: String): Array<Int>? = try {
    this.getString(attrName).split(splitter).mapNotNull { it.toIntOrNull() }.toTypedArray()
} catch (e: Exception) { null }

fun JSONObject.arguments(values: Map<String, Any>): JSONObject {
    for ((k, v) in values) this.put(k, v)
    return this
}

fun JSONObject.defaultIfNotFound(argumentName: String, default: Any): Any? =
    if (!isNull(argumentName)) get(argumentName) else default

// method to parse the given value and get date
fun dateFromString(value: String, format: String): Date? = SimpleDateFormat(format).parse(value)

// method to find out dates between these two
fun Date.daysBetween(date: Date): Int = TimeUnit.DAYS.convert(
    this.time - date.time, TimeUnit.MILLISECONDS).toInt()

fun Char.isJoint(): Boolean {
    val type = Character.getType(this)
    return (type.toByte() == Character.NON_SPACING_MARK
            || type.toByte() == Character.ENCLOSING_MARK
            || type.toByte() == Character.COMBINING_SPACING_MARK)
}

fun String.getTamilCharArray(): Array<String> {
    val chars = this.toCharArray()
    val letters = mutableListOf<String>()
    chars.forEachIndexed { index, c ->
        if (index != 0 && c.isJoint()) {
            letters[letters.size - 1] = letters[letters.size - 1] + c.toString()
        }
        else {
            letters.add(c.toString())
        }
    }
    return letters.toTypedArray()
}

// method to get the date from the string
fun String.date(format: String): Date = SimpleDateFormat(format).parse(this)

var Double.twoDecimal: String
    get() {
        val values = this.toString().split(".")
        return if (values.size == 2) {
            if (values[1].length > 1) {
                values[0] + "." + values[1].substring(0..1)
            }
            else {
                values[0] + "." + values[1] + "0"
            }
        }
        else {
            "$this.00"
        }
    }
    private set(_) {}

//method to get bitmap from downloaded path
fun getBitmapFromDownloadPath(context: Context, filePath: String): Bitmap {
    return BitmapFactory.decodeFile("${context.filesDir.absolutePath}/${filePath}.png")
}

fun getImageUri(bitmap: Bitmap?, mContext: Activity): Uri? {
    var uri: Uri? = null
    try {
        val currentMillis = System.currentTimeMillis()
        val file = File(mContext.cacheDir, "$currentMillis.png")
        val fileOutputStream = FileOutputStream(file)
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.flush()
        fileOutputStream.close()
        file.setReadable(true, false)
        uri = Uri.fromFile(file)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID
                    + ".fileprovider", file)
        }
    }
    catch (e: Exception) { e.printStackTrace() }
    return uri
}

// send the given firebase event
fun sendFirebaseEvent(analytics: FirebaseAnalytics, value: String) {
    analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
        putString(FirebaseAnalytics.Param.CONTENT_TYPE, value)
    })
}

fun timeLabel(length: Int): String {
    fun checkZero(num: Int): String = if (num < 10) "0$num" else "$num"
    val secs: Int = length / 1000
    val mins: Int = secs / 60
    val totalHours: Int = mins / 1000
    return if (totalHours > 0) "${checkZero(totalHours)}:${checkZero(mins % 60)}:${checkZero(secs % 60)}"
    else "${checkZero(mins % 60)}:${checkZero(secs % 60)}"
}

fun timeInHoursMins(length: Int): String {
    val secs: Int = length / 1000
    val mins: Int = secs / 60
    val totalHours: Int = mins / 60
    return if (totalHours > 0) "${totalHours}h ${mins % 60}m Left"
    else "0h ${mins % 60}m Left"
}

