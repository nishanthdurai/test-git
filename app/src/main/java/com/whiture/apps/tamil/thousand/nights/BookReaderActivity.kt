package com.whiture.apps.tamil.thousand.nights

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.whiture.apps.tamil.thousand.nights.dialogs.HelpUsDialog
import com.whiture.apps.tamil.thousand.nights.dialogs.ReadSettingsDialog
import com.whiture.apps.tamil.thousand.nights.dialogs.TourDialog
import com.whiture.apps.tamil.thousand.nights.models.Bookmark
import com.whiture.apps.tamil.thousand.nights.models.ChapterVO
import com.whiture.apps.tamil.thousand.nights.models.MetaBook
import com.whiture.apps.tamil.thousand.nights.utils.RenderingEngine
import com.whiture.apps.tamil.thousand.nights.views.NavMenuAdapter
import com.whiture.apps.tamil.thousand.nights.views.PaperView
import kotlinx.android.synthetic.main.activity_book_reader.*
import kotlinx.android.synthetic.main.nav_menu_footer.*
import kotlinx.android.synthetic.main.nav_menu_header.*
import kotlin.math.abs

/**
 * the activity is responsible for showing the book content
 * 1. from page to page (with-in same chapter)
 * 2. from chapter to chapter (with-in same section / bml file)
 * 3. from section to section (different bml files)
 *
 * Page rendering involves,
 * 1. loading of the text content (from assets or local storage)
 * 2. parsing the content to BML tags
 * 3. process the tags to produce ChapterVO
 * 4. calculate the graphics commands so the rendering becomes faster
 *
 * It has to handle the display metrics,
 * 1. background color
 * 2. font sizes (title, para, header, page heading)
 * 3. font color
 * 4. orientation (portrait or landscape)
 * 5. screen brightness
 *
 * Touch handling, to ensure the next page or prev page
 * Side navigation menu drawer
 * Title bar - appearance and disappearance
 * Bookmark a page
 * Ads
 * Display Settings Dialog
 *
 */
class BookReaderActivity: AppCompatActivity() {

    private lateinit var app: App
    // the paint object is used to render texts and images for both the left and right paper views
    private lateinit var paint: Paint
    // the rendering engine is common for all the pages
    private lateinit var engine: RenderingEngine
    // current loaded book chapter
    private var chapter: ChapterVO? = null

    // the meta-data of the book - 1001.json, holds the section, chapter, title, audio etc
    private var bookMeta: MetaBook? = null

    // the current book, section, chapter and page shown to the user
    private var bookId: Int = -1 // with exact book id in the storev2.json file
    private var sectionId = 1 // starts from 1, reflects the actual section id mentioned in the bml file
    private var chapterId = 1 // starts from 1, reflects the actual chapter id mentioned in the bml file
    private var pageId = 0

    // current screen orientation & other details
    private var currentOrientation = false // isLandscape
    private var currentBackground = 0
    private var currentFontSize = 0
    private var currentFontFace = 0
    private var brightnessProgress = 0

    // for touch related
    private var scrollThreshold: Float = 0.0f
    private var touchThreshold = 0.0f
    private var mDownX = 1f

    // display metrics
    // actual screen size stores width and height
    private var actualScreenHeight = 0
    private var actualScreenWidth = 0

    // used for swapping orientations
    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f

    // font and space metrics
    private var physicalFontSize = 0f
    private var lineSpacing = 0f
    private var titleFontSize = 0f
    private var headerFontSize = 0f
    private var pageHeadingFontSize = 0f

    // flag to track if the menu bar is shown or not
    private var isMenuShown = false

    // settings dialog
    private var settingDialog: ReadSettingsDialog? = null

    // Navigation Drawer Menu related
    private var navMenuAdapter: NavMenuAdapter? = null
    private var lastExpandedPosition = -1

    private var isSetCurrentPageToLastPage = false

    // ads specific
    private var loadingPage: Boolean = true
    private var interstitialAdMob: InterstitialAd? = null

