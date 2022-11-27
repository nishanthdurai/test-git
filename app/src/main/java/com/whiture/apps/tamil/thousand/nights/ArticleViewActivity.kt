package com.whiture.apps.tamil.thousand.nights

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isNotEmpty
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.whiture.apps.tamil.thousand.nights.models.Article
import com.whiture.apps.tamil.thousand.nights.models.ArticleAuthor
import com.whiture.apps.tamil.thousand.nights.models.ArticleTag
import com.whiture.apps.tamil.thousand.nights.models.RelatedArticle
import com.whiture.apps.tamil.thousand.nights.views.RelatedArticlesAdapter
import kotlinx.android.synthetic.main.activity_article_view.*
import kotlinx.android.synthetic.main.view_article_view_author_date.view.*
import kotlinx.android.synthetic.main.view_article_view_image.view.*
import kotlinx.android.synthetic.main.view_article_view_like.view.*
import kotlinx.android.synthetic.main.view_article_view_paragraph.view.*
import kotlinx.android.synthetic.main.view_article_view_quote.view.*
import kotlinx.android.synthetic.main.view_article_view_related.view.*
import kotlinx.android.synthetic.main.view_article_view_subtitle.view.*
import kotlinx.android.synthetic.main.view_article_view_title.view.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat

class ArticleViewActivity: AppCompatActivity() {

