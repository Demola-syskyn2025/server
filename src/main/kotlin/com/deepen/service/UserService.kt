package com.deepen.service

import com.deepen.dto.RegisterRequest
import com.deepen.dto.UserDto
import com.deepen.model.User
import com.deepen.model.UserRole
import com.deepen.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    
    fun findByEmail(email: String): User? = userRepository.findByEmail(email).orElse(null)
    
    fun findById(id: Long): User? = userRepository.findById(id).orElse(null)
    
    fun existsByEmail(email: String): Boolean = userRepository.existsByEmail(email)
    
    @Transactional
    fun createUser(request: RegisterRequest): User {
        if (existsByEmail(request.email)) {
            throw IllegalArgumentException("User with email ${request.email} already exists")
        }
        
        val user = User(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            firstName = request.firstName,
            lastName = request.lastName,
            phoneNumber = request.phoneNumber,
            role = request.role
        )
        
        return userRepository.save(user)
    }
    
    fun findStaff(): List<User> = userRepository.findByRoleIn(listOf(UserRole.DOCTOR, UserRole.NURSE))
    
    fun findPatients(): List<User> = userRepository.findByRole(UserRole.PATIENT)
    
    fun toDto(user: User): UserDto = UserDto(
        id = user.id,
        email = user.email,
        firstName = user.firstName,
        lastName = user.lastName,
        phoneNumber = user.phoneNumber,
        role = user.role
    )
}
