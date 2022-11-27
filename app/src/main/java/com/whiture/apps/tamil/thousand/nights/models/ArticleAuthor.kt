package com.whiture.apps.tamil.thousand.nights.models

import com.whiture.apps.tamil.thousand.nights.AC
import org.json.JSONObject

data class ArticleAuthor(val id: Int, val name: String, val title: String = "") {
    val profile: String get() = "${AC.Content2URL}/articles/author_image/$id.png"

    companion object {
        fun parse(json: JSONObject) = ArticleAuthor(id = json.getInt("id"),
            name = json.getString("name"), title = json.getString("title"))
    }
}
