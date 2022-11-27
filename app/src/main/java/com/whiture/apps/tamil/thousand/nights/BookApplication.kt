package com.whiture.apps.tamil.thousand.nights

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.edit
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.whiture.apps.tamil.thousand.nights.models.Audiomark
import com.whiture.apps.tamil.thousand.nights.models.Bookmark
import org.json.JSONArray
import java.io.File
import java.util.*

class BookApplication: Application() {

    override fun onCreate () {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        MobileAds.initialize(this)
//        MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(
//            Arrays.asList("17E2F9110D870856EE0C36B3264F6A18")).build())
    }

    // region Ratings
    // the method will check for if user can be asked to rate the game
    // call this once when the app / game gets opened, because this will increase the count every time invoked
    fun canRatingNowShown(): Boolean {
        val preferences = getSharedPreferences(AppPref, Context.MODE_PRIVATE)
        val currentVersion = preferences.getInt(PrefAppCurrentVersionNo, 0)
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionCode: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            packageInfo.longVersionCode.toInt() else packageInfo.versionCode
        if (versionCode > currentVersion) {
            // user has just updated the game, reset first asked param
            resetRateusParams()
            setTotalTimesPlayed(1)
        }
        else {
            val hasUserRated = preferences.getBoolean(PrefRateUsHasUserRated, false)
            val hasUserDenied = preferences.getBoolean(PrefRateUsHasUserDenied, false)
            val totalTimesUserOpened = preferences.getInt(PrefRateUsTotalTimesOpened, 0)
            if (!hasUserRated && !hasUserDenied && totalTimesUserOpened > 1) {
                // more than once, second time
                resetTotalTimesPlayed()
                return true
            }
            else {
                setTotalTimesPlayed(preferences.getInt(PrefRateUsTotalTimesOpened, 0) + 1)
            }
        }
        return false
    }

