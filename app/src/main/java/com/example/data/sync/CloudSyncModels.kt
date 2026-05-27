package com.example.data.sync

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CloudExpenseDto(
    val description: String,
    val amount: Double,
    val categoryName: String,
    val dateMillis: Long
)

@JsonClass(generateAdapter = true)
data class AuthRequest(
    val email: String? = null,
    val phoneNumber: String? = null,
    val idToken: String? = null,
    val name: String? = null,
    val provider: String // "google" or "phone"
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val success: Boolean,
    val token: String?,
    val userId: String?,
    val name: String?,
    val email: String?,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class SyncPayload(
    val userId: String,
    val expenses: List<CloudExpenseDto>
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    val success: Boolean,
    val count: Int,
    val expenses: List<CloudExpenseDto>?,
    val message: String?
)