    // handler and runnable for timer - clocks at every 1 sec
    // used to update the delegate about the progress
    // used to stop the media player at a specified interval
    private val handler: Handler = Handler(Looper.getMainLooper())
    // runnable to give 5 mins gap between displaying ads
    private val adIntervalRunnable: Runnable = Runnable { prepareInterstitial() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_reader)

        app = application as App // app reference

        // hide system navigation - because we require a full screen
        hideSystemNavigationBar()

        // paint
        paint = Paint()
        paint.isAntiAlias = true

        // prepare the display metrics - screen size and swipe lengths
        // Here the notch display area is subtracted from actual screen size
        getWidthHeight().let { (w, h) ->
            actualScreenHeight = h - getNotchDisplayHeight()
            actualScreenWidth = w
        }
        setDisplayMetrics()

        // prepare the font metrics - font sizes, background color and screen orientation
        val pref = getSharedPreferences(AppPref, Context.MODE_PRIVATE) // preferences
        currentBackground = intent.getIntExtra(IntentBackgroundId, pref.getInt(PrefLastReadBackground,
            DefaultReadModeIndex))
        currentFontSize = intent.getIntExtra(IntentFontSize, pref.getInt(PrefLastReadFontSize,
            DefaultFontSizeIndex))
        currentFontFace = intent.getIntExtra(IntentFontFace, pref.getInt(PrefLastReadFontFace,
            DefaultFontFaceIndex))
        currentOrientation = intent.getBooleanExtra(IntentScreenOrientation, pref.getBoolean(
            PrefLastReadOrientation, DefaultScreenOrientation))
        setFontMetrics()
        setFontFace()

        // rendering engine initialization
        initialiseEngine()

        // set the reading mode - background, font color etc
        setReaderMode()

        // fetch book details from intent
        bookId = intent.getIntExtra(IntentBookId, -1)
        sectionId = intent.getIntExtra(IntentSectionId, 1)
        chapterId = intent.getIntExtra(IntentChapterId, 1)
        pageId = intent.getIntExtra(IntentPageId, 0)

        // handler for back button press
        onBackPressedDispatcher.addCallback {
            savePagePreferences()
            finish()
        }

        // prepare view animator to handle the touch and swipe events
        bookReaderPageAnimator.setOnTouchListener { _, event ->
            when ((event.action and MotionEvent.ACTION_MASK)) {
                MotionEvent.ACTION_DOWN -> {
                    mDownX = event.x
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handleTouchEvent(mDownX - event.x)
                }
                MotionEvent.ACTION_MOVE -> {
                    // Do nothing for now
                }
            }
            true
        }

        // prepare the side drawer menu
        drawerLayout.addDrawerListener(ActionBarDrawerToggle(this, drawerLayout,
            R.string.app_name, R.string.app_name))

