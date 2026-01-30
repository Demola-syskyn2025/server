package com.deepen.dto

import com.deepen.model.UserRole

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String = "",
    val role: UserRole
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val role: UserRole
)
