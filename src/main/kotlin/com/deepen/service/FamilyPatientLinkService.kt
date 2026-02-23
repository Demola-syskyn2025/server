package com.deepen.service

import com.deepen.dto.CreateFamilyPatientLinkRequest
import com.deepen.dto.FamilyPatientLinkDto
import com.deepen.model.FamilyPatientLink
import com.deepen.repository.FamilyPatientLinkRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FamilyPatientLinkService(
    private val familyPatientLinkRepository: FamilyPatientLinkRepository,
    private val userService: UserService
) {

    fun findByFamilyMemberId(familyMemberId: Long): List<FamilyPatientLink> =
        familyPatientLinkRepository.findByFamilyMemberId(familyMemberId)

    fun findByPatientId(patientId: Long): List<FamilyPatientLink> =
        familyPatientLinkRepository.findByPatientId(patientId)

    @Transactional
    fun createLink(familyMemberId: Long, request: CreateFamilyPatientLinkRequest): FamilyPatientLink {
        val familyMember = userService.findById(familyMemberId)
            ?: throw IllegalArgumentException("Family member not found")
        val patient = userService.findById(request.patientId)
            ?: throw IllegalArgumentException("Patient not found")

        val existing = familyPatientLinkRepository.findByFamilyMemberIdAndPatientId(familyMemberId, request.patientId)
        if (existing != null) {
            throw IllegalArgumentException("Link already exists")
        }

        return familyPatientLinkRepository.save(
            FamilyPatientLink(
                familyMember = familyMember,
                patient = patient,
                relationship = request.relationship
            )
        )
    }

    @Transactional
    fun removeLink(id: Long) {
        familyPatientLinkRepository.deleteById(id)
    }

    fun toDto(link: FamilyPatientLink): FamilyPatientLinkDto = FamilyPatientLinkDto(
        id = link.id,
        familyMember = userService.toDto(link.familyMember),
        patient = userService.toDto(link.patient),
        relationship = link.relationship,
        createdAt = link.createdAt
    )
}