    private fun setTotalTimesPlayed(totalTimesPlayed: Int) {
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putInt(PrefRateUsTotalTimesOpened, totalTimesPlayed)
            apply()
        }
    }

    fun resetTotalTimesPlayed() = setTotalTimesPlayed(0)

    fun setUserRated() = getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
        putBoolean(PrefRateUsHasUserRated, true)
        apply()
    }

    fun setUserDenied() = getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
        putBoolean(PrefRateUsHasUserDenied, true)
        apply()
    }

    private fun resetRateusParams() {
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionCode: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                packageInfo.longVersionCode.toInt() else packageInfo.versionCode
            putInt(PrefAppCurrentVersionNo, versionCode)
            putBoolean(PrefRateUsHasUserRated, false)
            putBoolean(PrefRateUsHasUserDenied, false)
            putInt(PrefRateUsTotalTimesOpened, 0)
            apply()
        }
    }
    // endregion

    // method to check if the device is online or not
    fun isDeviceOnline(): Boolean {
        (applicationContext.getSystemService(
            Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.let { cm ->
            if (Build.VERSION.SDK_INT < 23) {
                cm.activeNetworkInfo?.let { ni ->
                    return ni.isConnected && (ni.type == ConnectivityManager.TYPE_WIFI
                            || ni.type == ConnectivityManager.TYPE_MOBILE)
                }
            }
            else {
                cm.activeNetwork?.let{ n ->
                    cm.getNetworkCapabilities(n).let { nc ->
                        if (nc != null) {
                            return nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                                    || nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        }
                    }
                }
            }
        }
        return false
    }

    fun getFCMDeviceToken() = getSharedPreferences(AppPref, Context.MODE_PRIVATE).let {
        it.getString(PrefFCMDeviceToken, "").toString()
    }

    fun setFCMDeviceToken (deviceToken: String) {
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putString(PrefFCMDeviceToken, deviceToken)
            commit()
        }
    }

    // to know if the user has been shown the popup to inform about the latest update
    fun getLatestUpdateInformed() = getSharedPreferences(AppPref, Context.MODE_PRIVATE).let {
        it.getBoolean(PrefLatestUpdate, false)
    }

    // set it as the user has shown the popup to inform about the latest update
    fun setLatestUpdateInformed() {
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putBoolean(PrefLatestUpdate, true)
            commit()
        }
    }
    // books
    fun getShelfBookIds(): Array<Int> = if (AC.bookId > -1) getDownloadedBookIds() + arrayOf(
        AC.bookId) else getDownloadedBookIds()

    private fun getDownloadedBookIds(): Array<Int> = getSharedPreferences(AppPref, Context.MODE_PRIVATE).let {
        it.getStringSet(PrefDownloadedBookIDs, emptySet())?.mapNotNull { it.toIntOrNull() }?.toTypedArray()
    } ?: emptyArray()

    fun bookDownloaded(id: Int) {
        val finalBooks = (getDownloadedBookIds() + arrayOf(id)).toSet().map { it.toString() }
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putStringSet(PrefDownloadedBookIDs, finalBooks.toSet())
            commit()
        }
    }

    // method to store the last read page in user preferences
    fun bookLastRead(id: Int, sectionId: Int, chapterId: Int, pageId: Int, fontSize: Int,
                     fontFace: Int, bg: Int, orientation: Boolean) {
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putInt(PrefLastReadBookId, id)
            putInt(PrefLastReadSectionId, sectionId)
            putInt(PrefLastReadChapterId, chapterId)
            putInt(PrefLastReadPageId, pageId)
            putInt(PrefLastReadFontSize, fontSize)
            putInt(PrefLastReadFontFace, fontFace)
            putInt(PrefLastReadBackground, bg)
            putBoolean(PrefLastReadOrientation, orientation)
            commit()
        }
    }

    // method to fetch the last opened page of the given book
    fun getLastOpenedDetail(bookId: Int): Bookmark? = JSONArray(getSharedPreferences(AppPref,
        Context.MODE_PRIVATE).getString(PrefLastOpenedBooks, "[]")).objectArray().map {
        Bookmark.parse(it) }.firstOrNull { it.bookId == bookId }

    // method to set the last opened page details for the given book
    fun setLastOpenedDetail(bookmark: Bookmark) {
        var openedBooks = JSONArray(getSharedPreferences(AppPref, Context.MODE_PRIVATE).getString(
            PrefLastOpenedBooks, "[]")).objectArray().map { Bookmark.parse(it) }.toTypedArray()
        val updated = openedBooks.any { it.updateLastOpened(bookmark) }
        if (!updated) { // add new
            openedBooks += arrayOf(bookmark)
        }
        val result = JSONArray().apply { openedBooks.forEach { this.put(it.deparse()) } }
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putString(PrefLastOpenedBooks, result.toString(2))
            apply()
        }
    }

    // method to add a new bookmark
    fun addBookmark(bookmark: Bookmark) {
        val bookmarks = JSONArray(getSharedPreferences(AppPref, Context.MODE_PRIVATE).getString(
            PrefBookmarks, "[]")).apply { put(bookmark.deparse()) }
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putString(PrefBookmarks, bookmarks.toString(2))
            apply()
        }
    }

    // method to get Array<Bookmark>
    fun getBookmarks() = JSONArray(getSharedPreferences(AppPref, Context.MODE_PRIVATE).getString(
        PrefBookmarks, "[]")).objectArray().map { Bookmark.parse(it) }.toTypedArray()

    // method to remove the given bookmark
    fun removeBookmark(bookmark: Bookmark) {
        val bookmarks = JSONArray(getSharedPreferences(AppPref, Context.MODE_PRIVATE).getString(
            PrefBookmarks, "[]")).objectArray().map { Bookmark.parse(it) }.filter {
            it != bookmark }
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putString(PrefBookmarks, JSONArray().apply { bookmarks.forEach {
                put(it.deparse()) } }.toString(2))
            apply()
        }
    }

    fun resetPageSettings() {
        getSharedPreferences(AppPref, MODE_PRIVATE).edit().apply {
            putInt(PrefLastReadChapterId, 1)
            putInt(PrefLastReadPageId, 0)
            putInt(PrefLastReadFontSize, DefaultFontSizeIndex)
            putInt(PrefLastReadFontFace, DefaultFontFaceIndex)
            putInt(PrefLastReadBackground, DefaultReadModeIndex)
            putBoolean(PrefLastReadOrientation, DefaultScreenOrientation)
            apply()
        }
    }

    // method to store the last heard album in user preferences
    fun bookLastHeard(audiomark: Audiomark) {
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putInt(PrefLastHeardBookId, audiomark.bookId)
            putInt(PrefLastHeardAlbumIndex, audiomark.albumIndex)
            putInt(PrefLastHeardAlbumDuration, audiomark.duration)
            commit()
        }
    }

    // method to fetch the last heard album of the given book
    fun getLastHeardDetail(bookId: Int): Audiomark? = JSONArray(getSharedPreferences(AppPref,
        Context.MODE_PRIVATE).getString(PrefLastHeardBooks, "[]")).objectArray().map {
        Audiomark.parse(it) }.firstOrNull { it.bookId == bookId }

    // method to set the last heard album details for the given book
    fun setLastHeardDetail(audiomark: Audiomark) {
        var openedBooks = JSONArray(getSharedPreferences(AppPref, Context.MODE_PRIVATE).getString(
            PrefLastHeardBooks, "[]")).objectArray().map { Audiomark.parse(it) }.toTypedArray()
        val updated = openedBooks.any { it.updateLastOpened(audiomark) }
        if (!updated) { // add new
            openedBooks += arrayOf(audiomark)
        }
        val result = JSONArray().apply { openedBooks.forEach { this.put(it.deparse()) } }
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putString(PrefLastHeardBooks, result.toString(2))
            apply()
        }
    }

    // method to add a new audio mark
    fun addAudiomark(audiomark: Audiomark) {
        val audiomarks = JSONArray(getSharedPreferences(AppPref, Context.MODE_PRIVATE).getString(
            PrefAudiomarks, "[]")).apply { put(audiomark.deparse()) }
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putString(PrefAudiomarks, audiomarks.toString(2))
            apply()
        }
    }

    // method to get Array<Audiomark>
    fun getAudiomarks() = JSONArray(getSharedPreferences(AppPref, Context.MODE_PRIVATE).getString(
        PrefAudiomarks, "[]")).objectArray().map { Audiomark.parse(it) }.toTypedArray()

    // method to remove the given audiomark
    fun removeAudiomark(audiomark: Audiomark) {
        val audiomarks = JSONArray(getSharedPreferences(AppPref, Context.MODE_PRIVATE).getString(
            PrefAudiomarks, "[]")).objectArray().map { Audiomark.parse(it) }.filter {
            it != audiomark }
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putString(PrefAudiomarks, JSONArray().apply { audiomarks.forEach {
                put(it.deparse()) } }.toString(2))
            apply()
        }
    }

    // check if the user has been shown the gesture help or not
    fun hasUserShownGestureHelp() = getSharedPreferences(AppPref,
        Context.MODE_PRIVATE).getBoolean(PrefUserShownGesturesHelp, false)

    // method to set the gesture help as shown
    fun userHasShownGestureHelp() {
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putBoolean(PrefUserShownGesturesHelp, true)
            apply()
        }
    }

    // method to find out if the given file is available in the local folder or not
    fun hasLocalAssetFile(filePath: String, fileName: String): Boolean {
        return File("$filePath/$fileName").exists()
    }

    // articles
    // method to save the favorite articles - string of JSON Array
    fun saveFavoriteArticles(articles: String) {
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putString(PrefFavoriteArticles, articles)
            commit()
        }
    }

    // method to fetch the favorite articles - string representation of the JSON Array
    fun loadFavouriteArticles(): String? = getSharedPreferences(AppPref,
        Context.MODE_PRIVATE).getString(PrefFavoriteArticles, null)

    // method to fetch the liked article ids
    fun getLikedArticles(): Array<Int> = getSharedPreferences(AppPref,
        Context.MODE_PRIVATE).getStringSet(PrefLikedArticles, null)?.map { it.toInt()
    }?.toTypedArray() ?: emptyArray()

    // method to save the liked article id
    fun saveLikedArticle(id: Int) {
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putStringSet(PrefLikedArticles, (arrayOf(id) + getLikedArticles()).map {
                it.toString() }.toSet())
            commit()
        }
    }

    // method to fetch the disliked article ids
    fun getDislikedArticles(): Array<Int> = getSharedPreferences(AppPref,
        Context.MODE_PRIVATE).getStringSet(PrefDislikedArticles, null)?.map { it.toInt()
    }?.toTypedArray() ?: emptyArray()

    // method to save the disliked article id
    fun saveDislikedArticle(id: Int) {
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putStringSet(PrefDislikedArticles, (arrayOf(id) + getDislikedArticles()).map {
                it.toString() }.toSet())
            commit()
        }
    }

    // method to save the favorites and history words
    fun saveArticleViewFontSize(previousSizeAdd: Float) {
        getSharedPreferences(AppPref, Context.MODE_PRIVATE).edit {
            putFloat(PrefArticleFontSize, previousSizeAdd)
            commit()
        }
    }

    fun getArticleViewFontSize() : Float {
        return getSharedPreferences(AppPref, Context.MODE_PRIVATE).getFloat(PrefArticleFontSize, 0f)
    }

}

