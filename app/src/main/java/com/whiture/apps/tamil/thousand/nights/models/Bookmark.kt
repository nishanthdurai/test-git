package com.whiture.apps.tamil.thousand.nights.models

import android.content.Intent
import com.whiture.apps.tamil.thousand.nights.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * this is a general data class, used for bookmarks by users, data for shelf books
 * and their last read page
 */
data class Bookmark(
    var bookId: Int = -1,
    var sectionId: Int = 1, // starts from 1
    var sectionTitle: String = "",
    var chapterId: Int = 1, // starts from 1
    var chapterTitle: String = "",
    var pageId: Int = 0, // starts from 0
    var backgroundId: Int = 0,
    var fontSize: Int = 0,
    var fontFace: Int = 0,
    var orientation: Boolean = false, // true - landscape, false - portrait
    var content: String = ""
) {

    companion object {
        fun parse(json: JSONObject) = Bookmark(bookId = json.getInt("b_id"),
            sectionId = json.getInt("s_id"),
            sectionTitle = json.getString("se"),
            chapterId = json.getInt("ch_id"),
            chapterTitle = json.getString("ch"),
            pageId = json.getInt("p_id"),
            backgroundId = json.getInt("bg_id"),
            fontSize = json.getInt("fo_size"),
            fontFace = json.getInt("ff"),
            orientation = json.getBoolean("o"),
            content = json.getString("co")
        )

        fun getBookIds(pref: String?): Array<Int> {
            if (pref != null) {
                return JSONArray(pref).objectArray().map { parse(it) }.map {
                    it.bookId }.toSet().toTypedArray()
            }
            return emptyArray()
        }

    }

    // method to reverse the parsing by generating a JSONObject
    fun deparse(): JSONObject = JSONObject().arguments(mapOf("b_id" to bookId, "s_id" to sectionId,
        "se" to sectionTitle, "ch_id" to chapterId, "ch" to chapterTitle, "p_id" to pageId,
        "bg_id" to backgroundId, "fo_size" to fontSize, "ff" to fontFace, "o" to orientation,
        "co" to content))

    // method to update the current book details with the latest opened
    fun updateLastOpened(bookmark: Bookmark): Boolean {
        if (bookId == bookmark.bookId) {
            // only these details are enough
            sectionId = bookmark.sectionId
            chapterId = bookmark.chapterId
            pageId = bookmark.pageId
            return true
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Bookmark) return false
        return (other.bookId == bookId && other.sectionId == sectionId && other.chapterId == chapterId
                && other.pageId == pageId)
    }

    // method to set the bookmark data values to the intent
    fun setValuesToIntent(intent: Intent) {
        intent.putExtra(IntentBookId, bookId)
        intent.putExtra(IntentSectionId, sectionId)
        intent.putExtra(IntentChapterId, chapterId)
        intent.putExtra(IntentPageId, pageId)

        intent.putExtra(IntentBackgroundId, backgroundId)
        intent.putExtra(IntentFontSize, fontSize)
        intent.putExtra(IntentFontFace, fontFace)
        intent.putExtra(IntentScreenOrientation, orientation)
    }

}

