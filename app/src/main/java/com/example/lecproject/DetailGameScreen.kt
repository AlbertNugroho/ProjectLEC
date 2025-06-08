package com.example.lecproject

import ThumbnailAdapter
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.text.Html
import android.util.TypedValue
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.example.lecproject.databinding.FragmentDetailGameScreenBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Locale

class DetailGameScreen : Fragment() {
    private var videoUrlToPlay: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private var _binding: FragmentDetailGameScreenBinding? = null
    private val binding get() = _binding!!
    private var game: GameDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game = arguments?.getParcelable(Home.NEXT_SCREEN) as? GameDetail
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailGameScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.videoView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                videoUrlToPlay?.let { url ->
                    startMediaPlayer(url, surface)
                }
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                mediaPlayer?.release()
                mediaPlayer = null
                return true
            }
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
        val rubikBoldTypeface = ResourcesCompat.getFont(requireContext(), R.font.rubik_bold)
        game?.let { gameDetail ->

            binding.gameName.text = gameDetail.name
            binding.ratingtxt.text = String.format(Locale.US, "⭐%.1f/5", gameDetail.rating)
            binding.totaldownloads.text = gameDetail.added.toString()
            binding.averageplaytext.text = gameDetail.rating.toString()
            binding.developerName.text =
                gameDetail.developers.joinToString(", ") { it.name }
            binding.description.text =
                Html.fromHtml(gameDetail.description ?: "", Html.FROM_HTML_MODE_LEGACY)

            binding.description.post {
                if (binding.description.lineCount <= 3) {
                    binding.toggleDescription.visibility = View.GONE
                } else {
                    binding.toggleDescription.visibility = View.VISIBLE
                    binding.toggleDescription.text = "Show more"
                    binding.description.maxLines = 3
                }
            }
            val container = binding.descriptionContainer
            binding.toggleDescription.setOnClickListener {
                val isExpanded = binding.description.maxLines == Int.MAX_VALUE
                val transition = AutoTransition()
                TransitionManager.beginDelayedTransition(container, transition)

                if (isExpanded) {
                    binding.description.maxLines = 3
                    binding.toggleDescription.text = "Show more"
                } else {
                    binding.description.maxLines = Int.MAX_VALUE
                    binding.toggleDescription.text = "Show less"
                }
            }


            setupThumbnails(gameDetail)
            val tagContainer = binding.tagContainer
            tagContainer.removeAllViews()

            for (tag in gameDetail.tags) {
                val tagView = TextView(requireContext()).apply {
                    text = tag.name
                    setPadding(24, 8, 24, 8)
                    setTextColor(ContextCompat.getColor(context, R.color.snow))
                    setBackgroundResource(R.drawable.rounded_tag_bg) // Rounded bg if you want
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    typeface = rubikBoldTypeface

                    val params = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(8, 8, 8, 8)
                    layoutParams = params
                }
                tagContainer.addView(tagView)
            }
            val entries = ArrayList<BarEntry>()
            val ratingsList = gameDetail.ratings
            ratingsList.forEachIndexed { index, rating ->
                val value = if (rating.count == 0) 0.1f else rating.count.toFloat()
                entries.add(BarEntry(index.toFloat(), value))
            }
            val colors = listOf(
                Color.parseColor("#FFD700"), // Gold
                Color.parseColor("#32CD32"), // Lime Green
                Color.parseColor("#A9A9A9"), // Dark Gray
                Color.parseColor("#DC143C")  // Crimson
            )
            val dataSet = BarDataSet(entries, "User Ratings").apply {
                setColors(colors)
                valueTextSize = 14f
                valueTextColor = ContextCompat.getColor(requireContext(), R.color.snow)
                valueTypeface = rubikBoldTypeface
            }
            val barData = BarData(dataSet).apply {
                barWidth = 0.6f
            }
            binding.barChart.apply {
                data = barData

                // Customize X Axis
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(listOf("🌟", "👍", "😐", "👎"))
                    granularity = 1f
                    setDrawGridLines(false)
                    textSize = 18f
                    textColor = ContextCompat.getColor(context, R.color.snow)
                    typeface = rubikBoldTypeface
                    position = XAxis.XAxisPosition.BOTTOM
                }
                axisLeft.apply {
                    axisMinimum = 0f
                    textSize = 13f
                    textColor = ContextCompat.getColor(context, R.color.snow)
                    typeface = rubikBoldTypeface
                    setDrawGridLines(true)
                }
                val maxValue = ratingsList.maxOfOrNull { it.count.toFloat() } ?: 0f
                binding.barChart.axisLeft.axisMaximum = maxValue + 10f
                binding.barChart.setExtraTopOffset(20f)
                binding.barChart.setExtraBottomOffset(12f)
                dataSet.valueTextSize = 14f

                axisRight.isEnabled = false

                legend.isEnabled = false
                description.isEnabled = false



                setScaleEnabled(false)
                setPinchZoom(false)

                animateY(1000, Easing.EaseInOutQuad)

                invalidate()
            }

            val platformContainer = binding.platformContainer
            platformContainer.removeAllViews()

            gameDetail.platforms.forEach { platformWrapper ->
                val platformView = TextView(requireContext()).apply {
                    text = platformWrapper.platform.name
                    setPadding(24, 8, 24, 8)
                    setTextColor(ContextCompat.getColor(context, R.color.snow))
                    setBackgroundResource(R.drawable.rounded_tag_bg)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    typeface = rubikBoldTypeface

                    val params = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(8, 8, 8, 8)
                    layoutParams = params
                }
                platformContainer.addView(platformView)
            }

        }
    }
    private fun setupThumbnails(gameDetail: GameDetail) {
        val thumbnails = gameDetail.toThumbnailList()
            .sortedByDescending { it.isVideo }

        val adapter = ThumbnailAdapter(thumbnails) { item ->
            if (item.isVideo && item.videoUrl != null) {
                playVideoOnTextureView(item.videoUrl)
            } else {
                stopVideo()
                binding.screenshotView.visibility = View.VISIBLE
                Glide.with(this).load(item.url).into(binding.screenshotView)
            }
        }

        binding.thumbnailViewPager.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.thumbnailViewPager.adapter = adapter

        if (thumbnails.isNotEmpty()) {
            adapter.onItemClick(thumbnails.first())
        }
    }

    private fun playVideoOnTextureView(videoUrl: String) {
        binding.screenshotView.visibility = View.GONE
        binding.videoView.visibility = View.VISIBLE

        videoUrlToPlay = videoUrl

        if (binding.videoView.isAvailable) {
            startMediaPlayer(videoUrl, binding.videoView.surfaceTexture!!)
        }
    }

    private fun startMediaPlayer(videoUrl: String, surfaceTexture: SurfaceTexture) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        } else {
            mediaPlayer?.reset()
        }

        mediaPlayer?.apply {
            setDataSource(videoUrl)
            setSurface(Surface(surfaceTexture))
            isLooping = true
            setOnPreparedListener {
                it.start()
            }
            prepareAsync()
        }
    }


    private fun stopVideo() {
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        binding.videoView.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(game: GameDetail): DetailGameScreen {
            return DetailGameScreen().apply {
                arguments = Bundle().apply {
                    putParcelable(Home.NEXT_SCREEN, game)
                }
            }
        }
    }
}
