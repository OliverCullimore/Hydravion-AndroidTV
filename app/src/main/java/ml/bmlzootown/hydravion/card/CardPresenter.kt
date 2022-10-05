package ml.bmlzootown.hydravion.card

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.parseAsHtml
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.leanback.widget.Presenter
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.google.android.material.progressindicator.LinearProgressIndicator
import ml.bmlzootown.hydravion.R
import ml.bmlzootown.hydravion.ext.getTagColor
import ml.bmlzootown.hydravion.models.Video
import ml.bmlzootown.hydravion.models.VideoProgress

class CardPresenter(private val videoProgress: List<VideoProgress>) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = CardViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.card_video, parent, false)
        )
        return ViewHolder(cardView.rootView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        (item as? Video?)?.let { video ->
            CardViewHolder(viewHolder.view).setData(video, videoProgress.find { it.id == video.id })
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        CardViewHolder(viewHolder.view).unBind()
    }

    private class CardViewHolder(val rootView: View) {

        /**
         * Views
         */
        private val image: ImageView = rootView.findViewById(R.id.image)
        private val progress: LinearProgressIndicator = rootView.findViewById(R.id.watch_progress)
        private val duration: TextView = rootView.findViewById(R.id.duration)
        private val title: TextView = rootView.findViewById(R.id.title)
        private val desc: TextView = rootView.findViewById(R.id.desc)
        private val tagList: LinearLayout = rootView.findViewById(R.id.tags)

        /**
         * Colors and objects that need context
         */
        private var defaultPrefs: SharedPreferences
        private var selectedBackgroundColor: Int = -1
        private var defaultBackgroundColor: Int = -1
        private var defaultCardImage: Drawable? = null

        init {
            rootView.context.run {
                defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this)
                defaultBackgroundColor = ContextCompat.getColor(this, R.color.default_background)
                selectedBackgroundColor = ContextCompat.getColor(this, R.color.selected_background)

                /**
                 * This template uses a default image in res/drawable, but the general case for Android TV
                 * will require your resources in xhdpi. For more information, see
                 * https://developer.android.com/training/tv/start/layouts.html#density-resources
                 */
                defaultCardImage = ContextCompat.getDrawable(this, R.drawable.white_plane)
            }
            rootView.onFocusChangeListener =
                View.OnFocusChangeListener { _, isFocused -> setIsSelected(isFocused) }
            setIsSelected(false)
        }

        private fun setIsSelected(isSelected: Boolean) {
            rootView.isSelected = isSelected
            title.isSelected = isSelected

            if (isSelected) {
                rootView.setBackgroundColor(selectedBackgroundColor)
                title.ellipsize = TextUtils.TruncateAt.MARQUEE
                title.marqueeRepeatLimit = -1
                title.isFocusable = true
                title.setHorizontallyScrolling(true)
            } else {
                rootView.setBackgroundColor(defaultBackgroundColor)
                title.ellipsize = TextUtils.TruncateAt.END
            }
        }

        fun setData(video: Video, videoProgress: VideoProgress?) {
            if (video.thumbnail != null && video.thumbnail!!.childImages != null) {
                title.text = video.title

                video.description.parseAsHtml().let { videoDesc ->
                    desc.text = videoDesc

                    if (videoDesc.isBlank()) {
                        desc.isInvisible = true
                        title.textSize = 20f
                    } else {
                        desc.isVisible = true
                        title.textSize = 16f
                    }
                }

                (if ((video.thumbnail?.childImages?.size ?: 0) > 0) {
                    video.thumbnail?.childImages?.get(0)?.path
                } else {
                    video.thumbnail?.path
                })?.let { thumbnail ->
                    Glide.with(rootView.context)
                        //.load(thumbnail)
                        .load(
                            GlideUrl(
                                thumbnail, LazyHeaders.Builder()
                                    .addHeader("User-Agent", "Hydravion (AndroidTV), CFNetwork")
                                    .build()
                            )
                        )
                        .centerCrop()
                        .error(defaultCardImage)
                        .into(image)
                }

                video.metadata?.videoDurationInSecs?.let { totalDurationSecs ->
                    if (totalDurationSecs <= 0) {
                        duration.isGone = true
                    } else {
                        duration.isVisible = true
                        duration.text = formatDuration(totalDurationSecs)
                    }
                } ?: run { duration.isGone = true }

                if (videoProgress != null) {
                    progress.isVisible = true
                    progress.min = 0
                    progress.max = 100 // to get in milliseconds like watch time
                    progress.progress = videoProgress.progress
                } else {
                    progress.isGone = true
                }

                if (video.tags.isNotEmpty()) {
                    tagList.removeAllViews()
                    tagList.visibility = View.VISIBLE
                    desc.maxLines = 1

                    video.tags.forEach { tag ->
                        (LayoutInflater.from(rootView.context)
                            .inflate(R.layout.view_tag, tagList, false) as TextView).apply {
                            text = "#$tag"
                            backgroundTintList = ColorStateList.valueOf(rootView.context.getTagColor(tag))
                            tagList.addView(this)
                        }
                    }
                } else {
                    tagList.removeAllViews()
                    tagList.visibility = View.GONE
                    desc.maxLines = 2
                }
            }
        }

        fun formatDuration(durationSecs: Int): String =
            DateUtils.formatElapsedTime(durationSecs.toLong())

        fun unBind() {
            image.setImageDrawable(null)
        }
    }
}