        // load book meta
        Handler(Looper.getMainLooper()).post {
            bookMeta = MetaBook.parse(if (bookId == AC.bookId) {
                // load from the assets directory
                loadJSONObjectFromAssets("$bookId.json")
            }
            else {
                // load from the local directory
                loadJSONObject("$bookId/$bookId.json")
            })
            // prepare the navigation drawer on the left
            prepareNavDrawer()
            // prepare all the menus
            prepareMenus()
            // check for gesture help
            if (app.hasUserShownGestureHelp()) {
                showMenu(0, true) // will do auto hide
                // show the loading and try to load the interstitial ad
                showLoadingView()
                lastPageIdSet = true
                prepareInterstitial(true)
            }
            else {
                app.userHasShownGestureHelp()
                showMenu(0, false)
                TourDialog(this).apply {
                    show()
                    setDialog {
                        hideMenu(1) // hide the menu
                        // show the loading and try to load the interstitial ad
                        showLoadingView()
                        lastPageIdSet = true
                        prepareInterstitial(true)
                    }
                }
            }
        }
        // start the timer for reloading the ad
        startAdIntervalTimer()
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (interstitialAdMob != null) {
                    // if there is a capture ad, show it, on close, finish the activity
                    handler.removeCallbacks(adIntervalRunnable) // stop the timer
                    interstitialAdMob?.fullScreenContentCallback = object: FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            finish()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            finish()
                        }
                    }
                    interstitialAdMob?.show(this@BookReaderActivity)
                }
                else {
                    finish()
                }
            }
        })
    }

    // method to initialise the rendering engine
    private fun initialiseEngine() {
        // TODO: handle hasImagesDownloaded variable
        engine = RenderingEngine(paint, lineSpacing, titleFontSize, headerFontSize, physicalFontSize,
            pageHeadingFontSize, screenWidth * 0.94f, Color.BLACK, Color.GRAY,
            screenWidth * 0.02f, 0.0f, true)
        leftPage.engine = engine
        rightPage.engine = engine
    }

    // local variable to capture the incoming method parameter 'loadPage(pageIdSet: Boolean)'
    private var lastPageIdSet: Boolean = false

    // method to load the current page from the book BML file
    private fun loadPage(pageIdSet: Boolean = false) = runOnUiThread {
        lastPageIdSet = pageIdSet
        showLoadingView()
        if (interstitialAdMob != null) {
            // on close of interstitial ad, showPage will be shown
            interstitialAdMob?.show(this@BookReaderActivity)
        }
        else {
            showPage()
        }
    }

    private fun showPage() = runOnUiThread {
        Handler(Looper.getMainLooper()).post {
            getBookChapter(chapterId, if (bookId == AC.bookId) loadBMLAssets(sectionId)
            else loadBMLInternal(bookId, sectionId))?.let { tag ->
                // TODO: handle image folder path here
                chapter = processBookChapter(chapter = tag, paint = paint, contentWidth = screenWidth * 0.94f,
                    contentHeight = screenHeight * 0.96f, pageHeaderFontSize = pageHeadingFontSize,
                    titleFontSize = titleFontSize, headerFontSize = headerFontSize,
                    paraFontSize = physicalFontSize, lineSpace = lineSpacing,
                    rootPath = "${this.filesDir.absolutePath}/$bookId/")
                // hide the loading view and display the book content on the current page
                hideLoadingView()
                // set the page to the paper view
                chapter?.let { chapter ->
                    if (!lastPageIdSet) {
                        if (isSetCurrentPageToLastPage) {
                            pageId = chapter.pages.size - 1
                            isSetCurrentPageToLastPage = false
                        }
                        else {
                            pageId = 0
                        }
                    }
                    // set the current page to the paper view
                    setPage(leftPage)
                    if (bookReaderPageAnimator.displayedChild != 0) {
                        bookReaderPageAnimator.displayedChild = (bookReaderPageAnimator.displayedChild + 1) % 2
                    }
                    refreshViews()
                }
            }
        }
    }

    // method to invalidate the pages so that it will re-render its contents again
    private fun refreshViews() {
        leftPage.invalidate()
        rightPage.invalidate()
    }

    // method to set the current page to the given paper view
    private fun setPage(page: PaperView) {
        chapter?.let { page.setPage(it, pageId) }
        // save it to user preference, so next time, the same page will be opened
        savePagePreferences()
    }

    // method to move to the previous page - left click or swipe left to right
    private fun leftClicked() {
        if (isMenuShown) {
            hideMenu(0)
        }
        chapter?.let { chapter ->
            bookMeta?.let { bookMeta ->
                if (pageId > 0) {
                    pageId--
                    val pageIndex = (bookReaderPageAnimator.displayedChild + 1) % 2
                    setPage(bookReaderPageAnimator.getChildAt(pageIndex) as PaperView)
                    bookReaderPageAnimator.inAnimation = AnimationUtils.loadAnimation(this,
                        R.anim.push_right_in)
                    bookReaderPageAnimator.outAnimation = AnimationUtils.loadAnimation(this,
                        R.anim.push_right_out)
                    bookReaderPageAnimator.displayedChild = pageIndex
                }
                else { // prev chapter
                    when {
                        (sectionId == 1 && chapterId == 1) -> {
                            // beginning of the book
                            Toast.makeText(this, "You are at beginning of the book",
                                Toast.LENGTH_SHORT).show()
                        }
                        (chapterId == 1) -> {
                            // beginning of the section, go to previous section's last chapter
                            sectionId--
                            chapterId = bookMeta.sections[sectionId - 1].chapters.size
                            pageId = 0
                            isSetCurrentPageToLastPage = true
                            loadPage()
                            bookReaderNavDrawerList.collapseGroup(lastExpandedPosition)
                            bookReaderNavDrawerList.expandGroup(sectionId - 1)
                            highlightNavMenu()
                        }
                        else -> {
                            // beginning of the chapter, go to previous chapter
                            chapterId--
                            pageId = 0
                            isSetCurrentPageToLastPage = true
                            loadPage()
                            highlightNavMenu()
                        }
                    }
                }
            }
        }
    }

    // method to move to the next page - right side click or swipe right to left
    private fun rightClicked() {
        if (isMenuShown) {
            hideMenu(0)
        }
        chapter?.let { chapter ->
            bookMeta?.let { bookMeta ->
                if (pageId < chapter.pages.size - 1) {
                    pageId++
                    val pageIndex = (bookReaderPageAnimator.displayedChild + 1) % 2
                    setPage(bookReaderPageAnimator.getChildAt(pageIndex) as PaperView)
                    bookReaderPageAnimator.inAnimation = AnimationUtils.loadAnimation(this,
                        R.anim.push_left_in)
                    bookReaderPageAnimator.outAnimation = AnimationUtils.loadAnimation(this,
                        R.anim.push_left_out)
                    bookReaderPageAnimator.displayedChild = pageIndex
                }
                else { // next chapter
                    when {
                        (sectionId == bookMeta.sections.size
                                && chapterId == bookMeta.sections[sectionId-1].chapters.size) -> {
                            // end of the book
                            Toast.makeText(this, "You are at end of the book",
                                Toast.LENGTH_SHORT).show()
                        }
                        (chapterId < bookMeta.sections[sectionId-1].chapters.size) -> {
                            // end of the chapter, go to next
                            chapterId++
                            pageId = 0
                            loadPage()
                            bookReaderNavDrawerList.collapseGroup(lastExpandedPosition)
                            bookReaderNavDrawerList.expandGroup(sectionId - 1)
                            highlightNavMenu()
                        }
                        else -> {
                            // end of the section, go to next
                            sectionId++
                            chapterId = 1
                            pageId = 0
                            loadPage()
                            highlightNavMenu()
                        }
                    }
                }
            }
        }
    }

    // method to prepare the navigation drawer on the left side
    private fun prepareNavDrawer() {
        navMenuAdapter = NavMenuAdapter(this, bookMeta!!, sectionId, chapterId)
        bookReaderNavDrawerList.setAdapter(navMenuAdapter)
        bookReaderNavDrawerList.expandGroup(sectionId - 1)
        bookReaderNavDrawerList.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            sectionId = groupPosition + 1
            chapterId = childPosition + 1
            loadPage()
            drawerLayout.closeDrawers()
            hideMenu(0)
            navMenuAdapter?.sectionId = sectionId
            navMenuAdapter?.chapterId = chapterId
            navMenuAdapter?.notifyDataSetChanged()
            false
        }
        bookReaderNavDrawerList.setOnGroupExpandListener { groupPosition ->
            if (lastExpandedPosition != -1 && groupPosition != lastExpandedPosition) {
                bookReaderNavDrawerList.collapseGroup(lastExpandedPosition)
            }
            lastExpandedPosition = groupPosition
        }
        // set header & footer for the navigation drawer menu list
        bookReaderNavDrawerList.addHeaderView(layoutInflater.inflate(R.layout.nav_menu_header, null))
        bookReaderNavDrawerList.addFooterView(layoutInflater.inflate(R.layout.nav_menu_footer, null))
        // set book image, title and year it published
        showImage("${AC.ContentURL}/books/store/tamil/$bookId.png", bookReaderNavDrawerImg)
        bookReaderNavDrawerTitle.text = bookMeta?.title
        bookReaderNavDrawerYear.text = "${intent.getStringExtra("published") ?: "2022"}"
    }

    // method to prepare all the menus and its click events
    private fun prepareMenus() {
        // check if the book has audio or not
        if (bookMeta?.gotAudio() == false) {
            bookReaderMenuAudio.visibility = View.INVISIBLE
        }
        bookReaderMenuNavDrawer.setOnClickListener {
            if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                drawerLayout.closeDrawers()
            }
            else {
                drawerLayout.openDrawer(Gravity.LEFT)
            }
        }
        navMenuHelp.setOnClickListener {
            HelpUsDialog(this).apply {
                show()
                setDialog(rateNowHandler = { openPlayStore(packageName) },
                    mailNowHandler = { mailNow() },
                    moreAppsHandler = { openPlayStoreChannel() })
            }
        }
        navMenuReset.setOnClickListener {
            resetSettings()
        }
        navMenuSearch.setOnClickListener {
            if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                drawerLayout.closeDrawers()
            }
            Handler(Looper.getMainLooper()).postDelayed({
                showSearchDialog { keyword ->
                    startActivity(Intent(this, BookSearchActivity::class.java).apply {
                        putExtra(IntentBookId, bookId)
                        putExtra("keyword", keyword)
                    })
                }
            }, 200)
        }

        navMenuLastRead.setOnClickListener {
            // Nothing to do, just hide the drawer
            if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                drawerLayout.closeDrawers()
            }
        }
        navMenuBookmark.setOnClickListener { startActivity(Intent(this,
            BookmarkActivity::class.java)) }
        navMenuDownloadFonts.setOnClickListener {
            downloadAssets("${AppConstants.ContentURL}/books/aesthetics/fonts.zip",
                "fonts.zip", "", 143360)
        }
        navMenuDownloadThemes.setOnClickListener {
            downloadAssets("${AppConstants.ContentURL}/books/aesthetics/bg.zip",
                "bg.zip", "", 1730000)
        }
        navMenuHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        }
        navMenuRating.setOnClickListener { openPlayStore(packageName) }
        navMenuMail.setOnClickListener { mailNow() }
        navMenuFacebook.setOnClickListener { openFaceBook() }
        navMenuMoreApps.setOnClickListener { openPlayStoreChannel() }
        bookReaderMenuBookmark.setOnClickListener {
            hideMenu(0)
            // prepare the current page as bookmark and save it in shared preferences
            bookMeta?.let { meta ->
                val section = meta.sections[sectionId - 1]
                val sectionTitle = section.title
                val chapterTitle = section.chapters[chapterId - 1].title
                val content = chapter?.let { it.pages[pageId].content() } ?: ""
                app.addBookmark(Bookmark(bookId = bookId, sectionId = sectionId,
                    sectionTitle = sectionTitle, chapterId = chapterId, chapterTitle = chapterTitle,
                    pageId = pageId, backgroundId = currentBackground, fontSize = currentFontSize,
                    fontFace = currentFontFace, orientation = currentOrientation, content = content))
            }
            showMessage(title = "Success", message = "Your bookmark has been saved!..") { }
        }
        bookReaderMenuAudio.setOnClickListener {
            startActivity(Intent(this@BookReaderActivity,
                AudioPlayerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(IntentBookId, bookId)
                putExtra(IntentSectionId, sectionId)
                putExtra(IntentChapterId, chapterId)
            })
        }
        bookReaderMenuRotate.setOnClickListener {
            hideMenu(0)
            currentOrientation = !currentOrientation
            setDisplayMetrics()
            // initialise the engine, set the reader mode and reload page
            initialiseEngine()
            setReaderMode()
            loadPage()
        }
        bookReaderMenuTools.setOnClickListener {
            hideMenu(0)
            displayReadingToolsUI()
        }
    }

    // method to display the reading tools UI for changing font size, background, font face and brightness
    private fun displayReadingToolsUI() {
        fun reloadChapter() {
            pageId = 0
            initialiseEngine()
            setReaderMode()
            loadPage()
        }

        settingDialog = ReadSettingsDialog(this).apply {
            show()
            setDialog(currentFontSize, currentFontFace, currentBackground)
            setHandlers({ size ->
                currentFontSize = size
                setFontMetrics()
                reloadChapter()
            }, { face ->
                currentFontFace = face
                setFontFace()
                reloadChapter()
            }, { bg ->
                currentBackground = bg
                PageLayout.of(bg)?.let { layout -> setLayout(layout, true) }
            })
            if (brightnessProgress == 0) {
                brightnessProgress = Settings.System.getInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS, -1)
            }
            setBrightness(brightnessProgress, { progress ->
                brightnessProgress = progress
            }) {
                setBrightness()
            }
        }
    }

    // method to get permission for changing brightness
    private fun setBrightness() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this)) {
                window.attributes.screenBrightness = brightnessProgress / 255.0f
            }
            else {
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }
        }
    }

    // method to set the reader mode
    private fun setReaderMode() {
        when (currentBackground) {
            0 -> setLayout(PageLayout.Day, false)
            1 -> setLayout(PageLayout.Night, false)
            2 -> setLayout(PageLayout.Sepia, false)
            3 -> setLayout(PageLayout.DarkGrey, false)
            4 -> setLayout(PageLayout.LiteGrey, false)
            5 -> setLayout(PageLayout.Green, false)
            6 -> setLayout(PageLayout.ClassicPaper, false)
        }
    }

    // method to set the layout for content and loading view
    private fun setLayout(layout: PageLayout, refresh: Boolean) {
        // clear the graphic elements - so that the page will be updated with the latest bg
        chapter?.pages?.forEach { page ->
            page.graphicElements.clear()
        }
        when (layout) {
            PageLayout.Day -> {
                bookReaderRootLayout.setBackgroundColor(Color.WHITE)
                bookReaderLoadingLayout.setBackgroundColor(Color.WHITE)
                bookReaderLoadingText.setTextColor(Color.BLACK)
                engine.setFontColor(Color.BLACK, Color.GRAY)
            }
            PageLayout.Night -> {
                bookReaderRootLayout.setBackgroundColor(Color.BLACK)
                bookReaderLoadingLayout.setBackgroundColor(Color.BLACK)
                bookReaderLoadingText.setTextColor(Color.WHITE)
                engine.setFontColor(Color.WHITE, Color.GRAY)
            }
            PageLayout.Sepia -> {
                bookReaderRootLayout.setBackgroundColor(Color.argb(255, 233, 216, 188))
                bookReaderLoadingLayout.setBackgroundColor(Color.argb(255, 233, 216, 188))
                bookReaderLoadingText.setTextColor(Color.argb(255, 89, 65, 43))
                engine.setFontColor(Color.argb(255, 89, 65, 43), Color.BLACK)
            }
            PageLayout.DarkGrey -> {
                if (!app.hasLocalAssetFile(filesDir.absolutePath, "bg_dark_grey.png")) {
                    settingDialog?.dismiss()
                    downloadAssets("${AC.ContentURL}/books/aesthetics/bg_dark_grey.zip",
                        "bg_dark_grey.zip", "", 405504, failHandler = {
                            currentBackground = PageLayout.Day.value
                            setLayout(PageLayout.Day, true)
                        }) {
                        setLayout(PageLayout.DarkGrey, true)
                    }
                }
                else {
                    bookReaderRootLayout.background = getBgDrawable("bg_dark_grey.png")
                    bookReaderLoadingLayout.background = getBgDrawable("bg_dark_grey.png")
                }
                bookReaderLoadingText.setTextColor(Color.argb(255, 113, 110, 106))
                engine.setFontColor(Color.argb(255, 149, 147, 143),
                    Color.argb(255, 113, 110, 106))
            }
            PageLayout.LiteGrey -> {
                if (!app.hasLocalAssetFile(filesDir.absolutePath, "bg_lite_grey.png")) {
                    settingDialog?.dismiss()
                    downloadAssets("${AC.ContentURL}/books/aesthetics/bg_lite_grey.zip",
                        "bg_lite_grey.zip", "", 567296, failHandler = {
                            currentBackground = PageLayout.Day.value
                            setLayout(PageLayout.Day, true)
                        }) {
                        setLayout(PageLayout.LiteGrey, true)
                    }
                }
                else {
                    bookReaderRootLayout.background = getBgDrawable("bg_lite_grey.png")
                    bookReaderLoadingLayout.background = getBgDrawable("bg_lite_grey.png")
                }
                bookReaderLoadingText.setTextColor(Color.argb(255, 119, 113, 105))
                engine.setFontColor(Color.argb(255, 53, 47, 39),
                    Color.argb(255, 119, 113, 105))
            }
            PageLayout.Green -> {
                if (!app.hasLocalAssetFile(filesDir.absolutePath, "bg_green.png")) {
                    settingDialog?.dismiss()
                    downloadAssets("${AC.ContentURL}/books/aesthetics/bg_green.zip",
                        "bg_green.zip", "", 332800, failHandler = {
                            currentBackground = PageLayout.Day.value
                            setLayout(PageLayout.Day, true)
                        }) {
                        setLayout(PageLayout.Green, true)
                    }
                }
                else {
                    bookReaderRootLayout.background = getBgDrawable("bg_green.png")
                    bookReaderLoadingLayout.background = getBgDrawable("bg_green.png")
                }
                bookReaderLoadingText.setTextColor(Color.argb(255, 122, 135, 122))
                engine.setFontColor(Color.argb(255, 44, 42, 44),
                    Color.argb(255, 122, 135, 122))
            }
            PageLayout.ClassicPaper -> {
                if (!app.hasLocalAssetFile(filesDir.absolutePath, "bg_sepia.png")) {
                    settingDialog?.dismiss()
                    downloadAssets("${AC.ContentURL}/books/aesthetics/bg_sepia.zip",
                        "bg_sepia.zip", "", 419840, failHandler = {
                            currentBackground = PageLayout.Day.value
                            setLayout(PageLayout.Day, true)
                        }) {
                        setLayout(PageLayout.ClassicPaper, true)
                    }
                }
                else {
                    bookReaderRootLayout.background = getBgDrawable("bg_sepia.png")
                    bookReaderLoadingLayout.background = getBgDrawable("bg_sepia.png")
                }
                bookReaderLoadingText.setTextColor(Color.argb(255, 69, 55, 50))
                engine.setFontColor(Color.argb(255, 69, 55, 50),
                    Color.argb(255, 118, 108, 93))
            }
        }
        if (refresh) refreshViews()
    }

    // highlighting current section & chapter in the navigation drawer menu list
    private fun highlightNavMenu() {
        navMenuAdapter?.sectionId = sectionId
        navMenuAdapter?.chapterId = chapterId
        navMenuAdapter?.notifyDataSetChanged()
    }

    // handling of touch event on the page
    private fun handleTouchEvent(diffInXSwipe: Float) {
        when {
            abs(diffInXSwipe) > scrollThreshold -> { // for page
                if (diffInXSwipe > 0) rightClicked() else leftClicked()
            }
            abs(diffInXSwipe) < touchThreshold -> { // for menu
                if (!drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                    if (isMenuShown) hideMenu(0) else showMenu(0, false)
                }
            }
            else -> { // for page
                if (mDownX <= (bookReaderPageAnimator.width * 0.5f)) leftClicked() else rightClicked()
            }
        }
    }

    // show the loading view and hide the content
    private fun showLoadingView() = runOnUiThread {
        bookReaderPageAnimator.visibility = View.GONE
        bookReaderLoadingLayout.visibility = View.VISIBLE
        loadingPage = true
    }

    // hide the loading view and display the content
    private fun hideLoadingView() = runOnUiThread {
        bookReaderPageAnimator.visibility = View.VISIBLE
        bookReaderLoadingLayout.visibility = View.GONE
        loadingPage = false
    }

    // method to display the menu bar
    private fun showMenu(delay: Long, autoHide: Boolean) {
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars())
        }
        else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        ObjectAnimator.ofFloat(bookReaderMenuBar, "translationY", 0f).apply {
            startDelay = delay
            duration = 250
            start()
            addListener(object: Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {
                    // Nothing to do here
                }

                override fun onAnimationEnd(p0: Animator) {
                    if (autoHide) {
                        hideMenu(750) // hide it after 500 milli secs, 250 milli secs for showing
                    }
                }

                override fun onAnimationCancel(p0: Animator) {
                    // Nothing to do here
                }

                override fun onAnimationRepeat(p0: Animator) {
                    // Nothing to do here
                }
            })
        }
        isMenuShown = true
    }

    // method to hide the menu bar
    private fun hideMenu(delay: Long) {
        ObjectAnimator.ofFloat(bookReaderMenuBar, "translationY",
            -bookReaderMenuBar.height.toFloat()).apply {
            startDelay = delay
            duration = 250
            start()
        }
        isMenuShown = false
        hideSystemNavigationBar()
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
    }

    // method to reset the app settings
    private fun resetSettings() = runOnUiThread {
        askUser("Reset Page Settings", "Do you want to reset the settings to default?", AppButton("Yes") {
            app.resetPageSettings()
            runOnUiThread {
                showMessage(title = "Success",
                    message = "Settings have been successfully reset to default.") {
                    reloadActivity()
                }
            }
        }, AppButton("No") { })
    }

    // reload the activity with the default settings from the shared preferences
    private fun reloadActivity() = runOnUiThread {
        openLastReadPage(bookId, sectionId, chapterId, pageId)
    }

    // set the display metrics, screen width & height, threshold etc
    private fun setDisplayMetrics() {
        if (currentOrientation) { // if it is landscape, swap the screen dimensions
            screenHeight = actualScreenWidth.toFloat()
            screenWidth = actualScreenHeight.toFloat()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        else {
            screenHeight = actualScreenHeight.toFloat()
            screenWidth = actualScreenWidth.toFloat()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        // setting threshold for swipe and touch controls (while reading)
        scrollThreshold = screenWidth * 0.17f
        touchThreshold = screenWidth * 0.04f
    }

    // method to set the font metrics for default
    private fun setFontMetrics() {
        physicalFontSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            when(currentFontSize) {
                0 -> resources.getString(R.string.font_size_vsmall).toFloat()
                1 -> resources.getString(R.string.font_size_small).toFloat()
                2 -> resources.getString(R.string.font_size_medium).toFloat()
                3 -> resources.getString(R.string.font_size_large).toFloat()
                4 -> resources.getString(R.string.font_size_xlarge).toFloat()
                else -> resources.getString(R.string.font_size_medium).toFloat()
            }, resources.displayMetrics)
        lineSpacing = physicalFontSize * 0.66f
        titleFontSize = physicalFontSize * 1.5f
        headerFontSize = physicalFontSize * 1.25f
        pageHeadingFontSize = physicalFontSize * 0.75f
    }

    // method to set the font face
    private fun setFontFace() {
        paint.typeface = if (currentFontFace > 0) Typeface.createFromAsset(assets,
            resources.getStringArray(R.array.font_names)[currentFontFace - 1]) else Typeface.DEFAULT
    }

    // method to save current preferences
    private fun savePagePreferences() {
        app.bookLastRead(bookId, sectionId, chapterId, pageId, currentFontSize,
            currentFontFace, currentBackground, currentOrientation)
        // save it for the shelf book, when a book is opened from the shelf,
        // the last read page will be opened for that particular book
        app.setLastOpenedDetail(Bookmark(bookId = bookId, sectionId = sectionId, chapterId = chapterId,
            pageId = pageId))
    }

    private fun prepareInterstitial(isFirstTime: Boolean = false) {
        InterstitialAd.load(this, AdIdInterstitial, AdRequest.Builder().build(),
            object: InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                log(adError.message)
                startAdIntervalTimer()
                if (isFirstTime) {
                    showPage()
                }
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                // capture and hold the ad, when the page load happens, it will be shown
                interstitialAdMob = interstitialAd
                interstitialAdMob?.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        showPage()
                        startAdIntervalTimer()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        showPage()
                        startAdIntervalTimer()
                    }
                }
                // for the first time, show the ad immediately
                if (isFirstTime) {
                    interstitialAd.show(this@BookReaderActivity)
                }
            }
        })
    }

    // initialise the timer for 5 mins to set showAd flag to true
    private fun startAdIntervalTimer() = runOnUiThread {
        interstitialAdMob = null
        handler.apply {
            removeCallbacks(adIntervalRunnable)
            postDelayed(adIntervalRunnable, 5 * 60 * 1000) // 5 mins
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(adIntervalRunnable) // stop the timer
    }

}

