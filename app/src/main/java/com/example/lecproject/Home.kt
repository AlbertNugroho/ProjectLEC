package com.example.lecproject

import GameRepository
import RawgApiService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.lecproject.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class Home : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    private lateinit var binding: FragmentHomeBinding

    private lateinit var adapter: GameSliderAdapter
    private lateinit var upcomingAdapter: GameSliderAdapter
    private lateinit var gameBlockAdapter: GameBlockAdapter

    private val topRatedList = ArrayList<Game>()
    private val upcomingList = ArrayList<Game>()
    private val randomGamesList = ArrayList<Game>()

    private lateinit var dots: ArrayList<TextView>
    private lateinit var upcomingDots: ArrayList<TextView>

    private lateinit var autoScrollHandlerMain: Handler
    private lateinit var autoScrollRunnableMain: Runnable
    private lateinit var autoScrollHandlerUpcoming: Handler
    private lateinit var autoScrollRunnableUpcoming: Runnable

    private lateinit var apiKey: String
    private lateinit var api: RawgApiService
    private lateinit var repository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        apiKey = "1d39e3d160fb4a4bafb73846d473ff9e" // Use your actual API key here
        api = RawgApiClient.retrofit.create(RawgApiService::class.java)
        repository = GameRepository(api, apiKey)

        autoScrollHandlerMain = Handler(Looper.getMainLooper())
        autoScrollRunnableMain = object : Runnable {
            override fun run() {
                val itemCount = adapter.itemCount
                if (itemCount == 0) return
                val nextItem = (binding.carousel.currentItem + 1) % itemCount
                binding.carousel.currentItem = nextItem
                autoScrollHandlerMain.postDelayed(this, 3000)
            }
        }

        autoScrollHandlerUpcoming = Handler(Looper.getMainLooper())
        autoScrollRunnableUpcoming = object : Runnable {
            override fun run() {
                val itemCount = upcomingAdapter.itemCount
                if (itemCount == 0) return
                val nextItem = (binding.carouselUpcoming.currentItem + 1) % itemCount
                binding.carouselUpcoming.currentItem = nextItem
                autoScrollHandlerUpcoming.postDelayed(this, 3000)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = GameSliderAdapter(topRatedList)
        upcomingAdapter = GameSliderAdapter(upcomingList)
        gameBlockAdapter = GameBlockAdapter(randomGamesList)

        binding.carousel.adapter = adapter
        binding.carouselUpcoming.adapter = upcomingAdapter

        val recyclerView = view.findViewById<RecyclerView>(R.id.randomGames)
        recyclerView.layoutManager = NonscrollableGridLayout(requireContext(), 2)
        recyclerView.adapter = gameBlockAdapter

        dots = ArrayList()
        upcomingDots = ArrayList()

        setIndicator(dots, binding.dotsIndicator, topRatedList, 0)
        setIndicator(upcomingDots, binding.dotsIndicatorUpcoming, upcomingList, 0)
        updateCurrentDot(dots, 0)
        updateCurrentDot(upcomingDots, 0)

        binding.carousel.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateCurrentDot(dots, position)
                super.onPageSelected(position)
            }
        })

        binding.carouselUpcoming.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateCurrentDot(upcomingDots, position)
                super.onPageSelected(position)
            }
        })

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)

        binding.YearTopRated.text = "Top Rated Games in $year"

        val dates = "$year-01-01,$year-12-31"

        lifecycleScope.launch {
            fetchTopRatedGames(dates, calendar)
            fetchUpcomingGames(year, calendar)
            fetchRandomGames()
        }

        adapter.setOnClickListener(createGameClickListener(repository))
        upcomingAdapter.setOnClickListener(createGameClickListener(repository))

        gameBlockAdapter.setOnClickListener(object : GameBlockAdapter.OnClickListener {
            override fun onClick(position: Int, model: Game) {
                lifecycleScope.launch {
                    val gameDetail = repository.fetchFullGameDetail(model.id)
                    if (gameDetail != null) {
                        val fragment = DetailGameScreen()
                        val bundle = Bundle().apply {
                            putParcelable(NEXT_SCREEN, gameDetail)
                        }
                        fragment.arguments = bundle
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, fragment)
                            .addToBackStack(null)
                            .commit()
                    } else {
                        Log.e("RAWG", "Error fetching full game detail for ID: ${model.id}")
                    }
                }
            }
        })
    }

    private suspend fun fetchTopRatedGames(dates: String, calendar: Calendar) = withContext(Dispatchers.IO) {
        try {
            val response = api.getGames(apiKey, "-rating", dates, true)
            if (response.isSuccessful) {
                val games = response.body()?.results ?: emptyList()
                val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd").format(calendar.time)
                val releasedGames = games.filter { it.released != null && it.released <= currentDate }
                val sortedGames = releasedGames.sortedByDescending { it.rating ?: 0f }
                val filteredGames = sortedGames.filter { game ->
                    game.esrb_rating?.slug?.lowercase() != "ao"
                }
                topRatedList.clear()
                topRatedList.addAll(filteredGames)

                fetchDeveloperNames(topRatedList, 30)

                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                    setIndicator(dots, binding.dotsIndicator, topRatedList, 0)
                    updateCurrentDot(dots, 0)
                    if (topRatedList.isNotEmpty()) {
                        autoScrollHandlerMain.postDelayed(autoScrollRunnableMain, 3000)
                    }
                }
            } else {
                Log.e("RAWG", "API Error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("RAWG", "Network Error in fetchTopRatedGames: ${e.localizedMessage}")
        }
    }

    private suspend fun fetchUpcomingGames(year: Int, calendar: Calendar) = withContext(Dispatchers.IO) {
        try {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowStr = java.text.SimpleDateFormat("yyyy-MM-dd").format(tomorrow.time)
            val upcomingDates = "$tomorrowStr,$year-12-31"

            val response = api.getUpcoming(apiKey, "released", upcomingDates, true)
            if (response.isSuccessful) {
                val games = response.body()?.results ?: emptyList()
                val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd").format(calendar.time)
                val upcomingGames = games.filter { it.released != null && it.released > currentDate }
                val sortedUpcoming = upcomingGames.sortedBy { it.released }
                val filteredGames = sortedUpcoming.filter { game ->
                    game.esrb_rating?.slug?.lowercase() != "ao"
                }
                upcomingList.clear()
                upcomingList.addAll(filteredGames)

                fetchDeveloperNames(upcomingList, 30)

                withContext(Dispatchers.Main) {
                    upcomingAdapter.notifyDataSetChanged()
                    setIndicator(upcomingDots, binding.dotsIndicatorUpcoming, upcomingList, 0)
                    updateCurrentDot(upcomingDots, 0)
                    if (upcomingList.isNotEmpty()) {
                        autoScrollHandlerUpcoming.postDelayed(autoScrollRunnableUpcoming, 3000)
                    }
                }
            } else {
                Log.e("RAWG", "Upcoming API Error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("RAWG", "Network Error in fetchUpcomingGames: ${e.localizedMessage}")
        }
    }

    private suspend fun fetchRandomGames() = withContext(Dispatchers.IO) {
        try {
            val randomPage = (1..50).random()
            val response = api.getRandom(apiKey, page = randomPage, pageSize = 8)
            if (response.isSuccessful) {
                val games = response.body()?.results ?: emptyList()
                val sortedGames = games.sortedBy { it.released }
                val filteredGames = sortedGames.filter { game ->
                    game.esrb_rating?.slug?.lowercase() != "ao"
                }
                randomGamesList.clear()
                randomGamesList.addAll(filteredGames)

                fetchDeveloperNames(randomGamesList, 10)

                withContext(Dispatchers.Main) {
                    gameBlockAdapter.notifyDataSetChanged()
                }
            } else {
                Log.e("RAWG", "Random API Error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("RAWG", "Network Error in fetchRandomGames: ${e.localizedMessage}")
        }
    }

    private suspend fun fetchDeveloperNames(games: MutableList<Game>, maxText: Int) = withContext(Dispatchers.IO) {
        try {
            val iterator = games.iterator()
            while (iterator.hasNext()) {
                val game = iterator.next()
                val response = api.getGameDetail(game.id, apiKey)

                if (response.isSuccessful) {
                    val gameDetail = response.body()

                    val blockedTags = listOf("nsfw", "hentai", "eroge", "adult", "porn")
                    val isNSFW = gameDetail?.tags?.any { tag ->
                        blockedTags.any { blocked -> tag.name.contains(blocked, ignoreCase = true) }
                    } == true

                    if (isNSFW) {
                        iterator.remove() // Remove the game from the list
                        continue
                    }

                    val devs = gameDetail?.developers
                    val devName = devs?.joinToString(", ") { it.name } ?: "Unknown Developer"
                    game.developerName = if (devName.length > maxText) devName.take(maxText - 3) + "..." else devName
                } else {
                    game.developerName = "Unknown Developer"
                }
            }

            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
                upcomingAdapter.notifyDataSetChanged()
                gameBlockAdapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            Log.e("RAWG", "Network Error in fetchDeveloperNames: ${e.localizedMessage}")
        }
    }

    private fun setIndicator(dots: ArrayList<TextView>, container: ViewGroup, list: List<Game>, currentIndex: Int) {
        dots.clear()
        container.removeAllViews()
        val count = list.size
        for (i in 0 until count) {
            val dot = TextView(requireContext())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                dot.text = Html.fromHtml("&#8226;", Html.FROM_HTML_MODE_LEGACY)
            } else {
                dot.text = Html.fromHtml("&#8226;")
            }
            dot.textSize = 35f
            dot.setTextColor(ContextCompat.getColor(requireContext(), R.color.snow))
            container.addView(dot)
            dots.add(dot)
        }
    }

    private fun updateCurrentDot(dots: ArrayList<TextView>, index: Int) {
        for (i in dots.indices) {
            dots[i].setTextColor(
                if (i == index) ContextCompat.getColor(requireContext(), R.color.tiffanyblue)
                else ContextCompat.getColor(requireContext(), R.color.snow)
            )
        }
    }

    private fun createGameClickListener(repository: GameRepository) = object : GameSliderAdapter.OnClickListener {
        override fun onClick(position: Int, model: Game) {
            lifecycleScope.launch {
                val gameDetail = repository.fetchFullGameDetail(model.id)
                if (gameDetail != null) {
                    val fragment = DetailGameScreen()
                    val bundle = Bundle().apply {
                        putParcelable(NEXT_SCREEN, gameDetail)
                    }
                    fragment.arguments = bundle
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                } else {
                    Log.e("RAWG", "Failed to fetch full game detail for ID: ${model.id}")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        autoScrollHandlerMain.postDelayed(autoScrollRunnableMain, 3000)
        autoScrollHandlerUpcoming.postDelayed(autoScrollRunnableUpcoming, 3000)
    }

    override fun onPause() {
        super.onPause()
        autoScrollHandlerMain.removeCallbacks(autoScrollRunnableMain)
        autoScrollHandlerUpcoming.removeCallbacks(autoScrollRunnableUpcoming)
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"
        const val NEXT_SCREEN = "NEXT_SCREEN"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Home().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
