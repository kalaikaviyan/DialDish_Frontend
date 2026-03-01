package com.simats.directdine.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET

interface directdineApi {

    @POST("api/login")
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

    @POST("/check_prefix")
    suspend fun checkPrefix(@Body request: CheckPrefixRequest): retrofit2.Response<CheckPrefixResponse>

    @GET("api/user/stalls")
    suspend fun fetchStalls(): Response<FetchStallsResponse>

    // --- ADD TO directdineApi interface ---

    @POST("api/user/stall_menu")
    suspend fun getStallMenu(@Body request: FetchMenuRequest): Response<MenuResponse>

    @POST("api/user/toggle_favorite")
    suspend fun toggleFavorite(@Body request: ToggleFavRequest): Response<FavResponse>

    @POST("api/user/get_favorites")
    suspend fun getFavorites(@Body request: GetFavRequest): Response<GetFavResponse>

    @POST("api/user/add_review")
    suspend fun addReview(@Body request: AddReviewRequest): Response<ApiResponse>

    @POST("api/user/place_order")
    suspend fun placeOrder(@Body request: PlaceOrderRequest): Response<PlaceOrderResponse>

    @POST("api/owner/update_status")
    suspend fun updateStoreStatus(@Body request: StoreStatusRequest): retrofit2.Response<ApiResponse>

    @POST("api/owner/get_timings")
    suspend fun getTimings(@Body request: GetTimingsRequest): retrofit2.Response<TimingsResponse>

    @POST("api/owner/update_timings")
    suspend fun updateTimings(@Body request: UpdateTimingsRequest): retrofit2.Response<ApiResponse>

    @POST("api/owner/add_menu_item")
    suspend fun addMenuItem(@Body request: AddMenuItemRequest): retrofit2.Response<ApiResponse>

    @POST("api/owner/get_menu")
    suspend fun getOwnerMenu(@Body request: FetchOwnerMenuRequest): retrofit2.Response<FetchOwnerMenuResponse>

    @POST("api/owner/edit_menu_item")
    suspend fun editMenuItem(@Body request: EditMenuItemRequest): retrofit2.Response<ApiResponse>

    @POST("api/owner/delete_menu_item")
    suspend fun deleteMenuItem(@Body request: DeleteMenuItemRequest): retrofit2.Response<ApiResponse>

    @POST("api/user/edit_review")
    suspend fun editReview(@Body request: EditReviewRequest): retrofit2.Response<ApiResponse>

    @POST("api/user/delete_review")
    suspend fun deleteReview(@Body request: DeleteReviewRequest): retrofit2.Response<ApiResponse>
    @POST("api/owner/get_live_orders")
    suspend fun getLiveOrders(@Body request: FetchOwnerOrdersRequest): retrofit2.Response<FetchOwnerOrdersResponse>

    @POST("api/owner/approve_order")
    suspend fun approveOrder(@Body request: ApproveOrderRequest): retrofit2.Response<ApproveOrderResponse>

    @POST("api/owner/get_dashboard_stats")
    suspend fun getDashboardStats(@Body request: DashboardStatsRequest): retrofit2.Response<DashboardStatsResponse>

    @POST("api/owner/reject_order")
    suspend fun rejectOrder(@Body request: RejectOrderRequest): retrofit2.Response<ApiResponse>

    @POST("api/owner/dispatch_order")
    suspend fun dispatchOrder(@Body request: DispatchOrderRequest): retrofit2.Response<ApiResponse>

    @POST("api/owner/toggle_stock")
    suspend fun toggleStock(@Body request: ToggleStockRequest): retrofit2.Response<ApiResponse>

    @POST("api/user/get_my_orders")
    suspend fun getUserOrders(@Body request: FetchUserOrdersRequest): retrofit2.Response<FetchUserOrdersResponse>

    @POST("api/owner/get_history_orders")
    suspend fun getHistoryOrders(@Body request: FetchHistoryRequest): retrofit2.Response<FetchOwnerOrdersResponse>

    @POST("api/user/hide_order")
    suspend fun hideUserOrder(@Body request: HideUserOrderRequest): retrofit2.Response<ApiResponse>

