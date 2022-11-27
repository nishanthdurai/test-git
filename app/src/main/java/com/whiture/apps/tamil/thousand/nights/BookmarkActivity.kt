package com.whiture.apps.tamil.thousand.nights

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.whiture.apps.tamil.thousand.nights.fragments.BookmarkAudioFragment
import com.whiture.apps.tamil.thousand.nights.fragments.BookmarkReaderFragment
import kotlinx.android.synthetic.main.activity_bookmark.*

class BookmarkActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark)

        //turns off night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // prepare the tool bar and set the back arrow color to accent color
        ContextCompat.getColor(this@BookmarkActivity, R.color.colorAccent).let { accentColor ->
            bookmarksToolbar.apply {
                title = "Bookmarks"
                setTitleTextColor(accentColor)
                navigationIcon = ContextCompat.getDrawable(this@BookmarkActivity,
                    R.drawable.btn_back_material)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        colorFilter = BlendModeColorFilter(accentColor, BlendMode.SRC_ATOP)
                    }
                    else {
                        setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP)
                    }
                }
            }
            setSupportActionBar(bookmarksToolbar)
        }

        // set the adapter for the view pager
        bookmarkViewPager.adapter = object: FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment = if(position == 0)
                BookmarkReaderFragment() else BookmarkAudioFragment()
        }
        TabLayoutMediator(bookmarkTabLayout, bookmarkViewPager) { tab, position ->
            tab.text = if (position == 0) "Reader Book" else "Audio Book"
        }.attach()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

}

