package com.whiture.apps.tamil.thousand.nights.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.whiture.apps.tamil.thousand.nights.*
import com.whiture.apps.tamil.thousand.nights.models.ArticleTag
import kotlinx.android.synthetic.main.fragment_articles_tags.*

class ArticlesTagsFragment: Fragment() {
    private var clicked: ((ArticleTag)->Unit)? = null

    companion object {
        fun newInstance(clicked: (ArticleTag)->Unit) = ArticlesTagsFragment().apply {
            this.clicked = clicked
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? = inflater.inflate(
        R.layout.fragment_articles_tags, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { activity ->
            activity.httpGetJSON("${AC.ServerURL}/articles/tags") { success, code, data, _ ->
                runOnUiThread {
                    if (activity.isActive() && success && code == 200 && data != null) {
                        article_tags_progress.visibility = View.GONE
                        article_tags_progress_text.visibility = View.GONE
                        activity.divideTags(data.objectArray("tags").map {
                            ArticleTag.parse(it) }.sortedBy { it.name }.toTypedArray()).forEach { tags ->
                            layoutInflater.inflate(R.layout.view_article_view_tag3,
                                article_view_root_layout, false).let { root ->
                                val views = arrayOf(R.id.view_article_tag1_txt, R.id.view_article_tag2_txt,
                                    R.id.view_article_tag3_txt).map { root.findViewById<TextView>(it) }
                                tags.forEachIndexed { i, tag -> views[i].apply {
                                    visibility = View.VISIBLE
                                    text = tag.name
                                    setOnClickListener { clicked?.invoke(tag) } } }
                                article_view_root_layout.addView(root)
                            }
                        }
                    }
                    else {
                        article_tags_progress.visibility = View.GONE
                        article_tags_progress_text.text = "No internet, please try again.."
                    }
                }
            }
        }
    }

}

