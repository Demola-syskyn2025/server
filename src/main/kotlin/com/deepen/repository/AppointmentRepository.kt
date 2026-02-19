package com.deepen.repository



import com.deepen.model.Appointment

import com.deepen.model.AppointmentStatus

import org.springframework.data.jpa.repository.JpaRepository

import org.springframework.data.jpa.repository.Query

import org.springframework.stereotype.Repository

import java.time.LocalDateTime



@Repository

interface AppointmentRepository : JpaRepository<Appointment, Long> {

    fun findByPatientId(patientId: Long): List<Appointment>

    fun findByStaffId(staffId: Long): List<Appointment>

    fun findByStatus(status: AppointmentStatus): List<Appointment>

    

    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId AND a.scheduledAt >= :startDate AND a.scheduledAt <= :endDate")

    fun findByPatientIdAndDateRange(patientId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<Appointment>

    

    @Query("SELECT a FROM Appointment a WHERE a.staff.id = :staffId AND a.scheduledAt >= :startDate AND a.scheduledAt <= :endDate")

    fun findByStaffIdAndDateRange(staffId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<Appointment>

    

    @Query("SELECT a FROM Appointment a WHERE a.scheduledAt >= :startDate AND a.scheduledAt <= :endDate")
    fun findByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<Appointment>

    fun findByPlanId(planId: Long): List<Appointment>

    @Query("SELECT a FROM Appointment a WHERE a.plan.id = :planId AND a.staff.id = :staffId")
    fun findByPlanIdAndStaffId(planId: Long, staffId: Long): List<Appointment>
}

