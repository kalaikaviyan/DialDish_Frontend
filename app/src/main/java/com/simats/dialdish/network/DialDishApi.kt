package com.simats.dialdish.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DialDishApi {

    @POST("login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    // FIXED: Changed back to UserSignupRequest and ApiResponse to perfectly match your SignupActivity!
    @POST("signup")
    suspend fun registerUser(@Body request: UserSignupRequest): Response<ApiResponse>

    @POST("register_stall")
    suspend fun registerStall(@Body request: OwnerStallRequest): Response<ApiResponse>

    @POST("add_delivery_man")
    suspend fun addDeliveryMan(@Body request: AddStaffRequest): Response<AddStaffResponse>

    @POST("fetch_free_staff")
    suspend fun getFreeStaff(@Body request: FetchStaffRequest): Response<FetchStaffResponse>
}