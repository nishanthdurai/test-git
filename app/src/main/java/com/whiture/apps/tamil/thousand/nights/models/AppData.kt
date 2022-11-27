package com.whiture.apps.tamil.thousand.nights.models

import org.json.JSONObject

/**
 * this data class is used to represent the JSON data for the storev2.json file
 * https://cdn.kadalpura.com/books/store/tamil/storev2.json
 * for each apps in promotions tab will be represented by this data class
 */
data class AppData(val id: String, val title: String, val desc: String, val img: String, val rating: Int) {
    companion object {
        fun parse(json: JSONObject) = AppData(
            id = json.getString("id"),
            title = json.getString("title"),
            desc = json.getString("subtitle"),
            img = json.getString("img_url"),
            rating = json.getInt("rating"),
        )
    }
}

