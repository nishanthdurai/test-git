package com.whiture.apps.tamil.thousand.nights

import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.whiture.apps.tamil.thousand.nights.dialogs.ExitDialog
import com.whiture.apps.tamil.thousand.nights.dialogs.HelpUsDialog
import com.whiture.apps.tamil.thousand.nights.dialogs.RateNowDialog
import com.whiture.apps.tamil.thousand.nights.fragments.AppListFragment
import com.whiture.apps.tamil.thousand.nights.fragments.ArticlesMainFragment
import com.whiture.apps.tamil.thousand.nights.fragments.BookListFragment
import com.whiture.apps.tamil.thousand.nights.models.BookData
import com.whiture.apps.tamil.thousand.nights.models.StoreData
import kotlinx.android.synthetic.main.activity_main.*

/**
 * the entry level activity for the reader application
 * it is responsible for,
 * 1. loading storev2.json from a. local assets b. server c. local storage
 * 2. displaying the categories and books
 * 3. displaying the articles
 * 4. displaying the app promotions
 * 5. notifications, ads, firebase and user preferences
 */
class MainActivity: AppCompatActivity() {

    private lateinit var app: App
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // load Admob ad request
        adBannerMain.loadAd(AdRequest.Builder().build())

        // turns off night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // book application
        app = application as App

        // mobile ads, firebase and notification
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        receiveDeviceFCMToken()

