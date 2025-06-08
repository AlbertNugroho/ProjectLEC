import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RawgApiClient {
    private const val BASE_URL = "https://api.rawg.io/api/"

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

}