    @POST("api/delivery/update_location")
    suspend fun updateLocation(@Body request: LocationUpdateRequest): retrofit2.Response<ApiResponse>
    @POST("api/delivery/get_task")
    suspend fun getDeliveryTask(@Body request: GetTaskRequest): retrofit2.Response<DeliveryTaskResponse>
    // Replace the old @POST("api/delivery/verify_code") line with these two:
    @POST("api/delivery/check_pin")
    suspend fun checkPin(@Body request: VerifyCodeRequest): retrofit2.Response<CheckPinResponse>

    @POST("api/delivery/complete_delivery")
    suspend fun completeDelivery(@Body request: ConfirmDeliveryRequest): retrofit2.Response<ApiResponse>
    @POST("api/get_tracking_data")
    suspend fun getTrackingData(@Body request: TrackingRequest): retrofit2.Response<TrackingResponse>

    // --- NEW LEDGER APIS ---
    @POST("api/ledger/get_active_cash")
    suspend fun getActiveCash(@Body request: GetActiveCashRequest): retrofit2.Response<ActiveCashResponse>

    @POST("api/ledger/get_all_staff_balances")
    suspend fun getAllStaffBalances(@Body request: FetchAllStaffBalancesRequest): retrofit2.Response<FetchAllStaffBalancesResponse>

    @POST("api/ledger/settle_cash")
    suspend fun settleCash(@Body request: SettleCashRequest): retrofit2.Response<ApiResponse>

    // Add inside your directdineApi interface
    @POST("api/owner/edit_staff")
    suspend fun editStaff(@Body request: EditStaffRequest): retrofit2.Response<ApiResponse>

    @POST("api/owner/delete_staff")
    suspend fun deleteStaff(@Body request: DeleteStaffRequest): retrofit2.Response<ApiResponse>

    // Add inside directdineApi interface
    @POST("api/user/get_profile")
    suspend fun getProfile(@Body request: GetProfileRequest): retrofit2.Response<GetProfileResponse>

    @POST("api/user/update_profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): retrofit2.Response<ApiResponse>

    @POST("api/user/change_password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): retrofit2.Response<ApiResponse>
    // --- ADDRESS APIS ---
    @POST("api/user/get_addresses")
    suspend fun getAddresses(@Body request: UserIdRequest): retrofit2.Response<GetAddressesResponse>

    @POST("api/user/add_address")
    suspend fun addAddress(@Body request: AddAddressRequest): retrofit2.Response<ApiResponse>

    @POST("api/user/set_default_address")
    suspend fun setDefaultAddress(@Body request: SetDefaultAddressRequest): retrofit2.Response<ApiResponse>

    @POST("api/user/delete_address")
    suspend fun deleteAddress(@Body request: DeleteAddressRequest): retrofit2.Response<ApiResponse>

    @POST("api/user/verify_current_password")
    suspend fun verifyCurrentPassword(@Body request: VerifyPasswordRequest): retrofit2.Response<ApiResponse>
    @POST("api/owner/get_profile")
    suspend fun getOwnerProfile(@Body request: OwnerIdRequest): retrofit2.Response<OwnerProfileResponse>

    @POST("api/owner/update_profile")
    suspend fun updateOwnerProfile(@Body request: UpdateOwnerProfileRequest): retrofit2.Response<ApiResponse>

    @POST("api/delivery/get_profile")
    suspend fun getDeliveryProfile(@Body request: DeliveryProfileRequest): retrofit2.Response<DeliveryProfileResponse>

    @POST("api/user/cancel_order")
    suspend fun cancelOrder(@Body request: CancelOrderRequest): retrofit2.Response<CancelOrderResponse>

    @POST("api/owner/clear_refund")
    suspend fun clearRefund(@Body request: ClearRefundRequest): retrofit2.Response<ClearRefundResponse>

    // Add this inside the directdineApi interface
    @POST("api/delivery/unreachable_cancel")
    suspend fun unreachableCancel(@Body request: ConfirmDeliveryRequest): retrofit2.Response<ApiResponse>

    @POST("api/delivery/confirm_return")
    suspend fun confirmReturnToStall(@Body request: ConfirmReturnRequest): retrofit2.Response<ApiResponse>

    // --- Add this ANYWHERE inside directdineApi.kt ---
    @GET("api/user/get_ai_context")
    suspend fun getAiContext(): retrofit2.Response<AiContextResponse>

    @POST("api/owner/toggle_attendance")
    suspend fun toggleAttendance(@Body request: ToggleAttendanceRequest): retrofit2.Response<ToggleAttendanceResponse>

}