package com.example.lecproject

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RAWGResponse(
    val count: Int,
    val results: List<Game> = emptyList()
) : Parcelable


data class User(val username: String, val password: String)
const val PREFS_NAME = "user_prefs"
const val USERS_KEY = "users"

@Parcelize
data class ScreenshotResponse(
    val results: List<Screenshot> = emptyList()
) : Parcelable

@Parcelize
data class MovieResponse(
    val results: List<Movie> = emptyList()
) : Parcelable

@Parcelize
data class ThumbnailItem(
    val url: String,
    val isVideo: Boolean,
    val videoUrl: String? = null
) : Parcelable


@Parcelize
data class GameDetail(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String,
    val released: String? = null,
    val added: Int = 0,
    val tba: Boolean = false,
    val playtime: Int = 0,
    val platforms: List<PlatformWrapper> = emptyList(),
    val developers: List<Developer> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val rating: Float = 0f,
    val ratings: List<Rating> = emptyList(),
    val screenshots: List<Screenshot> = emptyList(),
    val movies: List<Movie> = emptyList(),
) : Parcelable

fun GameDetail.toThumbnailList(): List<ThumbnailItem> {
    val screenshotItems = screenshots.map {
        ThumbnailItem(
            url = it.image,
            isVideo = false
        )
    }

    val movieItems = movies.mapNotNull {
        if (it.preview != null && it.data?.max != null) {
            ThumbnailItem(
                url = it.preview,
                isVideo = true,
                videoUrl = it.data.max
            )
        } else null
    }
    return screenshotItems + movieItems
}

@Parcelize
data class Game(
    val id: Int,
    val name: String,
    val slug: String,
    val released: String? = null,
    val background_image: String? = null,
    val rating: Float = 0f,
    val rating_top: Int = 0,
    val ratings_count: Int = 0,
    val added: Int = 0,
    val esrb_rating: EsrbRating? = null,
    var developerName: String? = null
) : Parcelable

@Parcelize
data class Screenshot(
    val id: Int,
    val image: String
) : Parcelable

@Parcelize
data class Movie(
    val id: Int,
    val name: String,
    val preview: String,
    val data: MovieData
) : Parcelable

@Parcelize
data class MovieData(
    val max: String,
    val `480`: String
) : Parcelable

@Parcelize
data class Developer(
    val id: Int,
    val name: String,
    val slug: String
) : Parcelable

@Parcelize
data class Rating(
    val id: Int,
    val title: String,
    val count: Int,
    val percent: Float
) : Parcelable

@Parcelize
data class Genre(
    val id: Int,
    val name: String,
    val slug: String
) : Parcelable

@Parcelize
data class PlatformWrapper(
    val platform: Platform,
    val requirements: Requirements? = null
) : Parcelable

@Parcelize
data class Platform(
    val id: Int,
    val name: String,
    val slug: String
) : Parcelable

@Parcelize
data class Requirements(
    val minimum: String,
    val recommended: String
) : Parcelable

@Parcelize
data class Publisher(
    val id: Int,
    val name: String,
    val slug: String
) : Parcelable

@Parcelize
data class Tag(
    val id: Int,
    val name: String,
    val slug: String
) : Parcelable

@Parcelize
data class TagResponse(
    val results: List<Tag>
) : Parcelable

@Parcelize
data class EsrbRating(
    val id: Int,
    val name: String,
    val slug: String
) : Parcelable

@Parcelize
data class Clip(
    val clip: String,
    val preview: String
) : Parcelable
