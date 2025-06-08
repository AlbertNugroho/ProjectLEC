import android.util.Log
import com.example.lecproject.GameDetail
import com.example.lecproject.RAWGResponse
import com.example.lecproject.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository(
    private val api: RawgApiService,
    private val apiKey: String
) {

    suspend fun fetchFullGameDetail(id: Int): GameDetail? = withContext(Dispatchers.IO) {
        try {
            val gameDetailResponse = api.getGameDetail(id, apiKey)
            if (!gameDetailResponse.isSuccessful) {
                Log.e("GameRepository", "Failed to fetch game detail: ${gameDetailResponse.code()}")
                return@withContext null
            }
            val gameDetail = gameDetailResponse.body() ?: return@withContext null

            val screenshotsResponse = api.getScreenshots(id, apiKey)
            val screenshots = if (screenshotsResponse.isSuccessful) {
                screenshotsResponse.body()?.results ?: emptyList()
            } else emptyList()

            val moviesResponse = api.getMovies(id, apiKey)
            val movies = if (moviesResponse.isSuccessful) {
                moviesResponse.body()?.results ?: emptyList()
            } else emptyList()

            Log.d("GameRepository", "Successfully fetched full game detail for ID: $id")

            // Return new GameDetail instance with screenshots and movies set
            gameDetail.copy(
                screenshots = screenshots,
                movies = movies
            )
        } catch (e: Exception) {
            Log.e("GameRepository", "Error fetching full game detail for ID $id", e)
            null
        }
    }
}