        // prepare toolbar
        toolbarMain.title = "முகப்பு"
        ContextCompat.getColor(this, R.color.colorAccent).let { color ->
            toolbarMain.setTitleTextColor(color)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                toolbarMain.overflowIcon?.colorFilter =
                    BlendModeColorFilter(color, BlendMode.SRC_ATOP)
            }
            else {
                toolbarMain.overflowIcon?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            }
        }
        setSupportActionBar(toolbarMain)

        if (!app.getLatestUpdateInformed()) {
            showMessage("வணக்கம் அன்பரே", "உங்களுக்கு மிகவும் பிடித்த இந்த செயலியை, " +
                    "நாங்கள் Java தொழில்நுட்பத்தின் அடுத்த தலைமுறையான Kotlin என்கிற தொழில்நுட்பத்தில் " +
                    "மாற்றியுள்ளோம்.\n\nஇதனால் தாங்கள் முன்பே Download செய்த புத்தகங்கள் மற்றும் தங்களது " +
                    "Bookmark ஆகியவகைகளை இழக்க நேரிடும்.\n\nதயவுகூர்ந்து இதை பொறுத்து மீண்டும் தங்களுக்கு " +
                    "பிடித்தமான புத்தகங்களை Download செய்யுமாறு வேண்டிக்கொள்கிறோம். நன்றி அன்பரே!..") {

            }
            app.setLatestUpdateInformed()
        }

        if (!handleNotifications()) {
            if (app.canRatingNowShown()) { // app-rating popup
                RateNowDialog(this).apply {
                    show()
                    setDialog(
                        rateLaterHandler = { app.resetTotalTimesPlayed() },
                        rateNowHandler = { openPlayStore(packageName)
                            app.setUserRated() },
                        notRatingHandler = { app.setUserDenied() })
                }
            }
        }

        // storev2.json comes from three places: assets, local folder and server
        var store: StoreData? = null
        loadJSONObjectFromAssets("storev2.json")?.let { json -> // assets
            store = StoreData.parse(json)
        }
        loadJSONObject("storev2.json")?.let {  json -> // local storage
            store = StoreData.parse(json)
        }

        // handle the back pressed event
        onBackPressedDispatcher.addCallback {
            ExitDialog(this@MainActivity).apply {
                show()
                setDialog {
                    this@MainActivity.finish()
                }
            }
        }

        store?.let { store ->
            prepareTabs(store)
            // as usual check the version and get it from server
            httpGetText("${AC.ContentURL}/books/store/tamil/book_storev2_version_tam.txt") {
                success, code, text ->
                if (success && code == 200 && text != null) {
                    text.toIntOrNull()?.let { version ->
                        // check if version is updated, if so, download and take the latest
                        if (version > store.version) {
                            saveHttpGetJSON("${AC.ContentURL}/books/store/tamil/storev2.json",
                                "", "storev2.json") { }
                        }
                    }
                }
            }
            return
        }

        // for 'store' object not found scenario
        showMessage("மன்னிக்கவும்", "தங்கள் மொபைலில் எங்களால் இந்த செயலியை இயக்க முடியவில்லை, " +
                "தயவு செய்து Play Store-ல் மீண்டும் பதிவிறக்கம் செய்து உபயோகிக்கவும்.") {
            openPlayStore(packageName)
            finish()
        }
    }

    private fun prepareTabs(data: StoreData) {
        val tabs = mutableListOf<String>()
        val shelfBooks = data.getBooks(app.getShelfBookIds())
        if (shelfBooks.isNotEmpty()) tabs.add("தங்கள் லைப்ரரி")
        tabs.add(data.categories[0].name)
        tabs.add("கட்டுரைகள்")
        tabs.add("Audio books")
        tabs.addAll(data.categories.filterIndexed { i, _ -> i != 0 }.map { it.name })
        tabs.add("எமது பிற செயலிகள்")

        val handler: (BookData, Boolean)->Unit = { book, read ->
            if (app.getShelfBookIds().contains(book.id)) {
                if (read) { // Read Book
                    startActivity(Intent(this@MainActivity,
                        BookReaderActivity::class.java).apply {
                        putExtra(IntentBookId, book.id)
                        putExtra("published", book.published)
                        // pass the last opened page detail
                        app.getLastOpenedDetail(book.id)?.let { bookmark ->
                            putExtra(IntentSectionId, bookmark.sectionId)
                            putExtra(IntentChapterId, bookmark.chapterId)
                            putExtra(IntentPageId, bookmark.pageId)
                        }
                    })
                }
                else { // Audio Book
                    startActivity(Intent(this@MainActivity,
                        AudioPlayerActivity::class.java).apply {
                        putExtra(IntentBookId, book.id)
                    })
                }
            }
            else {
                startActivity(Intent(this@MainActivity,
                    BookDetailActivity::class.java).apply {
                    putExtra("book", book)
                })
            }
        }

        mainViewPager.adapter = object: FragmentStateAdapter(this) {
            override fun getItemCount(): Int = tabs.size
            override fun createFragment(position: Int): Fragment {
                return if (shelfBooks.isEmpty()) {
                    when {
                        (position == 0) -> BookListFragment.newInstance(
                            data.getCategoryBooks(position), false, handler)
                        (position == 1) -> ArticlesMainFragment.newInstance()
                        (position == 2) -> BookListFragment.newInstance(
                            data.getAudioBooks(), false, handler)
                        (position == tabs.size - 1) -> AppListFragment.newInstance(
                            data.promotions) { app -> openPlayStore(app.id) }
                        else -> BookListFragment.newInstance(data.getCategoryBooks(
                            position - 2), false, handler)
                    }
                }
                else {
                    when {
                        (position == 0) -> BookListFragment.newInstance(shelfBooks, true, handler)
                        (position == 1) -> BookListFragment.newInstance(data.getCategoryBooks(0),
                            false, handler)
                        (position == 2) -> ArticlesMainFragment.newInstance()
                        (position == 3) -> BookListFragment.newInstance(data.getAudioBooks(),
                            false, handler)
                        (position == tabs.size - 1) -> AppListFragment.newInstance(
                            data.promotions) { app -> openPlayStore(app.id) }
                        else -> BookListFragment.newInstance(data.getCategoryBooks(position - 3),
                            false, handler)
                    }
                }
            }
        }

        TabLayoutMediator(mainTabLayout, mainViewPager) { tab, position ->
            tab.text = tabs[position] }.attach()
        mainTabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // 0 for scrolling the view pager, 1 for tapping on the tab
                if (tab?.position == 0 || tab?.position == 1) {
                    data.getBooks(app.getShelfBookIds()).let { books ->
                        if (books.isNotEmpty()) {
                            // f0 is the tag fixed for the first fragment
                            (supportFragmentManager.findFragmentByTag("f0") as?
                                    BookListFragment)?.let { fragment ->
                                if (fragment.isShelf) fragment.setBooks(books)
                            }
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // nothing to do here
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // nothing to do here
            }

        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_bar_menus, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> showSearchDialog { keyword -> startActivity(Intent(
                this, StoreSearchActivity::class.java).apply { putExtra(
                "keyword", keyword) }) }
            R.id.action_bookmark -> { startActivity(Intent(this,
                BookmarkActivity::class.java)) }
            R.id.action_last_heard -> { openLastHeardPage() }
            R.id.action_last_read -> {
                if (AC.bookId > 0) {
                    openLastReadPage(bookId = AC.bookId)
                }
                else {
                    showMessage(title = "No book opened",
                        message = "You have not opened any book yet on the app.") { }
                }
            }
            R.id.action_rate_app -> showRateNowDialog()
            // TODO: change the title here
            R.id.action_mail -> mailNow()
            R.id.action_facebook -> openFaceBook()
            R.id.action_more_apps -> openPlayStoreChannel()
            R.id.action_help -> HelpUsDialog(this).apply {
                show()
                setDialog(rateNowHandler = { showRateNowDialog() },
                    // TODO: change the title here
                    mailNowHandler = { mailNow() },
                    moreAppsHandler = { openPlayStoreChannel() })
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun receiveDeviceFCMToken() {
        val app = application as BookApplication
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.d("WHILOGS_DEVICE_TOKEN", "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }
                // get device token
                val token = task.result
                if (app.getFCMDeviceToken().isEmpty()) { // register it
                    Firebase.messaging.subscribeToTopic("TamilUtilsV1")
                    Firebase.messaging.subscribeToTopic("TamilArticlesV1")
                    Firebase.messaging.subscribeToTopic("TamilUtilAppsV1")
                    app.setFCMDeviceToken(token.toString())
                }
            })
    }

    /**
     * handle the incoming notification when the app is opened
     * NotificationService is available when the app is not opened, the service will open the
     * Main Activity with an intent carrying the below values
     */
    private fun handleNotifications(): Boolean {
        val bundle = intent.extras
        bundle?.getString("app_id")?.let { appId ->
            showPromoDialog(appTitle = bundle.getString("app_title"),
                appDescription = bundle.getString("app_description"),
                appImgUrl = bundle.getString("app_image_url")) {
                openPlayStore(appId.trim()) }
            return true
        }
        if (bundle?.containsKey("is_youtube") == true) {
            bundle.getString("id")?.let { showYoutube(it) }
            return true
        }
        bundle?.getString("html_link")?.let { link ->
            startActivity(Intent(this, HTMLActivity::class.java).apply {
                putExtra("notification_url", link) })
            return true
        }
        if (bundle?.containsKey("tamil_articles") == true) {
            bundle.getString("tag_id")?.let { it.toIntOrNull()?.let { id ->
                startActivity(Intent(this@MainActivity,
                    ArticleListActivity::class.java).apply { putExtra("tag_id", id) }) }
            }
            bundle.getString("category_id")?.let { it.toIntOrNull()?.let { id ->
                startActivity(Intent(this@MainActivity,
                    ArticleListActivity::class.java).apply { putExtra("category_id", id) }) }
            }
            bundle.getString("keyword")?.let { keyword ->
                startActivity(Intent(this@MainActivity,
                    ArticleListActivity::class.java).apply { putExtra("search", keyword) })
            }
            bundle.getString("article_id")?.let { it.toIntOrNull()?.let { id ->
                startActivity(Intent(this@MainActivity,
                    ArticleViewActivity::class.java).apply { putExtra("article_id", id) }) }
            }
            return true
        }
        return false
    }

}

