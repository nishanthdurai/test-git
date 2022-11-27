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

// https://api.kadalpura.com/articles/trends
class ArticlesTrendingFragment: Fragment() {

    private var clicked: ((Int?)->Unit)? = null

    companion object {
        fun newInstance(clicked: (Int?)->Unit) = ArticlesTrendingFragment().apply {
            this.clicked = clicked
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? = inflater.inflate(
        R.layout.fragment_articles_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            it.httpGetJSON("${AC.ServerURL}/articles/trends") { success, code, data, _ ->
                runOnUiThread {
                    article_home_progress.visibility = View.GONE
                    if (it.isActive() && success && code == 200 && data != null) {
                        article_home_progress_text.visibility = View.GONE
                        articles_home_list.adapter = ArticlesAdapter(it, Article.collection(data),
                            emptyArray()) { id, _, _ -> clicked?.invoke(id) }
                    }
                    else {
                        article_home_progress_text.text = "No internet, please try again.."
                    }
                }
            }
        }
    }

}

