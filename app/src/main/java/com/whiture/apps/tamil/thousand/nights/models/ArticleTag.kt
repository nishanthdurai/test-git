package com.whiture.apps.tamil.thousand.nights.models

import org.json.JSONObject

data class ArticleTag(val id: Int, val name: String, val desc: String = "") {
    companion object {
        fun parse(json: JSONObject) = ArticleTag(id = json.getInt("id"),
            name = json.getString("name"), desc = json.getString("desc"))
    }
}

