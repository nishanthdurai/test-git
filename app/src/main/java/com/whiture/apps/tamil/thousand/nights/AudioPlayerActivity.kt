package com.whiture.apps.tamil.thousand.nights

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.whiture.apps.tamil.thousand.nights.models.AlbumData
import com.whiture.apps.tamil.thousand.nights.models.Audiomark
import com.whiture.apps.tamil.thousand.nights.models.MetaBook
import kotlinx.android.synthetic.main.activity_audio_player.*
import java.net.URL

class AudioPlayerActivity: AppCompatActivity() {

    private lateinit var app: App
    private var service: AudioService? = null

    // current book and current playing album
    private lateinit var book: MetaBook
    private var album: AlbumData? = null
    private var albumIndex: Int = -1
    private var duration: Long = -1

    private val startPlaylist = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val index = it.data?.getIntExtra(IntentAlbumId, -1) ?: -1
            if (index > -1) {
                service?.mJumpTo(index)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)

        app = application as App // app reference

        val bookId = intent.getIntExtra(IntentBookId, AC.bookId)
        var albumId = intent.getIntExtra(IntentAlbumId, 0)
        val sectionId = intent.getIntExtra(IntentSectionId, 0)
        val chapterId = intent.getIntExtra(IntentChapterId, 0)
        var duration = intent.getIntExtra(IntentAudioDuration, 0)
        if (sectionId == 0 && chapterId == 0
            && albumId == 0 && duration == 0) {
            // fetch it from the last heard album details if any
            app.getLastHeardDetail(bookId)?.let {
                albumId = it.albumIndex
                duration = it.duration
            }
        }

