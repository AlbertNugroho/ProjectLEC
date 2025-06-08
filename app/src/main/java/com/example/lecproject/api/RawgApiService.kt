import com.example.lecproject.GameDetail
import com.example.lecproject.MovieResponse
import com.example.lecproject.RAWGResponse
import com.example.lecproject.ScreenshotResponse
import com.example.lecproject.TagResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RawgApiService {
    @GET("games")
    suspend fun getGames(
        @Query("key") apiKey: String,
        @Query("ordering") ordering: String = "-added",
        @Query("dates") dates: String,
        @Query("exclude_additions") excludeAdditions: Boolean = true,
        @Query("page_size") pageSize: Int = 5
    ): Response<RAWGResponse>
    @GET("games")
    suspend fun getSearchGames(
        @Query("key") apiKey: String,
        @Query("ordering") ordering: String = "-added",
        @Query("page") page: Int,
        @Query("search") search: String? = null,
        @Query("page_size") pageSize: Int = 40,
        @Query("exclude_additions") excludeAdditions: Boolean = true,
    ): Response<RAWGResponse>

    @GET("games")
    suspend fun getRandom(
        @Query("key") apiKey: String,
        @Query("ordering") ordering: String = "-rating",
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int = 5,
        @Query("exclude_additions") excludeAdditions: Boolean = true,
    ): Response<RAWGResponse>

    @GET("games")
    suspend fun getUpcoming(
        @Query("key") apiKey: String,
        @Query("ordering") ordering: String = "-released",
        @Query("dates") dates: String,
        @Query("exclude_additions") excludeAdditions: Boolean = true,
        @Query("page_size") pageSize: Int = 5
    ): Response<RAWGResponse>

    @GET("games/{id}")
    suspend fun getGameDetail(
        @Path("id") id: Int,
        @Query("key") apiKey: String
    ): Response<GameDetail>

    @GET("games/{id}/screenshots")
    suspend fun getScreenshots(
        @Path("id") id: Int,
        @Query("key") apiKey: String
    ): Response<ScreenshotResponse>

    @GET("games/{id}/movies")
    suspend fun getMovies(
        @Path("id") id: Int,
        @Query("key") apiKey: String
    ): Response<MovieResponse>
    @GET("tags")
    suspend fun getTags(
        @Query("key") apiKey: String,
        @Query("page_size") pageSize: Int = 40
    ): Response<TagResponse>
}
