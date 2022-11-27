package com.whiture.apps.tamil.thousand.nights

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.*
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.whiture.apps.tamil.thousand.nights.models.AlbumData

/**
 * In general, a media player can be played within an activity
 * However, in our case, there should be a media notification for outside app listening
 * The notification is of Media Style (means you will have play/pause, prev, next, seekbar etc
 * and the notification should stay as foreground service and continue to play the audio
 *
 * The service will be invoked by our activity (both bind and start). The service will send the
 * media notification and start playing the audio.
 *
 * the lifecycle methods of service is such that most methods gets called multiple times
 * e-g., onBind, onStartCommand, onUnbind, onRebind
 * only two methods get called once - onCreate and onDestroy
 */
class AudioService: Service() {

    companion object AudioServiceConstants {
        const val actionKey = "actionKey"
        const val playAction = "mediaPlayerPlay"
        const val pauseAction = "mediaPlayerPause"
        const val nextAction = "mediaPlayerNext"
        const val prevAction = "mediaPlayerPrev"
        const val stopAction = "mediaPlayerStop"
    }

    // different states of audio
    // media player doesn't expose its states, and any call during wrong state will result in exception
    // https://developer.android.com/reference/android/media/MediaPlayer
    // refer above link for media player states
    private enum class Status {
        Init,      // very initial stage, nothing is set, not yet buffered
        Buffering, // currently the buffering is happening, the very first buffering before it starts
        Streaming, // the player is streaming / playing the audio
        Paused,    // the player is paused by the user
        Streamed,  // the streaming / playing of the audio clip is done
        Error;     // error

        fun mediaSessionState(): Int = when(this) {
            Init -> PlaybackStateCompat.STATE_NONE
            Buffering -> PlaybackStateCompat.STATE_BUFFERING
            Streaming -> PlaybackStateCompat.STATE_PLAYING
            Paused -> PlaybackStateCompat.STATE_PAUSED
            Streamed -> PlaybackStateCompat.STATE_STOPPED
            Error -> PlaybackStateCompat.STATE_ERROR
        }
    }
    private var status: Status = Status.Init

    // variable to keep record of the last known media player seekbar position
    private var lastSeekBarPosition: Long = 0

    // starting seek position, if user wants to play from what they heard last
    private var startSeekBarPosition: Long = 0

    // listener class for the audio events
    private var listener: IAudioListener? = null

    private var mediaPlayer: MediaPlayer = MediaPlayer().apply {
        setOnCompletionListener {
            status = Status.Streamed
            handler.removeCallbacks(runnable) // stop the timer
            lastSeekBarPosition = 0
            playlist[index].repeat -= 1
            listener?.playCompleted(playlist[index])
            // completed move to next if any
            mNext()
        }

        setOnErrorListener { mp, what, _ ->
            status = Status.Error
            listener?.error(playlist[index])
            true
        }

        setOnBufferingUpdateListener { mp, perc ->
            // buffering will not happen 100% straight
            // it might be buffering to 100% as the audio is being played out
            // log("Buffering: $perc")
        }

        setOnInfoListener { _, info, _ ->
            // the media info here, you can check for the media buffering start and stop here
            true
        }

        setOnPreparedListener {
            // handle the media player getting prepared after the buffering
            status = Status.Streaming
            mStart()
        }
    }

    // media session object to connect with MediaPlayer via media controls in Notification
    private lateinit var mediaSession: MediaSessionCompat

    // Audio Manager is used for listening to physical volume control changes and reacting to that
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private lateinit var audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener

    // handler and runnable for timer - clocks at every 1 sec
    // used to update the delegate about the progress
    // used to stop the media player at a specified interval
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val runnable: Runnable = Runnable { timeClocked() }

    // audio data here
    private var playlist: Array<AlbumData> = emptyArray()
    private var index = -1

    private lateinit var icon: Bitmap

