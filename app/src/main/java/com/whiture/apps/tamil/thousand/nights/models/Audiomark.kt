package com.whiture.apps.tamil.thousand.nights.models

import android.content.Intent
import com.whiture.apps.tamil.thousand.nights.*
import org.json.JSONObject

/**
 * Like bookmark, this is audio mark, used for both bookmarks and last heard
 */
data class Audiomark(var bookId: Int,
                     var sectionTitle: String,
                     var chapterTitle: String,
                     var albumIndex: Int,
                     var duration: Int, ) {

    companion object {
        fun parse(json: JSONObject) = Audiomark(bookId = json.getInt("b_id"),
            sectionTitle = json.getString("se"),
            chapterTitle = json.getString("ch"),
            albumIndex = json.getInt("in"),
            duration = json.getInt("du"),
        )
    }

    // method to reverse the parsing by generating a JSONObject
    fun deparse(): JSONObject = JSONObject().arguments(mapOf("b_id" to bookId,
        "se" to sectionTitle, "ch" to chapterTitle, "in" to albumIndex,
        "du" to duration))

    // method to update the current book details with the latest opened
    fun updateLastOpened(audiomark: Audiomark): Boolean {
        if (bookId == audiomark.bookId) {
            // only these details are enough
            sectionTitle = audiomark.sectionTitle
            chapterTitle = audiomark.chapterTitle
            albumIndex = audiomark.albumIndex
            duration = audiomark.duration
            return true
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Audiomark) return false
        return (other.bookId == bookId && other.albumIndex == albumIndex)
    }

    // method to set the bookmark data values to the intent
    fun setValuesToIntent(intent: Intent) {
        intent.putExtra(IntentBookId, bookId)
        intent.putExtra(IntentAlbumId, albumIndex)
        intent.putExtra(IntentAudioDuration, duration)
    }

}
