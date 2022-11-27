package com.whiture.apps.tamil.thousand.nights.models

import com.whiture.apps.tamil.thousand.nights.intArray
import org.json.JSONObject

/**
 * this data class is used to represent the JSON data for the storev2.json file
 * https://cdn.kadalpura.com/books/store/tamil/storev2.json
 * each tab is a category basically, they have an array of book ids
 */
data class CategoryData(val name: String, val desc: String, val bookIds: Array<Int>) {
    companion object {
        fun parse(json: JSONObject) = CategoryData(
            name = json.getString("name"),
            desc = json.getString("desc"),
            bookIds = json.intArray("book_ids"),
        )
    }
}

