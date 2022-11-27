package com.whiture.apps.tamil.thousand.nights.views

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.whiture.apps.tamil.thousand.nights.R
import com.whiture.apps.tamil.thousand.nights.displayStringWithSlash
import com.whiture.apps.tamil.thousand.nights.models.RelatedArticle
import com.whiture.apps.tamil.thousand.nights.showImage
import kotlinx.android.synthetic.main.view_article_view_related_rv_child_with_img.view.*

class RelatedArticlesAdapter(val context: Activity, val articles: Array<RelatedArticle>,
                             val articleClicked:(RelatedArticle)->Unit)
    : RecyclerView.Adapter<RelatedArticlesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            if (viewType == 1) R.layout.view_article_view_related_rv_child_with_img
            else R.layout.view_article_view_related_rv_child_without_img, parent,
            false), articles, viewType)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(articles[position])
    }

    override fun getItemViewType(position: Int): Int = if (
        articles[position].thumbnailUrl.isNotEmpty()) 1 else 0

    override fun getItemCount(): Int = articles.size

    inner class ViewHolder(rootView: View, articles: Array<RelatedArticle>,
                           viewType: Int): RecyclerView.ViewHolder(rootView) {
        private var titleView: TextView? = null
        private var dopView: TextView? = null
        private var imgView: ImageView? = null

        init {
            titleView = rootView.view_article_view_related_title_txt
            dopView = rootView.view_article_view_related_date_txt
            if (viewType == 1) imgView = rootView.view_article_view_related_thumbnail_img

            rootView.setOnClickListener {
                rootView.alpha = 0.5f
                articleClicked(articles[adapterPosition])
            }
        }

        fun setData(article: RelatedArticle) {
            titleView?.text = article.title
            dopView?.text = article.publishDate.displayStringWithSlash
            imgView?.let { context.showImage(article.thumbnailUrl, it) }
        }

    }

}