    private val app: App by lazy { application as App }
    private lateinit var article: JSONObject
    private var articleLoaded = false
    private var adShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_view)

        prepareAd() // prepare the ad

        val articleId = intent.getIntExtra("article_id", 44)

        // article opened
        httpPostJSON("${AC.ServerURL}/articles/opened",
            JSONObject().put("id", articleId)) { _, _, _, _ -> }

        // fetch the article
        showLoadingView(true)
        httpPostJSON("${AC.ServerURL}/articles/get", JSONObject().apply { put("id", articleId) }) {
                success, code, data, _ ->
            runOnUiThread {
                if (isActive()) {
                    if (success && code == 200 && data != null) {
                        article = data.getJSONObject("article")
                        articleLoaded = true
                        if (adShown && articleLoaded) displayArticle()
                    }
                    else {
                        showMessage("Sorry", "Please Check your internet connection" +
                                " and try again") { this.finish() }
                    }
                }
            }
        }
    }

    // method to prepare the ad
    private fun prepareAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, AdIdInterstitial, adRequest,
            object: InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adShown = true
                    if (adShown && articleLoaded) displayArticle()
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    adShown = true
                    interstitialAd.fullScreenContentCallback = object: FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent()
                            if (adShown && articleLoaded) displayArticle()
                        }

                        override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                            super.onAdFailedToShowFullScreenContent(p0)
                            if (adShown && articleLoaded) displayArticle()
                        }
                    }
                    interstitialAd.show(this@ArticleViewActivity)
                }
            })
    }

    // method to display the article
    private fun displayArticle() {
        showLoadingView(false)

        val content = article.objectArray("content")
        val fontSize = app.getArticleViewFontSize()

        fun setText(view: TextView, text: String) {
            view.text = text
            view.textSize = pixelsToSp(this, view.textSize) + fontSize
        }

        fun setCount(view: TextView, attr: String) {
            view.text = "${if (article.has(attr)) article.getInt(attr) else 0}"
        }

        // prepare the root layout
        if (article_view_root_layout.isNotEmpty()) article_view_root_layout.removeAllViews()

        // title
        content.firstOrNull { it.has("title") }?.let { json ->
            layoutInflater.inflate(R.layout.view_article_view_title,
                article_view_root_layout, false).let { view ->
                setText(view.view_article_view_title_txt, json.getString("title"))
                article_view_root_layout.addView(view)
            }
        }
        // banner image
        article.getString("banner_url").let { url ->
            layoutInflater.inflate(
                R.layout.view_article_view_image, article_view_root_layout,
                false).let { view ->
                showImage("${AC.Content2URL}/articles/ta/$url", view.view_article_view_image_img)
                view.view_article_view_img_desc_txt.visibility = View.GONE
                article_view_root_layout.addView(view)
            }
        }
        // author view and date view
        layoutInflater.inflate(
            R.layout.view_article_view_author_date, article_view_root_layout,
            false).let { view ->
            view.view_article_view_author_name_txt.let {
                fetchAuthor(article)?.let { author ->
                    showImage(author.profile, view.view_article_view_author_img)
                    setText(it, author.name)
                    view.view_author_layout.setOnClickListener { authorClicked(author) }
                }
            }
            setText(view.view_article_view_date, getPublishedDate(article) ?: "Today")
            article_view_root_layout.addView(view)
        }
        // content
        content.forEach { obj ->
            when {
                obj.has("sub_title") -> layoutInflater.inflate(
                    R.layout.view_article_view_subtitle,
                    article_view_root_layout, false).let { view ->
                    setText(view.view_article_view_subtitle_txt, obj.getString("sub_title"))
                    article_view_root_layout.addView(view)
                }
                obj.has("img_url") -> layoutInflater.inflate(
                    R.layout.view_article_view_image,
                    article_view_root_layout, false).let { view ->
                    showImage("${AC.Content2URL}/articles/ta/${obj.getString("img_url")}",
                        view.view_article_view_image_img)
                    view.view_article_view_img_desc_txt.let {
                        if (obj.getString("img_desc").isNotEmpty()) setText(it, obj.getString(
                            "img_desc")) else it.visibility = View.GONE
                    }
                    article_view_root_layout.addView(view)
                }
                obj.has("para") -> layoutInflater.inflate(
                    R.layout.view_article_view_paragraph,
                    article_view_root_layout, false).let { view ->
                    setText(view.view_article_view_paragraph_txt, obj.getString("para"))
                    article_view_root_layout.addView(view)
                }
                obj.has("quote") -> layoutInflater.inflate(
                    R.layout.view_article_view_quote,
                    article_view_root_layout, false).let { view ->
                    setText(view.view_article_view_quote_txt, "\"${obj.getString("quote")}\"")
                    article_view_root_layout.addView(view)
                }
            }
        }
        setupActionBar() // top action bar
        // tags
        val tagViews = arrayOf(
            R.id.view_article_tag1_txt, R.id.view_article_tag2_txt,
            R.id.view_article_tag3_txt)
        divideTags(fetchTags(article)).forEach { tags ->
            layoutInflater.inflate(
                R.layout.view_article_view_tag3, article_view_root_layout,
                false).let { view ->
                tags.forEachIndexed { i, tag -> view.findViewById<TextView>(tagViews[i]).apply {
                    visibility = View.VISIBLE
                    text = tag.name
                    setOnClickListener { tagClicked(tag) }
                } }
                article_view_root_layout.addView(view)
            }
        }
        // share, likes, dislikes and view count
        layoutInflater.inflate(
            R.layout.view_article_view_like, article_view_root_layout,
            false).let { view ->
            setCount(view.view_article_view_like_text, "likes")
            setCount(view.view_article_view_dislike_text, "dislikes")
            setCount(view.view_article_view_count_txt, "viewed")
            article_view_root_layout.addView(view)
            // like button, dislike button and share button
            var liked = app.getLikedArticles().any { it == article.getInt("id") }
            var disliked = app.getDislikedArticles().any { it == article.getInt("id") }
            view.view_article_view_like_btn.apply {
                if (liked) this.setImageResource(R.drawable.ic_article_like)
                this.setOnClickListener {
                    if (liked || disliked) {
                        showMessage("Sorry", "You have already done this!..") { }
                    }
                    else {
                        article.put("likes", article.getInt("likes") + 1)
                        setCount(view.view_article_view_like_text, "likes")
                        liked = true
                        this.setImageResource(R.drawable.ic_article_like)
                        app.saveLikedArticle(article.getInt("id"))
                        httpPostJSON("${AC.ServerURL}/articles/liked", JSONObject().apply {
                            put("id", article.getInt("id")) }) { _, _, _, _ -> }
                    }
                }
            }
            view.view_article_view_dislike_btn.apply {
                if (disliked) this.setImageResource(R.drawable.ic_article_dislike)
                this.setOnClickListener {
                    if (disliked || liked) {
                        showMessage("Sorry", "You have already done this!..") {}
                    }
                    else {
                        article.put("dislikes", article.getInt("dislikes") + 1)
                        disliked = true
                        setCount(view.view_article_view_dislike_text, "dislikes")
                        this.setImageResource(R.drawable.ic_article_dislike)
                        app.saveDislikedArticle(article.getInt("id"))
                        httpPostJSON("${AC.ServerURL}/articles/disliked", JSONObject().apply {
                            put("id", article.getInt("id")) }) { _, _, _, _ -> }
                    }
                }
            }
            view.view_article_view_share_img.setOnClickListener {
                shareText("${AC.ServerURL}/articles-share/view?id=${
                    article.getInt("id")}")
            }
        }
        // for related articles
        getRelatedArticles(article)?.let { articles ->
            layoutInflater.inflate(R.layout.view_article_view_related, article_view_root_layout,
                false).let { view ->
                view.view_article_view_related_rv.let { rv ->
                    rv.layoutManager = LinearLayoutManager(this,
                        LinearLayoutManager.HORIZONTAL, false)
                    rv.adapter = RelatedArticlesAdapter(this, articles) { art ->
                        articleClicked(art)
                    }
                    rv.addItemDecoration(ItemDecorator(applicationContext))
                    LinearSnapHelper().attachToRecyclerView(rv)
                }
                article_view_root_layout.addView(view)
            }
        }
    }

    class ItemDecorator(context: Context): RecyclerView.ItemDecoration() {
        private var mDivider: Drawable = context.obtainStyledAttributes(R.style.AppBaseTheme,
            intArrayOf(android.R.attr.divider)).getDrawable(0)!!

        override fun onDraw(c: Canvas, parent: RecyclerView) {
            val top = parent.paddingTop
            val bottom = parent.height - parent.paddingBottom
            val childCount = parent.childCount - 1

            repeat(childCount) { index ->
                val child = parent.getChildAt(index)
                val params = child.layoutParams as RecyclerView.LayoutParams
                val left = child.right + params.rightMargin
                val right = left + mDivider.intrinsicWidth
                mDivider.setBounds(left, top, right, bottom)
                mDivider.draw(c)
            }
        }
    }

    private fun getRelatedArticles(json: JSONObject): Array<RelatedArticle>? = json.objectArray(
        "content").firstOrNull { it.has("relevance") }?.let { it.objectArray(
        "relevance").map { json -> RelatedArticle.parse(json) } }?.toTypedArray()

    private fun getPublishedDate(json: JSONObject): String? = SimpleDateFormat("yyyy-MM-dd").parse(
        json.getString("createdAt"))?.displayStringWithSlash

    private fun fetchAuthor(json: JSONObject): ArticleAuthor? = json.objectArray("content").firstOrNull {
        it.has("author_name") }?.let { ArticleAuthor(id = json.getInt("author_id"),
        name = it.getString("author_name")) }

    private fun authorClicked(author: ArticleAuthor) {
        startActivity(Intent(this, ArticleListActivity::class.java).apply {
            putExtra("author_id", author.id)
        })
    }

    private fun articleClicked(article: RelatedArticle) {
        startActivity(Intent(this, ArticleViewActivity::class.java).apply {
            putExtra("article_id", article.id)
        })
    }

    private fun setupActionBar () {
        //to action bar customization
        this.setSupportActionBar(view_article_view_tool_bar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        this.supportActionBar?.setHomeAsUpIndicator(
            ContextCompat.getDrawable(this,
                androidx.appcompat.R.drawable.abc_ic_ab_back_material).also {
                it?.colorFilter = PorterDuffColorFilter(
                    ContextCompat.getColor(this,
                        R.color.colorBlack), PorterDuff.Mode.SRC_ATOP) })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun fetchTags(json: JSONObject): Array<ArticleTag> {
        val ids = json.intArray("tag_ids")
        json.objectArray("content").firstOrNull { it.has("tag_names") }?.let {
            val names = it.stringArray("tag_names")
            return names.mapIndexed { i, name -> ArticleTag(ids[i], name) }.toTypedArray()
        }
        return emptyArray()
    }

    private fun tagClicked(tag: ArticleTag) {
        startActivity(Intent(this, ArticleListActivity::class.java).apply {
            putExtra("category", tag.id)
            putExtra("type", ArticleType.Type6.value)
        })
    }

    private fun showLoadingView(isVisible: Boolean) {
        article_view_loading_layout.visibility = if (isVisible) View.VISIBLE else View.GONE
        article_view_layout.visibility = if(isVisible) View.GONE else View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.article_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val articles: Array<Article> = app.loadFavouriteArticles()?.let { data ->
            JSONArray(data).objectArray().map { Article.parse(it) } }?.toTypedArray() ?: emptyArray()
        val menuItem: MenuItem = menu.findItem(R.id.menu_article_view_favourites)
        menuItem.icon = if (articles.any { it.id == article.getInt("id") })
            ContextCompat.getDrawable(this, R.drawable.favorite_selected) else
            ContextCompat.getDrawable(this, R.drawable.favorite_default)
        return true
    }

    private fun setFavouriteIcon(item: MenuItem) {
        val articles: Array<Article> = app.loadFavouriteArticles()?.let { data ->
            JSONArray(data).objectArray().map { Article.parse(it) } }?.toTypedArray() ?: emptyArray()
        val favourite = Article.parse(article)
        favourite.desc = article.objectArray("content").firstOrNull {
            it.has("quote") }?.getString("quote") ?: ""
        val isLiked = articles.none { it.id == favourite.id }
        item.icon = ContextCompat.getDrawable(this,
            if (isLiked) R.drawable.favorite_selected else R.drawable.favorite_default)
        if (isLiked) { // article liked
            httpPostJSON("${AC.ServerURL}/articles/liked", JSONObject().put("id",
                favourite.id)) { _, _, _, _ -> }
            saveFavorites(arrayOf(favourite) + articles)
        }
        else {
            saveFavorites(articles.filter { it.id != favourite.id }.toTypedArray())
        }
    }

    private fun saveFavorites(articles: Array<Article>) {
        val array = JSONArray()
        articles.map { JSONObject().arguments(mapOf("type" to it.type.value,
            "id" to it.id!!, "title" to it.title!!, "short_desc" to it.desc!!,
            "thumbnail_url" to it.thumbnailUrl!!)) }.forEach { array.put(it) }
        app.saveFavoriteArticles(array.toString(2))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_article_view_favourites -> {
                setFavouriteIcon(item)
                return true
            }
            R.id.menu_article_view_text_size -> { // change text size
                var previousSizeAdd = app.getArticleViewFontSize()
                if (previousSizeAdd >= -4f && previousSizeAdd < 4f) {
                    previousSizeAdd += 2f
                }
                else if (previousSizeAdd == 4f) {
                    previousSizeAdd = -4f
                }
                app.saveArticleViewFontSize(previousSizeAdd)
                displayArticle()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}

