package com.whiture.apps.tamil.thousand.nights.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.whiture.apps.tamil.thousand.nights.*
import com.whiture.apps.tamil.thousand.nights.models.Article
import com.whiture.apps.tamil.thousand.nights.views.ArticlesAdapter
import kotlinx.android.synthetic.main.fragment_articles_home.*

class ArticlesHomeFragment: Fragment() {

    private var clicked: ((Int?, String?, ArticleType) -> Unit)? = null

    companion object {
        fun newInstance(clicked: (Int?, String?, ArticleType) -> Unit) =
            ArticlesHomeFragment().apply { this.clicked = clicked }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? = inflater.inflate(
        R.layout.fragment_articles_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { activity ->
            activity.httpGetJSON("${AC.ServerURL}/articles/home") { success, code, data, _ ->
                article_home_progress.visibility = View.GONE
                if (activity.isActive() && success && code == 200 && data != null) {
                    article_home_progress_text.visibility = View.GONE
                    val articles: Array<Article> = data.objectArray("home").map {
                        Article.parse(it) }.toTypedArray()
                    runOnUiThread {
                        articles_home_list.adapter = ArticlesAdapter(activity, articles, emptyArray()) {
                                id, search, type -> clicked?.invoke(id, search, type) }
                        articles_home_list.recycledViewPool.setMaxRecycledViews(ArticleType.Type15.value, 0)
                    }
                }
                else {
                    article_home_progress_text.text = "No internet, please try again.."
                }
            }
        }
    }

}

