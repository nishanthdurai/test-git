package com.whiture.apps.tamil.thousand.nights.models

import com.whiture.apps.tamil.thousand.nights.stringArray
import org.json.JSONObject
import java.io.Serializable

/**
 * this data class is used to represent the JSON data for the storev2.json file
 * https://cdn.kadalpura.com/books/store/tamil/storev2.json
 * each book shown in each of the tabs will be represented by this data class
 */
data class BookData(val id: Int, val title: String, val desc: String, val audio: Boolean,
                    val author: String, val price: String, val size: Int, val published: String,
                    val tags: Array<String>, val comments: Array<String>): Serializable {

    companion object {
        fun parse(json: JSONObject) = BookData(
            id = json.getInt("id"),
            title = json.getString("title"),
            desc = json.getString("desc"),
            audio = json.getBoolean("audio"),
            author = json.getString("author"),
            price = json.getString("price"),
            size = json.getInt("size"),
            published = json.getString("published"),
            tags = json.stringArray("tags"),
            comments = json.stringArray("comments"),
        )
    }

    val bookmark: Bookmark get() = Bookmark(bookId = id)

    // method to find if the book contains the given keyword
    fun search(keyword: String): Boolean = (title.lowercase().contains(keyword)
            || desc.lowercase().contains(keyword) || author.lowercase().contains(keyword) ||
            tags.any { it.lowercase().contains(keyword) })

}

