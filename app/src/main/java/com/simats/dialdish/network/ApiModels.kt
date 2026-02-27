package com.simats.dialdish.network

data class SignupRequest(val full_name: String, val phone: String, val email: String, val password: String, val role: String)
data class SignupResponse(val status: String, val message: String)
data class UserSignupRequest(val fullName: String, val email: String, val phone: String, val passwordHash: String, val role: String, val profileImageBase64: String?)
data class OwnerStallRequest(val fullName: String, val email: String, val phone: String, val passwordHash: String, val role: String, val stallName: String, val fssaiNumber: String, val stallImageBase64: String?, val latitude: String?, val longitude: String?)
data class ApiResponse(val status: String, val message: String, val isVerified: Boolean? = null)
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val status: String, val message: String, val userId: String?, val name: String?, val role: String?, val stall_id: Int?, val staff_id: Int?, val is_open: Boolean?)
data class AddStaffRequest(val owner_id: Int, val name: String, val phone: String, val email: String, val aadhar: String, val prefix: String, val photoBase64: String?)
data class AddStaffResponse(val status: String, val message: String, val generated_id: String? = null, val prefix_used: String? = null)
data class FetchStaffRequest(val owner_id: Int)
data class StaffMember(val name: String, val staff_id: String)
data class FetchStaffResponse(val status: String, val message: String? = null, val staff: List<StaffMember>? = null)
data class CheckPrefixRequest(val owner_id: Int)
data class CheckPrefixResponse(val status: String, val has_prefix: Boolean, val prefix: String?)
data class Stall(val stall_id: Int, val stall_name: String, val tags: String, val rating: String, val is_open: Boolean, val latitude: String?, val longitude: String?, val contact_phone: String?)
data class FetchStallsResponse(val status: String, val stalls: List<Stall>)

// --- MENU & REVIEWS ---
data class FetchMenuRequest(val stall_id: Int, val user_id: String?)
data class MenuResponse(
    val status: String,
    val stall_data: StallInfo,
    val current_meal_type: String,
    val transition_message: String?,
    val is_locked: Boolean, // <--- ADD THIS LINE
    val highlight_item: MenuHighlight?,
    val menu: List<MenuItem>,
    val reviews: List<ReviewItem>
)

// FIXED: The line break is restored here so Android Studio can read it!
data class StallInfo(val name: String, val rating: String, val is_open: Boolean, val image_url: String?, val is_favorite: Boolean, val contact_phone: String?)
data class MenuHighlight(val name: String, val price: String, val image_url: String?)
data class MenuItem(val item_id: Int, val name: String, val price: String, val desc: String?, val is_veg: Boolean, val in_stock: Boolean, val image_url: String?)
data class ReviewItem(val user: String, val rating: Double, val comment: String, val date: String?,val review_id: Int)

// --- FAVORITES ---
data class ToggleFavRequest(val user_id: String, val stall_id: Int)
data class FavResponse(val status: String)
data class GetFavRequest(val user_id: String)
data class GetFavResponse(val status: String, val favorites: List<Int>)

// --- ORDER & REVIEWS ---
data class AddReviewRequest(val stall_id: Int, val user_id: String, val user_name: String, val rating: Float, val comment: String)
data class PlaceOrderRequest(val user_id: String, val stall_id: Int, val items_summary: String, val total_amount: Double, val delivery_address: String, val delivery_lat: String?, val delivery_lng: String?, val special_request: String, val order_type: String)
data class PlaceOrderResponse(val status: String, val message: String, val order_id: Int?)

// --- OWNER API ---
data class StoreStatusRequest(val stall_id: Int, val is_open: Boolean)
data class GetTimingsRequest(val stall_id: Int)
data class TimingsResponse(val status: String, val shop_open_time: String?, val delivery_time: String?, val breakfast_time: String?, val lunch_time: String?, val evening_time: String?, val dinner_time: String?)
data class UpdateTimingsRequest(val stall_id: Int, val shop_open_time: String, val delivery_time: String, val breakfast_time: String, val lunch_time: String, val evening_time: String, val dinner_time: String)
data class AddMenuItemRequest(val stall_id: Int, val name: String, val price: String, val type: String, val portion: String, val is_special: Boolean, val in_stock: Boolean, val image_base64: String?)
data class FetchOwnerMenuRequest(val stall_id: Int)
data class OwnerMenuItem(val id: Int, val name: String, val price: String, val type: String, val portion: String, val is_special: Boolean, val in_stock: Boolean, val image_url: String?)
data class FetchOwnerMenuResponse(val status: String, val menu: List<OwnerMenuItem>)
data class DeleteMenuItemRequest(val item_id: Int)
data class EditMenuItemRequest(val item_id: Int, val name: String, val price: String, val type: String, val portion: String, val is_special: Boolean, val in_stock: Boolean, val image_base64: String?)

data class EditReviewRequest(val review_id: Int, val user_name: String, val rating: Float, val comment: String)

data class DeleteReviewRequest(val review_id: Int, val user_name: String)
// --- PHASE 1: OWNER LIVE ORDERS ---
data class FetchOwnerOrdersRequest(val stall_id: Int)
data class OwnerOrder(
    val order_id: Int, val customer_name: String, val customer_phone: String,
    val items_summary: String, val total_amount: Double, val delivery_address: String,
    val special_request: String?, val order_type: String, val status: String,
    val payment_status: String?, val payment_method: String?, val verification_code: String?,
    val staff_name: String?, val staff_phone: String?,
    val created_at: String?

)

