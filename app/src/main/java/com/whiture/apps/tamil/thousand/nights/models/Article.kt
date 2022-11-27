package com.whiture.apps.tamil.thousand.nights.models

import com.whiture.apps.tamil.thousand.nights.*
import org.json.JSONArray
import org.json.JSONObject

// this is not the data class for the article
// this is used for storing the value of each article returned in home page
// https://api.kadalpura.com/articles/home
data class Article(val type: ArticleType) {
    var id: Int? = null
    var title: String? = null
    var desc: String? = null
    var bannerUrl: String? = null
    var thumbnailUrl: String? = null
    var author: String? = null
    var publishedDate: String? = null
    var totalViews: Int = 0
    var totalLikes: Int = 0
    var totalDislikes: Int = 0
    var similarArticles: Array<Article> = emptyArray()
    var keyword: String? = null
    var categoryImgUrls: Array<Int> = emptyArray()
    var audioFileId: Int? = null
    var audioLength: Int? = null
    var audioURL: String? = null
    var youtubeId: String? = null

    companion object {
        fun parse(json: JSONObject): Article = (if (json.has("type")) Article(
            type = ArticleType.of(json.getInt("type"))) else Article(
            type = ArticleType.Type11)).apply { prepare(json) }

        fun favorites(json: String): Array<Article> = if (json.isNotEmpty())
            JSONArray(json).objectArray().map { Article(type = ArticleType.Type11).apply {
                prepare(it) } }.toTypedArray() else emptyArray()

        fun collection(json: JSONObject): Array<Article> = json.objectArray(
            "articles").map { parse(it) }.toTypedArray()

        fun collection(json: JSONObject, type: ArticleType): Array<Article> = json.objectArray(
            "articles").map { json -> Article(type = type).also { it.prepare(json) } }.toTypedArray()

        val loading: Article get() = Article(type = ArticleType.loading)
    }

    fun prepare(json: JSONObject) {
        when (type) {
            ArticleType.Type1 -> { // title
                id = json.getInt("id")
                title = json.getString("title")
            }
            ArticleType.Type2 -> { // title with description
                id = json.getInt("id")
                title = json.getString("title")
                desc = json.getString("short_desc")
            }
            ArticleType.Type3, ArticleType.Type12 -> { // title, description and banner
                id = json.getInt("id")
                title = json.getString("title")
                desc = json.getString("short_desc")
                bannerUrl = prepareURL(json.getString("image_url"))
            }
            ArticleType.Type4 -> { // title, description, author
                id = json.getInt("id")
                title = json.getString("title")
                desc = json.getString("short_desc")
                thumbnailUrl = prepareURL(json.getString("image_url"))
            }
            ArticleType.Type5 -> { // articles, horizontal
                similarArticles = json.objectArray("articles").mapNotNull {
                    val type = ArticleType.of(it.getInt("type"))
                    if (type != null) Article(type = type).apply { prepare(it) } else null
                }.toTypedArray()
            }
            ArticleType.Type6 -> { // tag
                id = json.getInt("id")
                title = json.getString("title")
            }
            ArticleType.Type7 -> { // category
                id = json.getInt("id")
                title = json.getString("title")
            }
            ArticleType.Type8 -> { // search, keyword
                keyword = json.getString("keyword")
                title = json.getString("title")
            }
            ArticleType.Type11 -> {
                id = json.getInt("id")
                title = json.getString("title")
                desc = json.getString("short_desc")
                // this is the type used for favorites as well
                json.getString("thumbnail_url").let { thumbnailUrl = if (it.startsWith(
                        "http")) it else prepareURL(it) }
            }
            ArticleType.Type13 -> {
                categoryImgUrls = arrayOf(json.getInt("id_1"), json.getInt("id_2"),
                    json.getInt("id_3"), json.getInt("id_4"))
            }
            ArticleType.Type14 -> {
                id = json.getInt("id")
                title = json.getString("title")
                bannerUrl = prepareURL(json.getString("image_url"))
            }
            ArticleType.Type15 -> {
                title = json.getString("title")
                bannerUrl = prepareURL(json.getString("image_url"))
                audioLength = json.getInt("audio_length")
                audioURL = json.getString("audio_url")
                audioFileId = json.getInt("id")
            }
            ArticleType.Type16 -> {
                title = json.getString("title")
                bannerUrl = prepareURL(json.getString("image_url"))
                youtubeId = json.getString("youtube_id")
            }
            ArticleType.Type100 -> {
                id = json.getInt("id")
                title = json.getString("title")
                desc = json.getString("short_desc")
                thumbnailUrl = prepareURL(json.getString("thumbnail_url"))
                totalViews = json.getInt("viewed")
                totalLikes = json.getInt("likes")
                totalDislikes = json.getInt("dislikes")
                author = getKeyValue(json.objectArray("content"), "author_name")
                publishedDate = json.getString("createdAt").date(
                    "yyyy-MM-dd").displayStringWithSlash
            }
            ArticleType.Type9, ArticleType.Type10 -> { } // do nothing for now
        }
    }

    private fun prepareURL(url: String): String = "${AC.Content2URL}/articles/ta/$url"

    private fun getKeyValue(content: Array<JSONObject>, key: String) = content.firstOrNull {
        it.has(key) }?.getString(key)

}
