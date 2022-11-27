package com.whiture.apps.tamil.thousand.nights.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.whiture.apps.tamil.thousand.nights.*
import com.whiture.apps.tamil.thousand.nights.models.Article
import com.whiture.apps.tamil.thousand.nights.views.ArticlesAdapter
import kotlinx.android.synthetic.main.fragment_articles_latest.*
import org.json.JSONObject

class ArticlesLatestFragment: Fragment() {

    private var clicked: ((Int?, String?, ArticleType) -> Unit)? = null

    companion object {
        fun newInstance(clicked: (Int?, String?, ArticleType) -> Unit) =
            ArticlesLatestFragment().apply { this.clicked = clicked }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? = inflater.inflate(
        R.layout.fragment_articles_latest, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { activity ->
            var isLoading = false
            var total = 0
            var articles = mutableListOf<Article>()

            val adapter = ArticlesAdapter(activity, articles.toTypedArray(), emptyArray()) {
                    id, search, type -> clicked?.invoke(id, search, type) }

            fun firstLoadCompleted() {
                // first page loaded, hence remove progress and display recycle view
                article_list_progress.visibility = View.INVISIBLE
                article_list_progress_text.visibility = View.INVISIBLE
                articles_home_list.visibility = View.VISIBLE
            }

            fun fetch() {
                isLoading = true
                activity.httpPostJSON("${AC.ServerURL}/articles/latest", JSONObject().apply {
                    put("paging", JSONObject().arguments(mapOf(
                        "offset" to articles.filter { it.type != ArticleType.loading }.size,
                        "limit" to 10))) }) { success, code, data, _ ->
                    isLoading = false
                    if (activity.isActive() && success && code == 200 && data != null) {
                        total = data.getInt("total")
                        runOnUiThread {
                            firstLoadCompleted()
                            // remove the last one for loading purpose
                            val position = articles.size
                            articles = articles.filter { it.type != ArticleType.loading }.toMutableList()
                            articles.addAll(Article.collection(data))
                            adapter.setData(articles.toTypedArray())
                            adapter.notifyItemInserted(position)
                        }
                    }
                }
            }

            val linearManager = LinearLayoutManager(activity)
            articles_home_list.apply {
                setHasFixedSize(true) // for performance improvement
                visibility = View.INVISIBLE
                layoutManager = linearManager
                this.adapter = adapter
            }

            // add scroll listener
            articles_home_list.addOnScrollListener(object: RecyclerView.OnScrollListener(){
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (articles.size < total) {
                        // 5 is just threshold to preload the data before user scrolls to the bottom
                        if (linearManager.findLastVisibleItemPosition() + 5 >= linearManager.itemCount) {
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
            fetch() // let us begin
        }
    }

}

