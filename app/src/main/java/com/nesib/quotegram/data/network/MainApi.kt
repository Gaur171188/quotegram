package com.nesib.quotegram.data.network

import com.nesib.quotegram.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface MainApi {
    // Quotes
    @GET("/quotes")
    suspend fun getQuotes(
        @Query("page") page: Int,
        @Query("genres") genres: String? = null,
    ): Response<QuotesResponse>

    @GET("/quotes/{quoteId}")
    suspend fun getSingleQuote(
        @Path("quoteId") quoteId: String
    ): Response<QuoteResponse>

    @GET("/quotes/by-genre")
    suspend fun getQuotesByGenre(
        @Query("genre") genre: String,
        @Query("page") page: Int
    ): Response<QuotesResponse>

    @POST("/quotes")
    suspend fun postQuote(
        @Body quote: Map<String, String>,
    ): Response<QuoteResponse>

    @PATCH("/quotes/{quoteId}")
    suspend fun likeOrDislikeQuote(
        @Path("quoteId") quoteId: String,
        @Body book: Map<String, String>
    ): Response<QuoteResponse>

    @PUT("/quotes/{quoteId}")
    suspend fun updateQuote(
        @Path("quoteId") quoteId: String,
        @Body quote: Map<String, String>
    ): Response<QuoteResponse>

    @DELETE("/quotes/{quoteId}")
    suspend fun deleteQuote(@Path("quoteId") quoteId: String): Response<QuoteResponse>

    // Books
    @GET("/books/")
    suspend fun getBooks(
        @Query("search") searchText: String?,
        @Query("page") page: Int
    ): Response<BooksResponse>

    @GET("/books/discover")
    suspend fun discoverBooks(
        @Query("genre") genre: String,
        @Query("page") page: Int,
        @Query("search") search: String
    ): Response<BooksResponse>

    @GET("/books/{bookId}")
    suspend fun getBook(@Path("bookId") bookId: String): Response<BookResponse>

    @Multipart
    @POST("/books")
    suspend fun postBook(
        @Part("name") name: RequestBody,
        @Part("author") author: RequestBody,
        @Part("genre") genre: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<BookResponse>

    @GET("/books/{bookId}")
    suspend fun getMoreBookQuotes(
        @Path("bookId") bookId: String,
        @Query("page") page: Int
    ): Response<QuotesResponse>

    @POST("/books/{bookToFollowId}/followers")
    suspend fun followOrUnfollowBook(@Path("bookToFollowId") bookId: String): Response<BookResponse>

    @POST("/reports/quote")
    suspend fun reportQuote(@Body reportBody: Map<String, String>): Response<BasicResponse>

    @POST("/reports/user")
    suspend fun reportUser(@Body reportBody: Map<String, String>): Response<BasicResponse>

    @POST("/reports/book")
    suspend fun reportBook(@Body reportBody: Map<String, String>): Response<BasicResponse>

    @GET("/notifications/")
    suspend fun getNotifications(@Query("page") page: Int): Response<NotificationsResponse>

    @DELETE("/notifications/")
    suspend fun clearNotifications(): Response<NotificationsResponse>
}