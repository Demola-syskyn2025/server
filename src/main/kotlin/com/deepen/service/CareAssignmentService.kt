package com.deepen.service

import com.deepen.dto.CareAssignmentDto
import com.deepen.dto.CreateCareAssignmentRequest
import com.deepen.model.CareAssignment
import com.deepen.model.UserRole
import com.deepen.repository.CareAssignmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CareAssignmentService(
    private val careAssignmentRepository: CareAssignmentRepository,
    private val userService: UserService
) {

    fun findByPatientId(patientId: Long): List<CareAssignment> =
        careAssignmentRepository.findByPatientId(patientId)

    fun findByStaffId(staffId: Long): List<CareAssignment> =
        careAssignmentRepository.findByStaffId(staffId)

    fun isStaffAssignedToPatient(staffId: Long, patientId: Long): Boolean =
        careAssignmentRepository.existsByPatientIdAndStaffId(patientId, staffId)

    @Transactional
    fun createAssignment(request: CreateCareAssignmentRequest): CareAssignment {
        val patient = userService.findById(request.patientId)
            ?: throw IllegalArgumentException("Patient not found")
        val staff = userService.findById(request.staffId)
            ?: throw IllegalArgumentException("Staff not found")

        if (patient.role != UserRole.PATIENT) {
            throw IllegalArgumentException("User ${request.patientId} is not a patient")
        }
        if (staff.role != UserRole.DOCTOR && staff.role != UserRole.NURSE) {
            throw IllegalArgumentException("User ${request.staffId} is not a doctor or nurse")
        }
        if (careAssignmentRepository.existsByPatientIdAndStaffId(request.patientId, request.staffId)) {
            throw IllegalArgumentException("Assignment already exists")
        }

        return careAssignmentRepository.save(
            CareAssignment(
                patient = patient,
                staff = staff,
                isPrimary = request.isPrimary
            )
        )
    }

    @Transactional
    fun removeAssignment(id: Long) {
        if (!careAssignmentRepository.existsById(id)) {
            throw IllegalArgumentException("Assignment not found")
        }
        careAssignmentRepository.deleteById(id)
    }

    fun toDto(assignment: CareAssignment): CareAssignmentDto = CareAssignmentDto(
        id = assignment.id,
        patient = userService.toDto(assignment.patient),
        staff = userService.toDto(assignment.staff),
        isPrimary = assignment.isPrimary,
        createdAt = assignment.createdAt
    )
}
