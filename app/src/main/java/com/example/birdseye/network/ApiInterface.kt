package com.example.birdseye.network

import com.example.birdseye.model.Result
import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    @GET("maps/api/directions/json")
    fun getDirection(
        @Query("mode") mode: String,
        @Query("transit_routing_preference") preferance: String,
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") key: String
    ): Single<Result>
}