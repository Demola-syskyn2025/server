package com.deepen.controller

import com.deepen.dto.AuthResponse
import com.deepen.dto.LoginRequest
import com.deepen.dto.RegisterRequest
import com.deepen.security.JwtUtil
import com.deepen.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
    private val jwtUtil: JwtUtil,
    private val passwordEncoder: PasswordEncoder
) {
    
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val user = userService.createUser(request)
        val token = jwtUtil.generateToken(user.email, user.role.name)
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
            AuthResponse(token = token, user = userService.toDto(user))
        )
    }
    
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val user = userService.findByEmail(request.email)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        
        if (!passwordEncoder.matches(request.password, user.password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        
        val token = jwtUtil.generateToken(user.email, user.role.name)
        
        return ResponseEntity.ok(
            AuthResponse(token = token, user = userService.toDto(user))
        )
    }
}
