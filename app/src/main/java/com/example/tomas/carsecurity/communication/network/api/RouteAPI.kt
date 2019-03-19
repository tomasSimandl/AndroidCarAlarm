package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Retrofit API for outgoing network requests related to Route object.
 */
interface RouteAPI {

    /**
     * Prepare request to server to create new route.
     *
     * @param carId id of car which creates the route.
     * @param routeStartTime is time when the route started.
     */
    @FormUrlEncoded
    @POST(Mapping.ROUTE_URL)
    fun createRoute(
            @Field("car_id") carId: Long,
            @Field("time") routeStartTime: Long
    ): Call<Any>
}