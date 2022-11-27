package com.whiture.apps.tamil.thousand.nights.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.whiture.apps.tamil.thousand.nights.App
import com.whiture.apps.tamil.thousand.nights.R
import com.whiture.apps.tamil.thousand.nights.models.Article
import com.whiture.apps.tamil.thousand.nights.views.ArticlesAdapter
import kotlinx.android.synthetic.main.fragment_articles_favourites.*

class ArticlesFavouritesFragment: Fragment() {

    private var clicked: ((Int?)->Unit)? = null

    companion object {
        fun newInstance(clicked: (Int?)->Unit) = ArticlesFavouritesFragment().apply {
            this.clicked = clicked
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? = inflater.inflate(
        R.layout.fragment_articles_favourites, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { activity ->
            val favs = Article.favorites((activity.application as App).loadFavouriteArticles() ?: "")
            articles_favourites_list.adapter = ArticlesAdapter(activity, favs, emptyArray()) { id, _, _ ->
                clicked?.invoke(id) }
            article_fav_progress.visibility = View.GONE
            if (favs.isEmpty()) {
                article_fav_progress_text.text = "No favorites found"
            }
            else {
                article_fav_progress_text.visibility = View.GONE
            }
        }
    }

}

