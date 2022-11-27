package com.whiture.apps.tamil.thousand.nights.views

import android.app.Activity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.whiture.apps.tamil.thousand.nights.AC
import com.whiture.apps.tamil.thousand.nights.ArticleType
import com.whiture.apps.tamil.thousand.nights.ArticleType.*
import com.whiture.apps.tamil.thousand.nights.R
import com.whiture.apps.tamil.thousand.nights.models.Article
import com.whiture.apps.tamil.thousand.nights.showImage

class ArticlesAdapter(val context: Activity, var articles: Array<Article>,
                      val viewedArticles: Array<Int>,
                      val clicked: (Int?, String?, ArticleType) -> Unit)
    : RecyclerView.Adapter<ArticlesAdapter.ViewHolder>() {

    fun setData(articles: Array<Article>) {
        this.articles = articles
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            1 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_home_type1,
                parent, false), viewType)
            2 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_home_type2,
                parent, false), viewType)
            3 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_home_type3,
                parent, false), viewType)
            4, 11 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_home_type4,
                parent, false), viewType)
            5 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_home_type5,
                parent, false), viewType)
            6, 7, 8, 9 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_home_type6,
                parent, false), viewType)
            10 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_listloading,
                parent, false), viewType)
            12 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_home_type12,
                parent, false), viewType)
            13 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_home_type13,
                parent, false), viewType)
            14 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_home_type14,
                parent, false), viewType)
            15 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_home_type15,
                parent, false), viewType)
            16 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_home_type16,
                parent, false), viewType)
            100 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_home_type100,
                parent, false), viewType)
            else -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.view_article_empty_view,
                parent, false), viewType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(articles[position])
    }

    override fun getItemCount(): Int = articles.size

    override fun getItemViewType(position: Int): Int = articles[position].type.value

    inner class ViewHolder(private val rootView: View, type: Int): RecyclerView.ViewHolder(rootView) {
        var layout: ConstraintLayout? = null
        private var titleView: TextView? = null
        private var descriptionView: TextView? = null
        private var bannerImageView: ImageView? = null
        private var thumbNailImageView: ImageView? = null
        private var recyclerView: RecyclerView? = null
        private var categoryImageView1: ImageView? = null
        private var categoryImageView2: ImageView? = null
        private var categoryImageView3: ImageView? = null
        private var categoryImageView4: ImageView? = null
        private var authorNameTxt: TextView? = null
        private var articleLikedCountTxt: TextView? = null
        private var articleDisLikedCountTxt: TextView? = null
        private var publishedDateTxt: TextView? = null
        private var articleLikedImg: ImageView? = null
        private var articleDislikedImg: ImageView? = null
        private var articleViewsCountTxt: TextView? = null
        // for media player
        private var playOrPauseButtonImageView: ImageView? = null

        init {
            when (type) {
                Type1.value -> {
                    titleView = rootView.findViewById(R.id.view_article_title)
                    layout = rootView.findViewById(R.id.view_article_layout)
                }
                Type2.value -> {
                    titleView = rootView.findViewById(R.id.view_article_title)
                    descriptionView = rootView.findViewById(R.id.view_article_description)
                    layout = rootView.findViewById(R.id.view_article_layout)
                }
                Type3.value -> {
                    titleView = rootView.findViewById(R.id.view_article_title)
                    descriptionView = rootView.findViewById(R.id.view_article_description)
                    bannerImageView = rootView.findViewById(R.id.view_article_banner_img)
                    layout = rootView.findViewById(R.id.view_article_layout)
                }
                Type4.value -> {
                    titleView = rootView.findViewById(R.id.view_article_title)
                    descriptionView = rootView.findViewById(R.id.view_article_description)
                    thumbNailImageView = rootView.findViewById(R.id.view_article_img)
                    layout = rootView.findViewById(R.id.view_article_layout)
                }
                Type5.value -> {
                    recyclerView = rootView.findViewById(R.id.view_type6_recyclerview)
                    LinearSnapHelper().attachToRecyclerView(recyclerView)
                    layout = rootView.findViewById(R.id.view_article_layout)
                }
                Type6.value, Type7.value,
                Type8.value, Type9.value -> {
                    titleView = rootView.findViewById(R.id.view_article_title)
                    layout = rootView.findViewById(R.id.view_article_layout)
                }
                Type11.value -> {
                    titleView = rootView.findViewById(R.id.view_article_title)
                    descriptionView = rootView.findViewById(R.id.view_article_description)
                    thumbNailImageView = rootView.findViewById(R.id.view_article_img)
                    layout = rootView.findViewById(R.id.view_article_layout)
                }
                Type100.value -> {
                    titleView = rootView.findViewById(R.id.view_article_title)
                    descriptionView = rootView.findViewById(R.id.view_article_description)
                    thumbNailImageView = rootView.findViewById(R.id.view_article_banner_img)
                    layout = rootView.findViewById(R.id.view_article_layout)
                    authorNameTxt = rootView.findViewById(R.id.view_article_view_author_name_txt)
                    publishedDateTxt = rootView.findViewById(R.id.view_article_view_date)
                    articleLikedImg = rootView.findViewById(R.id.view_article_view_like_btn)
                    articleDislikedImg = rootView.findViewById(R.id.view_article_view_dislike_btn)
                    articleLikedCountTxt = rootView.findViewById(R.id.view_article_view_like_text)
                    articleDisLikedCountTxt = rootView.findViewById(R.id.view_article_view_dislike_text)
                    articleViewsCountTxt = rootView.findViewById(R.id.view_article_view_count_txt)
                }
                Type12.value -> {
                    titleView = rootView.findViewById(R.id.view_article_title)
                    descriptionView = rootView.findViewById(R.id.view_article_description)
                    bannerImageView = rootView.findViewById(R.id.view_article_banner_img)
                    layout = rootView.findViewById(R.id.view_article_layout)
                }
                Type13.value -> {
                    categoryImageView1 = rootView.findViewById(R.id.view_article_category_img_1)
                    categoryImageView2 = rootView.findViewById(R.id.view_article_category_img_2)
                    categoryImageView3 = rootView.findViewById(R.id.view_article_category_img_3)
                    categoryImageView4 = rootView.findViewById(R.id.view_article_category_img_4)
                }
                Type14.value -> {
                    titleView = rootView.findViewById(R.id.view_article_title)
                    bannerImageView = rootView.findViewById(R.id.view_article_banner_img)
                }
                Type15.value -> {
                    titleView = rootView.findViewById(R.id.view_article_home_title)
                    bannerImageView = rootView.findViewById(R.id.view_article_home_banner)
                    playOrPauseButtonImageView = rootView.findViewById(R.id.article_home_media_player_play_button)
                }
                Type16.value -> {
                    titleView = rootView.findViewById(R.id.view_article_home_title)
                    bannerImageView = rootView.findViewById(R.id.view_article_home_banner)
                    playOrPauseButtonImageView = rootView.findViewById(R.id.article_home_media_player_play_button)
                }
            }
        }

        fun setData(article: Article) {
            when (article.type) {
                Type1 -> titleView?.text = article.title
                Type2 -> {
                    titleView?.text = article.title
                    descriptionView?.text = article.desc
                }
                Type3, Type12 -> {
                    titleView?.text = article.title
                    descriptionView?.text = article.desc
                    article.bannerUrl?.let { url -> bannerImageView?.let { img ->
                        context.showImage(url, img) } }
                }
                Type4, Type11 -> {
                    titleView?.text = article.title
                    descriptionView?.text = article.desc
                    article.thumbnailUrl?.let { url -> thumbNailImageView?.let { img ->
                        context.showImage(url, img) } }
                }
                Type5 -> {
                    val simArts = article.similarArticles
                    recyclerView?.layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    recyclerView?.adapter = object: RecyclerView.Adapter<SimilarArticlesViewHolder>() {
                        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
                                SimilarArticlesViewHolder {
                            return when (viewType) {
                                1 -> SimilarArticlesViewHolder(LayoutInflater.from(context).inflate(
                                    R.layout.view_similar_article_type1, parent, false),
                                    article.type)
                                2 -> SimilarArticlesViewHolder(LayoutInflater.from(context).inflate(
                                    R.layout.view_similar_article_type2, parent, false),
                                    article.type)
                                else -> SimilarArticlesViewHolder(LayoutInflater.from(context).inflate(
                                    R.layout.view_similar_article_type6, parent, false),
                                    article.type)
                            }
                        }

                        override fun onBindViewHolder(holder: SimilarArticlesViewHolder,
                                                      position: Int) { holder.setData(simArts[position]) }
                        override fun getItemCount(): Int = simArts.size
                        override fun getItemViewType(position: Int): Int = simArts[position].type.value
                    }
                    handleHorizontalListener(recyclerView!!)
                }
                Type6, Type7, Type8, Type9 -> {
                    titleView?.text = article.title
                }
                Type10 -> { } // do nothing for now
                Type100 -> {
                    titleView?.text = article.title
                    descriptionView?.text = article.desc
                    article.thumbnailUrl?.let { url -> thumbNailImageView?.let { img ->
                        context.showImage(url, img) } }
                    authorNameTxt?.text = article.author
                    publishedDateTxt?.text = article.publishedDate
                    articleLikedCountTxt?.text = "${article.totalLikes}"
                    articleDisLikedCountTxt?.text = "${article.totalDislikes}"
                    articleViewsCountTxt?.text = "${article.totalViews}"
                }
                Type13 -> {
                    if (article.categoryImgUrls.size == 4) {
                        arrayOf(categoryImageView1, categoryImageView2, categoryImageView3,
                            categoryImageView4).forEachIndexed { i, view -> showCategoryImage(
                            article.categoryImgUrls[i], view) } }
                }
                Type14 -> {
                    titleView?.text = article.title
                    article.bannerUrl?.let { url -> bannerImageView?.let { img ->
                        context.showImage(url, img) } }
                }
                Type15 -> {
                    titleView?.text = article.title
                    article.bannerUrl?.let { url -> bannerImageView?.let { img ->
                        context.showImage(url, img) } }
                    // TODO: handle media player
                }
                Type16 -> {
                    titleView?.text = article.title
                    article.bannerUrl?.let { url -> bannerImageView?.let { img ->
                        context.showImage(url, img) } }
                }
            }

            articles[adapterPosition].let { article ->
                if (article.type == Type13) {
                    if (article.categoryImgUrls.size == 4) {
                        arrayOf(categoryImageView1, categoryImageView2, categoryImageView3,
                            categoryImageView4).forEachIndexed { i, img -> img?.setOnClickListener {
                            clicked(article.categoryImgUrls[i], "", article.type) } }
                    }
                }
                else if (article.type == Type16) {
                    playOrPauseButtonImageView?.setOnClickListener { clicked(null, article.youtubeId,
                        article.type) }
                    rootView.setOnClickListener { clicked(null, article.youtubeId, article.type) }
                }
                else if (article.type != Type10) {
                    rootView.setOnClickListener {
                        if (viewedArticles.none { it == article.id }) rootView.alpha = 0.5f
                        if (article.type == Type8) clicked(-1, article.keyword, article.type)
                        else clicked(article.id, "", article.type)
                    }
                }
            }
        }

    }

    private fun showCategoryImage(url: Int, view: ImageView?) {
        view?.let { view -> context.showImage(
            "${AC.Content2URL}/articles/ta/CategoryImages/category_${url}.png", view) }
    }

    inner class SimilarArticlesViewHolder(private val rootView: View, type: ArticleType)
        : RecyclerView.ViewHolder(rootView) {
        private var titleView: TextView? = null
        private var descriptionView: TextView? = null

        init {
            when (type) {
                Type1 -> {
                    titleView = rootView.findViewById(R.id.view_article_title)
                }
                Type2 -> {
                    titleView = rootView.findViewById(R.id.view_article_title)
                    descriptionView = rootView.findViewById(R.id.view_article_description)
                }
                else -> {
                    titleView = rootView.findViewById(R.id.view_article_title)
                    descriptionView = rootView.findViewById(R.id.view_article_description)
                }
            }
        }

        fun setData(simArt: Article) {
            when (simArt.type) {
                Type1 -> titleView?.text = simArt.title
                Type2 -> {
                    titleView?.text = simArt.title
                    descriptionView?.text = simArt.desc
                }
                else -> titleView?.text = simArt.title
            }
            rootView.setOnClickListener {
                rootView.alpha = 0.5f
                if (simArt.type == Type8) clicked(-1, simArt.keyword, simArt.type) else
                    clicked(simArt.id, "", simArt.type)
            }
        }
    }

    private fun handleHorizontalListener(recyclerView: RecyclerView) {
        val listener = object: RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_MOVE)
                    rv.parent.requestDisallowInterceptTouchEvent(true)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        }
        recyclerView.addOnItemTouchListener(listener)
    }

}

