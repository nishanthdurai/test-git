package com.whiture.apps.tamil.thousand.nights.models

import com.whiture.apps.tamil.thousand.nights.AC
import com.whiture.apps.tamil.thousand.nights.date
import org.json.JSONObject
import java.util.*

data class RelatedArticle(val id: Int, val title: String, val thumbnailUrl: String,
                          val dateOfPublish: String, ) {

    val publishDate: Date get() = dateOfPublish.date("yyyy-MM-dd")

    companion object {
        fun parse(json: JSONObject) = RelatedArticle(id = json.getInt("id"),
            title = json.getString("title"),
            thumbnailUrl = "${AC.Content2URL}/articles/ta/${json.getString("thumbnail_url")}",
            dateOfPublish = json.getString("date_of_publish"))
    }

}

