package com.example.lecproject

import GameRepository
import RawgApiService
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.lecproject.databinding.FragmentSearchBinding
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class Search : Fragment() {

    private lateinit var headerImageView: ImageView
    private lateinit var drawerLayout: androidx.drawerlayout.widget.DrawerLayout
    private val randomGamesList = ArrayList<Game>()
    private val sortedGamesList = ArrayList<Game>()
    private lateinit var autoScrollHandlerMain: Handler
    private lateinit var autoScrollRunnableMain: Runnable
    private lateinit var adapter: SearchRandomAdapter
    private lateinit var sortedAdapter: GameBlockAdapter
    private lateinit var binding: FragmentSearchBinding
    private lateinit var apiKey: String
    private lateinit var api: RawgApiService
    private var currentPage = 1
    private var totalPages = 1
    private var latestSearchQuery: String? = null
    private var currentSortOrder = "-rating,-added"
    private lateinit var gameRepository: GameRepository // Initialize your repo here

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiKey = "1d39e3d160fb4a4bafb73846d473ff9e"
        api = RawgApiClient.retrofit.create(RawgApiService::class.java)
        gameRepository = GameRepository(api, apiKey)
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)
        val navigationView = requireActivity().findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)
        headerImageView = view.findViewById(R.id.menuIcon)
        val sortByRating = headerView.findViewById<RadioButton>(R.id.sortByRating)
        val sortByReleaseDate = headerView.findViewById<RadioButton>(R.id.sortByReleaseDate)
        val sortByName = headerView.findViewById<RadioButton>(R.id.sortByName)
        val sortByUpdated = headerView.findViewById<RadioButton>(R.id.sortByUpdated)
        val radioButtons = listOf(sortByRating, sortByReleaseDate, sortByName, sortByUpdated)

        binding.editTextSearch.addTextChangedListener { editable ->
            val newText = editable?.toString()?.trim()
            if (newText.isNullOrEmpty()) {
                latestSearchQuery = null
                lifecycleScope.launch {
                    fetchGamesPage(1, currentSortOrder)
                }
            }
        }
        binding.editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.editTextSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    latestSearchQuery = query
                    lifecycleScope.launch {
                        searchGamesByQuery(query, 1)
                    }
                }
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.editTextSearch.windowToken, 0)
                true
            } else {
                false
            }
        }

        for (radio in radioButtons) {
            radio.setOnClickListener {
                radioButtons.forEach { it.isChecked = false }
                radio.isChecked = true

                currentSortOrder = when (radio.id) {
                    R.id.sortByRating -> "-rating,-added"
                    R.id.sortByReleaseDate -> "-released"
                    R.id.sortByName -> "name"
                    R.id.sortByUpdated -> "-updated"
                    else -> "-rating,-added"
                }

                lifecycleScope.launch {
                    fetchGamesPage(1, currentSortOrder)
                }
            }
        }

        sortByRating.performClick()

        adapter = SearchRandomAdapter(randomGamesList)
        sortedAdapter = GameBlockAdapter(sortedGamesList)
        binding.carousel.adapter = adapter
        val recyclerView = view.findViewById<RecyclerView>(R.id.searchResultsRecycler)
        recyclerView.layoutManager = NonscrollableGridLayout(requireContext(), 2)
        recyclerView.adapter = sortedAdapter

        headerImageView.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.openDrawer(navigationView)
            }
        }
        lifecycleScope.launch {
            fetchRandomGames()
        }
        adapter.setOnClickListener(createGameClickListener(gameRepository))
        sortedAdapter.setOnClickListener(object : GameBlockAdapter.OnClickListener {
            override fun onClick(position: Int, model: Game) {
                lifecycleScope.launch {
                    val gameDetail = gameRepository.fetchFullGameDetail(model.id)
                    if (gameDetail != null) {
                        val fragment = DetailGameScreen()
                        val bundle = Bundle().apply {
                            putParcelable(Home.NEXT_SCREEN, gameDetail)
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

    private suspend fun searchGamesByQuery(query: String, page: Int = 1) = withContext(Dispatchers.IO) {
        try {
            val response = api.getSearchGames(
                apiKey = apiKey,
                search = query,
                ordering = "-relevance",
                page = page,
                pageSize = 40
            )


            if (response.isSuccessful) {
                val body = response.body()
                val games = response.body()?.results ?: emptyList()
                val totalResults = body?.count ?: 0
                totalPages = (totalResults + 39) / 40

                sortedGamesList.clear()
                sortedGamesList.addAll(games)

                fetchDeveloperNames(sortedGamesList, 10)

                withContext(Dispatchers.Main) {
                    sortedAdapter.notifyDataSetChanged()
                    currentPage = page
                    updatePaginationUI(currentPage, totalPages)
                }
            } else {
                Log.e("RAWG", "Search Error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("RAWG", "Search Failed: ${e.localizedMessage}")
        }
    }

    private suspend fun fetchGamesPage(page: Int, ordering: String) = withContext(Dispatchers.IO) {
        try {
            val response = api.getSearchGames(
                apiKey = apiKey,
                ordering = ordering,
                page = page,
                pageSize = 40,
            )

            if (response.isSuccessful) {
                val body = response.body()
                val games = body?.results ?: emptyList()
                val totalResults = body?.count ?: 0
                totalPages = (totalResults + 39) / 40

                sortedGamesList.clear()
                sortedGamesList.addAll(games)
                fetchDeveloperNames(sortedGamesList, 10)
                withContext(Dispatchers.Main) {
                    sortedAdapter.notifyDataSetChanged()
                    currentPage = page
                    updatePaginationUI(currentPage, totalPages)
                }
            } else {
                Log.e("RAWG", "Page Fetch Failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("RAWG", "Error fetching page: ${e.localizedMessage}")
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
                    adapter.notifyDataSetChanged()
                }
            } else {
                Log.e("RAWG", "Random API Error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("RAWG", "Network Error in fetchRandomGames: ${e.localizedMessage}")
        }
    }

    private fun createGameClickListener(repository: GameRepository) =
        object : SearchRandomAdapter.OnClickListener {
            override fun onClick(position: Int, model: Game) {
                lifecycleScope.launch {
                    val gameDetail = repository.fetchFullGameDetail(model.id)
                    if (gameDetail != null) {
                        val fragment = DetailGameScreen()
                        val bundle = Bundle().apply {
                            putParcelable(Home.NEXT_SCREEN, gameDetail)
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
    private fun updatePaginationUI(currentPage: Int, totalPages: Int) {
        val layout = binding.paginationLayout
        layout.removeAllViews()

        val context = requireContext()
        val maxPagesToShow = 5
        val halfWindow = maxPagesToShow / 2

        val start = (currentPage - halfWindow).coerceAtLeast(1)
        val end = (start + maxPagesToShow - 1).coerceAtMost(totalPages)

        val adjustedStart = (end - maxPagesToShow + 1).coerceAtLeast(1)
        val pageRange = adjustedStart..end

        if (pageRange.first > 1) {
            layout.addView(createPageButton(1))
            layout.addView(createEllipsisView())
        }

        for (page in pageRange) {
            layout.addView(createPageButton(page, isCurrent = page == currentPage))
        }

        if (pageRange.last < totalPages) {
            layout.addView(createEllipsisView())
            layout.addView(createPageButton(totalPages))
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
                sortedAdapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            Log.e("RAWG", "Network Error in fetchDeveloperNames: ${e.localizedMessage}")
        }
    }

    private fun createPageButton(pageNumber: Int, isCurrent: Boolean = false): Button {
        val button = Button(requireContext())
        button.text = pageNumber.toString()
        button.textSize = 15f
        button.setPadding(0, 0, 0, 0) // Remove default padding
        button.setBackgroundColor(android.graphics.Color.TRANSPARENT) // No background
        val rubikBold = ResourcesCompat.getFont(requireContext(), R.font.rubik_bold)
        button.typeface = rubikBold
        button.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isCurrent) R.color.tiffanyblue else R.color.snow
            )
        )
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = 2
        }
        button.layoutParams = params

        button.setOnClickListener {
            lifecycleScope.launch {
                if (latestSearchQuery != null) {
                    searchGamesByQuery(latestSearchQuery!!, pageNumber)
                } else {
                    fetchGamesPage(pageNumber, currentSortOrder)
                }
                currentPage = pageNumber
                binding.searchResultsRecycler.scrollToPosition(0)
            }
        }


        return button
    }


    private fun createEllipsisView(): View {
        val textView = TextView(requireContext())
        textView.text = "..."
        textView.textSize = 15f
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.snow))
        textView.gravity = Gravity.CENTER
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.marginEnd = 2
        textView.layoutParams = params
        return textView
    }


    override fun onResume() {
        super.onResume()
        autoScrollHandlerMain.postDelayed(autoScrollRunnableMain, 3000)
    }

    override fun onPause() {
        super.onPause()
        autoScrollHandlerMain.removeCallbacks(autoScrollRunnableMain)
    }
}