data class RejectOrderRequest(val order_id: Int)
data class DispatchOrderRequest(val order_id: Int)
data class FetchOwnerOrdersResponse(
    val status: String,
    val orders: List<OwnerOrder>,
    val is_account_locked: Boolean? = false,
    val refund_owed: Double? = 0.0
)
data class ApproveOrderRequest(val order_id: Int, val staff_id_code: String, val is_paid: Boolean)
data class ApproveOrderResponse(val status: String, val message: String, val verification_code: String?)
// --- DASHBOARD STATS ---
data class DashboardStatsRequest(val stall_id: Int)
data class DashboardStatsResponse(val status: String, val approved_today: Int, val rejected_today: Int)
data class ToggleStockRequest(val item_id: Int, val in_stock: Boolean)
// --- PHASE 2: USER ORDERS ---
data class FetchUserOrdersRequest(val user_id: Int)

data class UserOrder(
    val order_id: Int,
    val stall_name: String,
    val items_summary: String,
    val total_amount: Double,
    val status: String,
    val created_at: String,
    val verification_code: String?,
    val delivery_man_name: String?,
    val delivery_man_phone: String?,
    val delivery_man_photo: String?,
    val payment_method: String?,
    val payment_status: String?
)

data class FetchUserOrdersResponse(val status: String, val orders: List<UserOrder>)
// --- PHASE 2: HISTORY & SOFT DELETE ---
data class FetchHistoryRequest(val stall_id: Int, val filter_date: String?)
data class HideUserOrderRequest(val order_id: Int)
// --- PHASE 3: DELIVERY & TRACKING ---
data class LocationUpdateRequest(val staff_id: Int, val lat: Double, val lng: Double)
data class GetTaskRequest(val staff_id: Int)
data class DeliveryTaskResponse(val status: String, val order_id: Int?, val customer_name: String?, val customer_phone: String?, val delivery_address: String?, val items_summary: String?, val total_amount: Double?, val payment_status: String?, val order_status: String?, val stall_name: String?, val stall_lat: String?, val stall_lng: String?, val return_reason: String?)
data class VerifyCodeRequest(val order_id: Int, val code: String)
data class TrackingRequest(val order_id: Int)
data class TrackingResponse(val status: String, val status_text: String?, val driver_lat: String?, val driver_lng: String?, val user_lat: String?, val user_lng: String?, val stall_lat: String?, val stall_lng: String?)
// Add to the bottom of ApiModels.kt
data class CheckPinResponse(val status: String, val payment_status: String?, val message: String?)
data class ConfirmDeliveryRequest(val order_id: Int)
data class GetActiveCashRequest(val staff_id: Int)
data class OrderLogItem(val order_id: Int, val amount: Double, val method: String, val date: String, val is_settled: Boolean)
data class ActiveCashResponse(val status: String, val message: String?, val active_cash: Double?, val total_deliveries: Int?, val log: List<OrderLogItem>?)

data class FetchAllStaffBalancesRequest(val owner_id: Int)
data class StaffBalanceItem(val id: Int, val staff_id_code: String, val name: String, val phone: String, val email: String, val active_cash: Double)
data class FetchAllStaffBalancesResponse(val status: String, val message: String?, val staff: List<StaffBalanceItem>?)

data class SettleCashRequest(val owner_id: Int, val staff_id: Int, val expense_reason: String, val expense_amount: Double)
// Add to the bottom of ApiModels.kt
data class EditStaffRequest(val staff_id: Int, val name: String, val phone: String, val email: String, val photoBase64: String?)
data class DeleteStaffRequest(val staff_id: Int)
// Add to the bottom of ApiModels.kt
data class GetProfileRequest(val user_id: Int)
data class GetProfileResponse(val status: String, val full_name: String?, val phone: String?, val email: String?, val profile_image_url: String?, val message: String?)
data class UpdateProfileRequest(val user_id: Int, val full_name: String, val phone: String, val email: String, val profile_image_base64: String?)
data class ChangePasswordRequest(val user_id: Int, val current_password: String, val new_password: String)
// --- ADDRESS MANAGEMENT ---
data class UserIdRequest(val user_id: Int)
data class AddressItem(val id: Int, val title: String, val address_text: String, val latitude: String?, val longitude: String?, val is_default: Boolean)
data class GetAddressesResponse(val status: String, val addresses: List<AddressItem>)
data class AddAddressRequest(val user_id: Int, val title: String, val address_text: String, val latitude: String?, val longitude: String?)
data class SetDefaultAddressRequest(val user_id: Int, val address_id: Int)
data class DeleteAddressRequest(val address_id: Int)
data class VerifyPasswordRequest(val user_id: Int, val current_password: String)
// --- OWNER PROFILE ---
data class OwnerIdRequest(val owner_id: Int)
data class OwnerProfileResponse(
    val status: String, val full_name: String?, val phone: String?, val email: String?,
    val owner_image_url: String?, val stall_name: String?, val stall_prefix: String?,
    val fssai: String?, val location: String?, val stall_image_url: String?
)
data class UpdateOwnerProfileRequest(
    val owner_id: Int, val full_name: String,
    val owner_image_base64: String?, val stall_image_base64: String?
)
// --- DELIVERY PROFILE ---
data class DeliveryProfileRequest(val staff_id: Int)
data class DeliveryProfileResponse(
    val status: String,
    val full_name: String?,
    val staff_id_code: String?,
    val phone: String?,
    val aadhar_number: String?,
    val profile_image_url: String?
)
// --- PHASE 4: CANCELLATION ---
data class CancelOrderRequest(val order_id: Int, val user_id: Int)
data class CancelOrderResponse(val status: String, val message: String?, val refund_amount: Double?)
// --- PHASE 4: CLEAR REFUND ---
data class ClearRefundRequest(val stall_id: Int, val utr_number: String)
data class ClearRefundResponse(val status: String, val message: String?)
data class ConfirmReturnRequest(val staff_id: Int)
data class AiContextResponse(val status: String, val context_string: String?)