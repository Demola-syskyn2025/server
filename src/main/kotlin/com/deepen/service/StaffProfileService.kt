package com.deepen.service

import com.deepen.dto.CreateStaffProfileRequest
import com.deepen.dto.StaffProfileDto
import com.deepen.dto.UpdateStaffProfileRequest
import com.deepen.model.StaffProfile
import com.deepen.repository.StaffProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StaffProfileService(
    private val staffProfileRepository: StaffProfileRepository,
    private val userService: UserService
) {

    fun findByUserId(userId: Long): StaffProfile? =
        staffProfileRepository.findByUserId(userId)

    @Transactional
    fun createProfile(userId: Long, request: CreateStaffProfileRequest): StaffProfile {
        val user = userService.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        if (staffProfileRepository.findByUserId(userId) != null) {
            throw IllegalArgumentException("Profile already exists for this user")
        }

        return staffProfileRepository.save(
            StaffProfile(
                user = user,
                department = request.department,
                specialization = request.specialization,
                licenseNumber = request.licenseNumber,
                hireDate = request.hireDate
            )
        )
    }

    @Transactional
    fun updateProfile(userId: Long, request: UpdateStaffProfileRequest): StaffProfile {
        val profile = staffProfileRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("Profile not found")

        val updated = profile.copy(
            department = request.department ?: profile.department,
            specialization = request.specialization ?: profile.specialization,
            licenseNumber = request.licenseNumber ?: profile.licenseNumber,
            hireDate = request.hireDate ?: profile.hireDate
        )

        return staffProfileRepository.save(updated)
    }

    fun toDto(profile: StaffProfile): StaffProfileDto = StaffProfileDto(
        id = profile.id,
        user = userService.toDto(profile.user),
        department = profile.department,
        specialization = profile.specialization,
        licenseNumber = profile.licenseNumber,
        hireDate = profile.hireDate,
        createdAt = profile.createdAt
    )
}
