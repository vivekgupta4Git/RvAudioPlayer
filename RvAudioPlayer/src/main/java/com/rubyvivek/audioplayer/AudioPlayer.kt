package com.rubyvivek.audioplayer

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

private const val MILLIS_TO_HOUR = 3600000L

private const val MILLIS_TO_MINUTES = 60000L

private const val MILLIS_TO_SECONDS = 1000L
class RvAudioPlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val mSeekBarHandler = Handler()
    private lateinit var updateSeekBar : Runnable

    private val mainLooper = Handler(Looper.getMainLooper())
    private lateinit var updateText : Runnable

    private var mediaPlayer: MediaPlayer = MediaPlayer()
    private val playButton: AppCompatImageView by lazy {
        findViewById(R.id.playButton)
    }
    private val seekBar: AppCompatSeekBar by lazy {
        findViewById(R.id.seeker)
    }
    private val endPositionText: AppCompatTextView by lazy {
        findViewById(R.id.endPosText)
    }
    private val startPositionText: AppCompatTextView by lazy {
        findViewById(R.id.startPosText)
    }
    private val audioFileImageView: AppCompatImageView by lazy {
        findViewById(R.id.audioFile)
    }
    private val backgroundImage: AppCompatImageView by lazy {
        findViewById(R.id.backgroundImage)
    }
    private val playIcon: Drawable? by lazy {
        AppCompatResources.getDrawable(context, R.drawable.round_play_arrow_24)
    }
    private val pauseIcon: Drawable? by lazy {
        AppCompatResources.getDrawable(context, R.drawable.baseline_pause_24)
    }
    private val audioFileIcon: Drawable? by lazy {
        AppCompatResources.getDrawable(context, R.drawable.outline_audio_file_24)
    }
    private val container : ConstraintLayout by lazy{
        findViewById(R.id.container)
    }
    private var audioUri: String? = null
    private var backgroundUri: String? = null
    private var mediaState: MediaPlayerState = MediaPlayerState.IDLE
    private val prepareListener = MediaPlayer.OnPreparedListener {
        Log.d("MyLog", "Media State = PREPARED")
        mediaState = MediaPlayerState.PREPARED
        seekBar.max = it.duration
        endPositionText.text = formattedDuration(it.duration.toLong())
    }
    private val onCompletedListener = MediaPlayer.OnCompletionListener {
        Log.d("MyLog", "Media State = Completed")
        mediaState = MediaPlayerState.COMPLETED
        mSeekBarHandler.removeCallbacks(updateSeekBar)
        mainLooper.removeCallbacks(updateText)

        if(backgroundUri!=null)
            playIcon?.setTint(Color.WHITE)

        playButton.setImageDrawable(
            playIcon
        )
        postDelayed({
            startPositionText.text = "00:00"
            seekBar.progress = 0
        }, 100)

    }


    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.audio_player, this)
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AudioPlayer,
            0, 0
        ).apply {
            try {
                audioUri = getString(R.styleable.AudioPlayer_audioUri)
                backgroundUri = getString(R.styleable.AudioPlayer_setBackGroundUri)
            } finally {
                recycle()
            }
        }
        if (audioUri != null) {
            val uri = Uri.parse(audioUri)
            Log.d("MyLog", "Media State = Preparing....")

            mediaPlayer = MediaPlayer.create(context, uri)
        }
        if (backgroundUri != null) {
            showBackGround()
        } else
            backgroundImage.visibility = View.GONE

        mediaPlayer.setOnPreparedListener(prepareListener)
        mediaPlayer.setOnCompletionListener(onCompletedListener)

        initializedListener()
        updateSeekBar = Runnable {
            if(mediaState == MediaPlayerState.PLAYING)
                seekBar.progress = mediaPlayer.currentPosition

            mSeekBarHandler.postDelayed(updateSeekBar,50)
        }

        updateText = Runnable {
            if(mediaState == MediaPlayerState.PLAYING)
                startPositionText.text = formattedDuration(mediaPlayer.currentPosition.toLong())

            mainLooper.postDelayed(updateText,15)
        }
    }

    private fun showBackGround() {
        container.foreground =
            AppCompatResources.getDrawable(context, R.drawable.gradient_shadow_background)

        backgroundUri?.let {
            backgroundImage.visibility = View.VISIBLE
            Glide.with(context)
                .asDrawable()
                .load(it)
                .into(object : CustomTarget<Drawable>(){
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        container.background = resource
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }

                })


            playIcon?.setTint(Color.WHITE)
            playButton.setImageDrawable(playIcon)

            startPositionText.setTextColor(Color.WHITE)

            seekBar.progressTintList = ColorStateList.valueOf(Color.WHITE)
            seekBar.thumbTintList = ColorStateList.valueOf(Color.WHITE)

            endPositionText.setTextColor(Color.WHITE)

            audioFileIcon?.setTint(Color.WHITE)
            audioFileImageView.setImageDrawable(audioFileIcon)
        }
        postInvalidate()

    }

    fun setBackgroundImage(uri: Uri?) {
        if (uri == null) {
            backgroundImage.visibility = View.GONE
            return
        }

        backgroundUri = uri.toString()
        showBackGround()
    }

    fun setAudioUri(uri: Uri?) {
        audioUri = uri.toString()
        audioUri?.let {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mediaPlayer.setDataSource(context, Uri.parse(it))
            mediaPlayer.setOnPreparedListener(prepareListener)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnCompletionListener(onCompletedListener)

        }
        postInvalidate()
    }

    private fun initializedListener() {

        playButton.setOnClickListener {
            when (mediaState) {
                MediaPlayerState.IDLE -> {
                    mediaPlayer = MediaPlayer.create(context, Uri.parse(audioUri))
                    mediaPlayer.setOnPreparedListener(prepareListener)
                    mediaPlayer.setOnCompletionListener(onCompletedListener)
                    if (backgroundUri != null)
                        pauseIcon?.setTint(Color.WHITE)

                    playButton
                        .setImageDrawable(pauseIcon)
                    mediaPlayer.start()
                    mSeekBarHandler.postDelayed(updateSeekBar,0)
                    mainLooper.postDelayed(updateText,0)
                }

                MediaPlayerState.PLAYING -> {
                    if (backgroundUri != null)
                        playIcon?.setTint(Color.WHITE)

                    playButton
                        .setImageDrawable(playIcon)

                    mediaState = MediaPlayerState.PAUSED
                    mediaPlayer.pause()
                    mSeekBarHandler.removeCallbacks(updateSeekBar)
                    mainLooper.removeCallbacks(updateText)
                    Log.d("MyLog", "Media State = Paused")

                }
                MediaPlayerState.COMPLETED, MediaPlayerState.PREPARED -> {
                    if (backgroundUri != null)
                        pauseIcon?.setTint(Color.WHITE)

                    playButton
                        .setImageDrawable(pauseIcon)
                    mediaPlayer.start()
                    mediaState = MediaPlayerState.PLAYING
                    mSeekBarHandler.postDelayed(updateSeekBar,0)
                    mainLooper.postDelayed(updateText,0)

                    Log.d("MyLog", "Media State = Playing")

                }
                MediaPlayerState.PAUSED -> {
                    if (backgroundUri != null)
                        pauseIcon?.setTint(Color.WHITE)

                    playButton
                        .setImageDrawable(pauseIcon)

                    mediaPlayer.start()
                    mediaState = MediaPlayerState.PLAYING
                    mSeekBarHandler.postDelayed(updateSeekBar,0)
                    mainLooper.postDelayed(updateText,0)

                    Log.d("MyLog", "Media State = Playing")

                }

            }
        }


    }


    private enum class MediaPlayerState {
        IDLE,
        PLAYING,
        PAUSED,
        COMPLETED,
        PREPARED
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mediaState = MediaPlayerState.IDLE
        mSeekBarHandler.removeCallbacks(updateSeekBar)
        mainLooper.removeCallbacks(updateText)
        mediaPlayer.release()
        seekBar.setOnSeekBarChangeListener(null)
        Log.d("MyLog", "Media State = Released")

    }

    private fun formattedDuration(duration: Long): String {

        val hourDuration = duration / MILLIS_TO_HOUR
        val durationRemaining = duration - (hourDuration * MILLIS_TO_HOUR)

        val durationRemainingToMinutes = durationRemaining / (MILLIS_TO_MINUTES)

        val durationRemainingAgain =
            durationRemaining - (durationRemainingToMinutes * MILLIS_TO_MINUTES)
        val durationRemainingAgainToSeconds = durationRemainingAgain / MILLIS_TO_SECONDS


        val hourText = formatText(hourDuration, false)
        val minuteText = formatText(durationRemainingToMinutes)
        val secondText = formatText(durationRemainingAgainToSeconds)

        return if (hourText.isEmpty())
            "$minuteText:$secondText"
        else
            "$hourText:$minuteText:$secondText"
    }

    private fun formatText(duration: Long, usePrefix: Boolean = true): String {
        return if (duration == 0L) {

            if (usePrefix)
                "00"
            else
                ""
        } else if (duration < 10)
            "0$duration"
        else
            "$duration"
    }
}