    // the service is being created for the first time, one time call
    override fun onCreate() {
        super.onCreate()

        // for Oreo+ versions, we should have a notification channel
        // before we can send a notification in system tray
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // this should match with notification in MediaService
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("mediaPlayerNotification",
                    "audio_book_notification",
                    NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Audio Book Notification"
                })
        }

        // prepare the notification icon bitmap
        icon = BitmapFactory.decodeResource(resources,
            R.drawable.image_not_found_thumbnail)

        mediaSession = MediaSessionCompat(this, "playerAudio")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
            when (it) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    mediaPlayer.setVolume(1.0f, 1.0f)
                }
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> { }
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> { }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    // just in case we lost audio focus due to bluetooth connection etc
                    if (mediaPlayer.isPlaying) {
                        mPause()
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // just in case we lost audio focus due to bluetooth connection etc
                    if (mediaPlayer.isPlaying) {
                        mPause()
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    mediaPlayer.setVolume(0.1f, 0.1f)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(
                    AudioAttributes.USAGE_UNKNOWN).build())
                .setWillPauseWhenDucked(false)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
        }
    }

    // service is being bound by an activity, gets called everytime an activity binds it
    // using bindService method call
    override fun onBind(intent: Intent?): IBinder? {
        return MediaBinder()
    }

    // service is being started by an activity, gets called everytime an activity starts it
    // however the same service instance will be running, and this method gets called everytime
    // using startService method call
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.extras?.getString(actionKey)) {
            playAction -> { mPlay() }
            pauseAction -> { mPause() }
            stopAction -> { mStop() }
            nextAction -> { mNext() }
            prevAction -> { mPrev() }
            else -> { }
        }
        return START_NOT_STICKY
    }

    // the service has been unbind by an activity, gets called everytime an activity unbinds it
    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    // the service is being re-bound by an activity after it has unbound the same service
    // gets called several times
    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    inner class MediaBinder: Binder() {
        fun getService(): AudioService {
            return this@AudioService
        }
    }

    // method to start the stream for the given URL: external http URL
    // if the streaming is already happening, the current index will be returned
    fun stream(playlist: Array<AlbumData>, albumIndex: Int = 0, startTime: Long = 0,
               listener: IAudioListener? = null): Int {
        this.listener = listener
        if (status == Status.Init) {
            this.playlist = playlist
            this.startSeekBarPosition = startTime
            if (playlist.isNotEmpty()) { index = albumIndex }
            prepareMediaPlayer()
        }
        return index
    }

    // method to update the image as soon as it is loaded from the activity
    fun iconLoaded(icon: Bitmap) {
        this.icon = icon
        showNotification() // show the notification, this will update the image
    }

    // method to get the current playing album index
    fun currentAlbum(): Int = index

    // method to prepare the media player for the buffering
    private fun prepareMediaPlayer() {
        mediaPlayer.apply {
            status = Status.Buffering // start the buffering
            listener?.bufferingStarted(playlist[index])
            setAudioAttributes(
                AudioAttributes.Builder().setContentType(
                AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build())
            setDataSource(playlist[index].url)
            prepareAsync() // this will actually start the buffering process
        }
    }

    // method to start the play
    private fun mStart() {
        if (gainAudioFocus()) {
            lastSeekBarPosition = startSeekBarPosition // reset it
            startSeekBarPosition = 0 // reset it, only for the very first time
            mediaPlayer.setVolume(1.0f, 1.0f)
            mediaPlayer.seekTo(lastSeekBarPosition.toInt())
            mediaPlayer.start()
            listener?.playStarted(playlist[index])
            showNotification()
            initTimer() // kick the timer
        }
    }

    // method to handle the play / pause button press
    fun mPlayPause() {
        if (mediaPlayer.isPlaying) mPause() else mPlay()
    }

    fun mPlay() {
        if (status == Status.Paused) {
            status = Status.Streaming
            mediaPlayer.start()
            listener?.playStarted(playlist[index])
            initTimer() // start the timer again
            showNotification() // this will re-display the notification
        }
    }

    fun mPause() {
        if (status == Status.Streaming) {
            status = Status.Paused
            listener?.playPaused(playlist[index])
            handler.removeCallbacks(runnable) // stop the timer
            lastSeekBarPosition = mediaPlayer.currentPosition.toLong()
            mediaPlayer.pause()
            showNotification() // this will re-display the notification
        }
    }

    fun mStop() {
        status = Status.Init
        mediaPlayer.stop()
        mediaPlayer.reset()
        releaseAudioFocus()
        handler.removeCallbacks(runnable) // stop the timer
        // remove the notification from the tray
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        else {
            stopForeground(true)
        }
        stopSelf() // stop the service
    }

    // forward the media player to given secs
    fun mForward(secs: Int): Int {
        if (status == Status.Streaming || status == Status.Paused) {
            var target = mediaPlayer.currentPosition + (secs * 1000)
            if (target > playlist[index].length) {
                target = playlist[index].length.toInt()
            }
            mSeekTo(target.toLong())
            return target
        }
        return -1
    }

    // backward the media player to given secs
    fun mBackward(secs: Int): Int {
        if (status == Status.Streaming || status == Status.Paused) {
            var target = mediaPlayer.currentPosition - (secs * 1000)
            if (target < 0) {
                target = 0
            }
            mSeekTo(target.toLong())
            return target
        }
        return -1
    }

    fun mNext() {
        // check if the current audio play-back is completed with repeat-count
        if (playlist[index].repeat <= 0) {
            if (index < playlist.size - 1) {
                index += 1
                status = Status.Init
                mediaPlayer.stop()
                mediaPlayer.reset()
                prepareMediaPlayer()
            }
        }
        else { // repeat the same audio again
            playlist[index].repeat -= 1
            status = Status.Paused
            mSeekTo(0)
            mPlay()
        }
    }

    fun mPrev() {
        lastSeekBarPosition = mediaPlayer.currentPosition.toLong()
        // <=2 secs, move to the previous album if any
        // 2+ secs, don't move to the previous album, set the seekbar to beginning of the current album
        if (lastSeekBarPosition <= 2000) {
            if (index > 0) {
                index -= 1
                status = Status.Init
                mediaPlayer.stop()
                mediaPlayer.reset()
                prepareMediaPlayer()
            }
        }
        else {
            mSeekTo(0)
        }
    }

    fun mSeekTo(time: Long) {
        if (status == Status.Streaming || status == Status.Paused) {
            lastSeekBarPosition = time
            mediaPlayer.seekTo(time.toInt())
            showNotification()
        }
    }

    fun mJumpTo(albumIndex: Int) {
        if (albumIndex < playlist.size) {
            this.index = albumIndex
            status = Status.Init
            mediaPlayer.stop()
            mediaPlayer.reset()
            prepareMediaPlayer()
        }
    }

    // called when app is removed from recent apps...
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopForeground(true)
        // do this, so that user will keep our app live, when it requires,
        // he / she will open the app to see the ad
        stopSelf() // this will stop the service from playing the audio
    }

    // the service has to run as a foreground service, so it will not be get killed
    // to run it as a foreground service, we need to keep a notification
    // the notification style should be a media player
    private fun showNotification() {
        // A notification channel is registered in Application class
        // refer this link for seekbar implementation on the notification
        // https://stackoverflow.com/questions/59516981/android-10-seek-bar-on-notification
        var actionsCount = 0
        // prepare media session here
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, icon)
                .putString(MediaMetadata.METADATA_KEY_TITLE, playlist[index].title)
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, playlist[index].desc)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, playlist[index].length)
                .build())
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                            or PlaybackStateCompat.ACTION_PLAY_PAUSE
                            or PlaybackStateCompat.ACTION_PAUSE
                            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                            or PlaybackStateCompat.ACTION_SEEK_TO)
                .setState(status.mediaSessionState(), lastSeekBarPosition, 1f)
                .build())
        // make it in main thread, this will be called from the seekbar change in notification
        Handler(Looper.getMainLooper()).post {
            mediaSession.setCallback(object: MediaSessionCompat.Callback() {
                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    mSeekTo(pos)
                }
            })
        }

        fun getIntent(action: String) = Intent(this,
            AudioBroadcastReceiver::class.java).setAction(action)

        fun updateFlag(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else
            PendingIntent.FLAG_UPDATE_CURRENT

        fun cancelFlag(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT else
            PendingIntent.FLAG_CANCEL_CURRENT

        fun getPendingIntent(action: String, isUpdate: Boolean) = PendingIntent.getBroadcast(
            this@AudioService, 0, getIntent(action),
            if (isUpdate) updateFlag() else cancelFlag())

        // Note: This should match with id in application class
        val builder = NotificationCompat.Builder(this,
            "mediaPlayerNotification").apply {
            foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            setSmallIcon(android.R.drawable.ic_media_play)
            setLargeIcon(icon)
            setContentTitle(playlist[index].title)
            setContentText(playlist[index].desc)
            priority = NotificationCompat.PRIORITY_HIGH
            setContentIntent(
                PendingIntent.getActivities(this@AudioService, 0,
                arrayOf(Intent(this@AudioService, MainActivity::class.java),
                    Intent(this@AudioService, MainActivity::class.java)), updateFlag()))
            setOngoing(true)
            setShowWhen(false)
            // there will be four actions added, based on certain conditions
            // each action will point to a button on the media controller notification
            // the pending intent will be sent back to the broadcast receiver
            // the broadcast receiver will in turn start the service with the passed intent
            // so, this will be handled in startCommand method call
            addAction(android.R.drawable.ic_media_previous, "previous",
                getPendingIntent(prevAction, true))
            if (status == Status.Streaming) {
                addAction(android.R.drawable.ic_media_pause, "pause",
                    getPendingIntent(pauseAction, true))
            }
            else {
                addAction(android.R.drawable.ic_media_play, "play",
                    getPendingIntent(playAction, true))
            }
            addAction(android.R.drawable.ic_media_next, "next",
                getPendingIntent(nextAction, true))
            addAction(android.R.drawable.ic_menu_close_clear_cancel, "stop",
                getPendingIntent(stopAction, false))
        }

        // here, the notification is in media style
        actionsCount = 4 // this will change based on some conditions
        builder.setStyle(androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(actionsCount).setMediaSession(mediaSession.sessionToken))
        startForeground(1, builder.build())
        // to move service from background to foreground Req for API 26 and above..!
    }

    // method to gain the audio focus for changing the volume control
    private fun gainAudioFocus(): Boolean = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        AudioManager.AUDIOFOCUS_GAIN == audioManager.requestAudioFocus(audioFocusChangeListener,
            AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }
    else {
        (audioFocusRequest != null && AudioManager.AUDIOFOCUS_GAIN ==
                audioManager.requestAudioFocus(audioFocusRequest!!))
    }

    // method to free the audio focus
    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        else {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }
    }

    // time has been clocked by the runnable for every 1 sec
    private fun timeClocked() {
        lastSeekBarPosition = mediaPlayer.currentPosition.toLong()
        handler.postDelayed(runnable, 1000)
        listener?.playing(lastSeekBarPosition)
    }

    // method to initialise the timer for notifying the listener about the current seek bar position
    private fun initTimer() {
        handler.apply {
            removeCallbacks(runnable)
            postDelayed(runnable, 1000)
        }
    }

    // final destroy method call, release all the resources here
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.reset()
        mediaPlayer.release()
        handler.removeCallbacks(runnable) // stop the timer
        releaseAudioFocus()
    }

}