        book = MetaBook.parse(if (bookId == AC.bookId) loadJSONObjectFromAssets("$bookId.json") else loadJSONObject(
            "$bookId/$bookId.json"))
        showImage("${AC.AudioBookUrl}/${book.id}/images/banner_${book.id}.png", coverArtImg)
        ContextCompat.getColor(this@AudioPlayerActivity, R.color.colorAccent).let { accentColor ->
            audioViewToolbar.apply {
                this.title = book.title
                setTitleTextColor(accentColor)
                navigationIcon = ContextCompat.getDrawable(this@AudioPlayerActivity,
                    R.drawable.btn_back_material)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        colorFilter = BlendModeColorFilter(accentColor, BlendMode.SRC_ATOP)
                    }
                    else {
                        setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP)
                    }
                }
            }
        }
        setSupportActionBar(audioViewToolbar)

        fun startAudio() {
            // initialise the streaming of audio
            mpLoadingLayout.visibility = View.GONE
            initAudio(book, albumId, duration, sectionId, chapterId)
        }

        InterstitialAd.load(this, AdIdInterstitial, AdRequest.Builder().build(),
            object: InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    log(adError.message)
                    startAudio()
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAd.fullScreenContentCallback = object: FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            startAudio()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            startAudio()
                        }
                    }
                    interstitialAd.show(this@AudioPlayerActivity)
                }
            })

        mpSectionsImg.clickAnimation(this) {
            startPlaylist.launch(Intent(this,
                AudioPlayListActivity::class.java).apply {
                putExtra(IntentBookId, bookId)
                putExtra(IntentAlbumId, service?.currentAlbum() ?: 0)
            })
            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up)
        }

        // close the service
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        handleBackPress()
        return true
    }

    private fun handleBackPress() {
        askUser("வெளியேற்றம்?", "இந்த பக்கத்தில் இருந்து தாங்கள் வெளியேற விரும்புகிறீர்களா? " +
                "இதனால் தாங்கள் கேட்கும் கதையின் ஒலிபரப்பு நிறுத்தப்படும்.",
            AppButton("ஆம்") {
                service?.stopSelf()
                finish()
        }, AppButton("இல்லை") { })
    }

    private fun initAudio(book: MetaBook, albumId: Int, duration: Int,
                          sectionId: Int, chapterId: Int) = runOnUiThread {
        val albums: Array<AlbumData> = book.getPlaylist()
        var albumIndex = albumId
        if (sectionId != 0 && chapterId != 0) {
            albums.firstOrNull { it.sectionId == sectionId && it.chapterId == chapterId }?.let {
                albumIndex = albums.indexOf(it)
            }
        }
        // prepare the audio service and run it
        val intent = Intent(this, AudioService::class.java)
        // we will first bind the service to our activity and get access to the service instance here
        bindService(intent, object: ServiceConnection {
            override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
                service = (binder as? AudioService.MediaBinder)?.getService()
                var isUserTrackingSeekBar = false // to know if the user is currently tracking the seek bar
                // sometimes, users might close the activity and go back to the main activity
                // while the audio will be running in the background
                // now, if the user comes back again, we will capture all the details and setup the activity
                service?.stream(playlist = albums, albumIndex = albumIndex, startTime = duration.toLong(),
                    listener = object: IAudioListener {
                        override fun bufferingStarted(data: AlbumData) {
                            this@AudioPlayerActivity.album = data
                            this@AudioPlayerActivity.albumIndex = albums.indexOf(album)
                            mpAlbumTitleTxt.text = data.desc
                            mpTotalTimeTxt.text = timeLabel(data.length.toInt())
                            mpTotalTimeLeftTxt.text = timeInHoursMins(book.getTotalDuration(
                                albums.indexOf(data)))
                            mpCurrentTimeTxt.text = timeLabel(0)
                            isUserTrackingSeekBar = false
                            mpPlayPauseImg.setImageResource(R.drawable.buffering)
                            mpSeekBar.max = data.length.toInt()
                            mpSeekBar.progress = 0
                            changeMediaControl(View.INVISIBLE)
                        }

                        override fun playStarted(data: AlbumData) {
                            mpPlayPauseImg.setImageResource(R.drawable.ic_audio_pause)
                            changeMediaControl(View.VISIBLE)
                            mpAlbumTitleTxt.text = data.desc
                        }

                        override fun playPaused(data: AlbumData) {
                            mpPlayPauseImg.setImageResource(R.drawable.ic_audio_play)
                        }

                        override fun playing(time: Long) {
                            if (!isUserTrackingSeekBar) {
                                mpSeekBar.progress = time.toInt()
                                mpCurrentTimeTxt.text = timeLabel(time.toInt())
                                this@AudioPlayerActivity.duration = time
                                // for every 5 secs, save it
                                if ((mpSeekBar.progress / 1000) % 5 == 4) {
                                    prepareAudiomark()?.let {
                                        app.setLastHeardDetail(it)
                                        app.bookLastHeard(it)
                                    }
                                }
                            }
                        }

                        override fun playCompleted(data: AlbumData) {
                            // do nothing for now, the service will handle it
                        }

                        override fun error(data: AlbumData) {
                            // inform the user and exit the activity
                            service?.mStop()
                            runOnUiThread {
                                informUser("Network Problem",
                                    "Please try again with proper network.", AppButton("OK") {
                                    this@AudioPlayerActivity.finish()
                                })
                            }
                        }
                    })

                // now try to load the notification icon
                Thread {
                    val bitmap = BitmapFactory.decodeStream(URL(
                        "${AC.AudioBookUrl}/${book.id}/images/fav_${book.id}.png").openStream())
                    // as soon as loaded, update the service
                    runOnUiThread { service?.iconLoaded(bitmap) }
                }.start()

                // set listener for each buttons and the seekbar
                mpPlayPauseImg.liteClickAnimation(this@AudioPlayerActivity) { service?.mPlayPause() }
                mpSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, p1: Int, p2: Boolean) {
                        sb?.progress?.let { mpCurrentTimeTxt.text = timeLabel(it) }
                    }

                    override fun onStartTrackingTouch(sb: SeekBar?) {
                        isUserTrackingSeekBar = true
                    }

                    override fun onStopTrackingTouch(sb: SeekBar?) {
                        isUserTrackingSeekBar = false
                        sb?.progress?.let {
                            service?.mSeekTo(it.toLong())
                            mpCurrentTimeTxt.text = timeLabel(it)
                        }
                    }
                })

                fun updateTarget(target: Int?) {
                    target?.let {
                        if (it > 0) {
                            mpSeekBar.progress = it
                            mpCurrentTimeTxt.text = timeLabel(it)
                        }
                    }
                }

                mp30sForwardImg.liteClickAnimation(this@AudioPlayerActivity) {
                    updateTarget(service?.mForward(30)) }
                mp30sBackImg.liteClickAnimation(this@AudioPlayerActivity) {
                    updateTarget(service?.mBackward(30)) }
                mpNextTrackImg.liteClickAnimation(this@AudioPlayerActivity) {
                    service?.mNext() }
                mpPrevTrackImg.liteClickAnimation(this@AudioPlayerActivity) {
                    service?.mPrev() }
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                service = null
            }
        }, Context.BIND_AUTO_CREATE)

        // we also start the service to make it run even if the activity is closed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        }
        else {
            startService(intent)
        }
    }

    // this will change the visibility of the media controls except the play / pause button
    private fun changeMediaControl(visibility: Int) {
        mp30sBackImg.visibility = visibility
        mp30sForwardImg.visibility = visibility
        mpNextTrackImg.visibility = visibility
        mpPrevTrackImg.visibility = visibility
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.audio_book_menus, menu)
        return true
    }

    private fun prepareAudiomark(): Audiomark? {
        album?.let { album ->
            val section = book.sections[album.sectionId - 1]
            return Audiomark(bookId = book.id, sectionTitle = section.title,
                chapterTitle = section.chapters[album.chapterId - 1].title,
                albumIndex = albumIndex, duration = duration.toInt())
        }
        return null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_bookmark -> {
                prepareAudiomark()?.let {
                    app.addAudiomark(it)
                    informUser(title = "Bookmark செய்யப்பட்டது", message = "தாங்கள் " +
                            "கேட்டுக்கொண்டிருக்கும் ஆடியோ வெற்றிகரமாக Bookmark செய்யப்பட்டுள்ளது",
                        AppButton("OK") { })
                }
            }
            R.id.open_book -> {
                askUser(title = "வெளியேற விரும்புகிறீர்களா?",
                    message = "தாங்கள் கேட்டுக்கொண்டிருக்கும் ஆடியோ நிறுத்தப்பட்டு, படிப்பதற்கு இதே புத்தகம் " +
                            "திறக்கப்படும், தங்களுக்கு சம்மதமா?",
                    option1 = AppButton("சம்மதம்") {
                        service?.stopSelf()
                        startActivity(Intent(this@AudioPlayerActivity,
                            BookReaderActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        or Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra(IntentBookId, book.id)
                            putExtra(IntentSectionId, album?.sectionId)
                            putExtra(IntentChapterId, album?.chapterId)
                            putExtra(IntentPageId, 0)
                        })
                    },
                    option2 = AppButton("வேண்டாம்") { })
            }
        }
        return super.onOptionsItemSelected(item)
    }

}

