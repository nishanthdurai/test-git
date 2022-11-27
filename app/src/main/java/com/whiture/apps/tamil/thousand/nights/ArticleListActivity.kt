package com.whiture.apps.tamil.thousand.nights

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.whiture.apps.tamil.thousand.nights.models.Article
import com.whiture.apps.tamil.thousand.nights.views.ArticlesAdapter
import kotlinx.android.synthetic.main.activity_articles_list.*
import org.json.JSONArray
import org.json.JSONObject

class ArticleListActivity: AppCompatActivity() {

    private val app: App by lazy { application as App }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_articles_list)

        // load Admob ad request
        adBannerArticleList.loadAd(AdRequest.Builder().build())

        showLoadingView() // for initial fetching of results
        var isLoading = false // flag to know if the http request is under progress
        var total = 0 // total number of records
        val isFavourite = intent.hasExtra("favorites")
        val filter = JSONObject() // prepare filter
        if (intent.hasExtra("tag_id")) {
            filter.put("tag_id", JSONArray().put(
                intent.getIntExtra("tag_id", -1)))
        }
        if (intent.hasExtra("category_id")) {
            filter.put("category_id", JSONArray().put(
                intent.getIntExtra("category_id", -1)))
        }
        if (intent.hasExtra("author_id")) {
            filter.put("author_id", intent.getIntExtra("author_id", -1))
        }
        if (intent.hasExtra("search")) {
            filter.put("search", intent.getStringExtra("search"))
        }
        var articles: MutableList<Article> = mutableListOf()
        val adapter = ArticlesAdapter(this, articles.toTypedArray(), emptyArray()) {
                id, _, _ -> id?.let { clicked(it) } }

        fun fetch() {
            isLoading = true
            httpPostJSON("${AC.ServerURL}/articles/list", JSONObject().apply { put("filter",
                filter); put("paging", JSONObject().arguments(mapOf("offset" to
                    articles.filter { it.type != ArticleType.loading }.size,
                "limit" to 50 ))) }) { success, code, data, _ ->
                hideLoadingView()
                isLoading = false
                if (isActive() && success && code == 200 && data != null) {
                    total = data.getInt("total")
                    val position = articles.size
                    articles = articles.filter { it.type != ArticleType.loading }.toMutableList()
                    articles.addAll(Article.collection(data, ArticleType.Type11))
                    adapter.setData(articles.toTypedArray())
                    adapter.notifyItemInserted(position)
                    if (articles.isEmpty()) {
                        showMessage(title = "மன்னிக்கவும்",
                            message = "தங்கள் தேடலுக்கான கட்டுரைகள் எங்களிடம் தற்பொழுது இல்லை.") {
                            finish()
                        }
                    }
                }
            }
        }

        val linearManager = LinearLayoutManager(this)
        articlesHomeList.apply {
            setHasFixedSize(true) // for performance improvement
            visibility = View.INVISIBLE
            layoutManager = linearManager
            this.adapter = adapter
            if (!isFavourite) {
                addOnScrollListener(object: RecyclerView.OnScrollListener(){
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (articles.size < total) {
                            // 20 is just threshold to preload the data before user scrolls to the bottom
                            if (linearManager.findLastVisibleItemPosition() + 20 >= linearManager.itemCount) {
                                if (!isLoading) {
                                    isLoading = true
                                    articles.add(Article.loading)
                                    adapter.setData(articles.toTypedArray())
                                    adapter.notifyItemInserted(articles.size - 1)
                                    fetch()
                                }
                            }
                        }
                    }
                })
            }
        }

        if (isFavourite) {
            articlesListHeaderTxt.text = "தாங்கள் விரும்பியவை"
            articles.addAll(app.loadFavouriteArticles()?.let { data ->
                JSONArray(data).objectArray().map { Article.parse(it) } }?.toTypedArray() ?: emptyArray())
            hideLoadingView()
            if (articles.isNotEmpty()) {
                adapter.setData(articles.toTypedArray())
                adapter.notifyDataSetChanged()
            }
            else {
                showMessage("Sorry!", "No Favourites added") { this.finish() }
            }
        }
        else {
            articlesListHeaderTxt.text = "கட்டுரைகள்"
            fetch()
        }
    }

    private fun hideLoadingView() {
        articlesListLoadingLayout.visibility = View.GONE
        articlesHomeList.visibility = View.VISIBLE
    }

    private fun showLoadingView() {
        articlesListLoadingLayout.visibility = View.VISIBLE
        articlesHomeList.visibility = View.GONE
    }

    private fun clicked(id: Int) {
        startActivity(Intent(this, ArticleViewActivity::class.java).apply {
            putExtra("article_id", id)
        })
    }

}

