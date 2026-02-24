package com.simats.dialdish.network

data class SignupRequest(
    val full_name: String,
    val phone: String,
    val email: String,
    val password: String,
    val role: String
)

data class SignupResponse(
    val status: String,
    val message: String
)

data class UserSignupRequest(
    val fullName: String, val email: String, val phone: String,
    val passwordHash: String, val role: String, val profileImageBase64: String?
)

data class OwnerStallRequest(
    val fullName: String, val email: String, val phone: String,
    val passwordHash: String, val role: String,
    val stallName: String, val fssaiNumber: String, val stallImageBase64: String?
)

data class ApiResponse(val status: String, val message: String, val isVerified: Boolean? = null)

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val status: String,
    val message: String,
    val userId: String?,
    val name: String?,
    val role: String? // "Owner", "User", or "Delivery"
)

data class AddStaffRequest(
    val owner_id: Int,
    val name: String,
    val phone: String,
    val email: String,
    val aadhar: String,
    val prefix: String,
    val photoBase64: String?
)

data class AddStaffResponse(
    val status: String,
    val message: String,
    val generated_id: String? = null,
    val prefix_used: String? = null
)

// Request: We send the Owner ID
data class FetchStaffRequest(
    val owner_id: Int
)

// The individual Staff object we get back
data class StaffMember(
    val name: String,
    val staff_id: String
)

// Response: The full package from Python
data class FetchStaffResponse(
    val status: String,
    val message: String? = null,
    val staff: List<StaffMember>? = null
)