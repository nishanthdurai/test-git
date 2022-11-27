package com.whiture.apps.tamil.thousand.nights.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.whiture.apps.tamil.thousand.nights.*
import kotlinx.android.synthetic.main.fragment_articles_main.*

class ArticlesMainFragment: Fragment() {

    companion object {
        fun newInstance() = ArticlesMainFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_articles_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tabs = arrayOf("Home", "Trending", "New", "Favourites", "Categories", "Authors", "Tags")
        articlesHomeViewPager.adapter = object: FragmentStateAdapter(this) {
            override fun getItemCount(): Int = tabs.size

            override fun createFragment(position: Int): Fragment {
                return when(position) {
                    0 -> ArticlesHomeFragment.newInstance { id, search, type ->
                        when (type) {
                            // category, tag , search
                            ArticleType.Type6, ArticleType.Type7 -> id?.let { openArticleList(category = it) }
                            ArticleType.Type8 -> search?.let { openArticleList(search = it) }
                            // category Images 1, 2, 3, 4
                            ArticleType.Type13 -> id?.let { openArticleList(category = it) }
                            ArticleType.Type14 -> id?.let { openArticleList(category = it) }
                            ArticleType.Type16 -> search?.let { activity?.showYoutube(it) }
                            else -> id?.let { openArticle(it) }
                        }
                    }
                    1 -> ArticlesTrendingFragment.newInstance { id -> id?.let { openArticle(it) } }
                    2 -> ArticlesLatestFragment.newInstance { id, _, _ -> id?.let { openArticle(it) } }
                    3 -> ArticlesFavouritesFragment.newInstance { id -> id?.let { openArticle(it) } }
                    4 -> ArticlesCategoriesFragment.newInstance { category ->
                        openArticleList(category = category.id) }
                    5 -> ArticlesAuthorsFragment.newInstance { author ->
                        openArticleList(author = author.id) }
                    6 -> ArticlesTagsFragment.newInstance { tag -> openArticleList(tag = tag.id) }
                    else -> ArticlesFavouritesFragment.newInstance { }
                }
            }
        }
        TabLayoutMediator(articlesHomeTabLayout, articlesHomeViewPager) { tab, position ->
            tab.text = tabs[position]
        }.attach()
    }

    private fun openArticleList(category: Int? = null, author: Int? = null, tag: Int? = null,
                                search: String? = null) = startActivity(Intent(activity,
        ArticleListActivity::class.java).apply {
            category?.let { putExtra("category_id", it) }
            author?.let { putExtra("author_id", it) }
            tag?.let { putExtra("tag_id", it) }
            search?.let { putExtra("search", it) }
        })

    private fun openArticle(id: Int) = startActivity(Intent(activity,
        ArticleViewActivity::class.java).apply { putExtra("article_id", id) })

}

