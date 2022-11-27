package com.whiture.apps.tamil.thousand.nights.models

import com.whiture.apps.tamil.thousand.nights.AC
import org.json.JSONObject

data class ArticleCategory(val id: Int, val name: String, val desc: String) {
    val banner: String get() = "${AC.Content2URL}/articles/ta/CategoryImages/category_${id}.png"

    companion object {
        fun parse(json: JSONObject) = ArticleCategory(id = json.getInt("id"),
            name = json.getString("name"), desc = json.getString("desc"))
    }
}

