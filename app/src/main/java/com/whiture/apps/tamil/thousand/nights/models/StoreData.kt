package com.whiture.apps.tamil.thousand.nights.models

import com.whiture.apps.tamil.thousand.nights.objectArray
import org.json.JSONObject

/**
 * this data class is used to represent the JSON data for the storev2.json file
 * https://cdn.kadalpura.com/books/store/tamil/storev2.json
 * Store Data -> Category Data -> Book Data
 * Store Data -> App Data
 */
data class StoreData(val type: Int, val version: Int, val ref_url: String, val modified: String,
                     var categories: Array<CategoryData>, val books: Array<BookData>,
                     val promotions: Array<AppData> ) {
    companion object {
        fun parse(json: JSONObject) = StoreData(
            type = json.getInt("type"),
            version = json.getInt("version"),
            ref_url = json.getString("reference_url"),
            modified = json.getString("last_modified"),
            categories = json.objectArray("categories").map { CategoryData.parse(it) }.toTypedArray(),
            books = json.objectArray("books").map { BookData.parse(it) }.toTypedArray(),
            promotions = json.objectArray("promotions").map { AppData.parse(it) }.toTypedArray(),
        )
    }

    fun getAudioBooks() = books.filter { it.audio }.toTypedArray()
    fun getBook(id: Int): BookData? = books.firstOrNull { it.id == id }
    fun getCategoryBooks(index: Int) = getBooks(categories[index])
    fun getBooks(category: CategoryData) = getBooks(category.bookIds)
    fun getBooks(ids: Array<Int>) = books.filter { ids.contains(it.id) }.toTypedArray()

    // method to search for the keyword
    fun search(keyword: String): Array<BookData> = books.filter { it.search(keyword) }.toTypedArray()

}

