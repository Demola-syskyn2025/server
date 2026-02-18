package com.deepen.config

import com.deepen.model.*
import com.deepen.repository.*
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Configuration
@Profile("dev", "default")
class DataSeeder {

    @Bean
    fun seedData(
        userRepository: UserRepository,
        appointmentRepository: AppointmentRepository,
        careTaskRepository: CareTaskRepository,
        visitSummaryRepository: VisitSummaryRepository,
        careAssignmentRepository: CareAssignmentRepository,
        staffProfileRepository: StaffProfileRepository,
        staffAvailabilityRepository: StaffAvailabilityRepository,
        timeOffRequestRepository: TimeOffRequestRepository,
        rescheduleRequestRepository: RescheduleRequestRepository,
        passwordEncoder: PasswordEncoder
    ): CommandLineRunner = CommandLineRunner {

        if (userRepository.count() > 0) return@CommandLineRunner

        // --- Users ---
        val doctor = userRepository.save(
            User(
                email = "dr.smith@hospital.fi",
                password = passwordEncoder.encode("password123"),
                firstName = "John",
                lastName = "Smith",
                phoneNumber = "+358401234567",
                role = UserRole.DOCTOR
            )
        )

        val nurse = userRepository.save(
            User(
                email = "nurse.jones@hospital.fi",
                password = passwordEncoder.encode("password123"),
                firstName = "Sarah",
                lastName = "Jones",
                phoneNumber = "+358401234568",
                role = UserRole.NURSE
            )
        )

        val patient1 = userRepository.save(
            User(
                email = "patient1@email.com",
                password = passwordEncoder.encode("password123"),
                firstName = "Matti",
                lastName = "Virtanen",
                phoneNumber = "+358401234569",
                role = UserRole.PATIENT
            )
        )

        val patient2 = userRepository.save(
            User(
                email = "patient2@email.com",
                password = passwordEncoder.encode("password123"),
                firstName = "Liisa",
                lastName = "Korhonen",
                phoneNumber = "+358401234570",
                role = UserRole.PATIENT
            )
        )

        val familyMember = userRepository.save(
            User(
                email = "family1@email.com",
                password = passwordEncoder.encode("password123"),
                firstName = "Pekka",
                lastName = "Virtanen",
                phoneNumber = "+358401234571",
                role = UserRole.FAMILY_MEMBER
            )
        )

        // --- Appointments ---
        val appointment1 = appointmentRepository.save(
            Appointment(
                patient = patient1,
                staff = doctor,
                scheduledAt = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0),
                estimatedDurationMinutes = 30,
                type = AppointmentType.HOME_VISIT,
                status = AppointmentStatus.SCHEDULED,
                notes = "Regular checkup",
                location = "Patient home - Oulu"
            )
        )

        val appointment2 = appointmentRepository.save(
            Appointment(
                patient = patient1,
                staff = nurse,
                scheduledAt = LocalDateTime.now().plusDays(5).withHour(14).withMinute(0),
                estimatedDurationMinutes = 45,
                type = AppointmentType.HOME_VISIT,
                status = AppointmentStatus.SCHEDULED,
                notes = "Wound care follow-up",
                location = "Patient home - Oulu"
            )
        )

        val appointment3 = appointmentRepository.save(
            Appointment(
                patient = patient2,
                staff = doctor,
                scheduledAt = LocalDateTime.now().plusDays(3).withHour(11).withMinute(30),
                estimatedDurationMinutes = 30,
                type = AppointmentType.TELECONSULTATION,
                status = AppointmentStatus.CONFIRMED,
                notes = "Medication review"
            )
        )

        val completedAppointment = appointmentRepository.save(
            Appointment(
                patient = patient1,
                staff = doctor,
                scheduledAt = LocalDateTime.now().minusDays(3).withHour(10).withMinute(0),
                estimatedDurationMinutes = 30,
                type = AppointmentType.HOME_VISIT,
                status = AppointmentStatus.COMPLETED,
                notes = "Initial assessment",
                location = "Patient home - Oulu"
            )
        )

        // --- Visit Summary for completed appointment ---
        visitSummaryRepository.save(
            VisitSummary(
                appointment = completedAppointment,
                summary = "Patient recovering well after surgery. Vital signs stable. Blood pressure 120/80.",
                recommendations = "Continue current medication. Light exercise recommended. Follow up in one week.",
                medications = "Paracetamol 500mg twice daily, Vitamin D supplement",
                nextVisitRecommendation = LocalDateTime.now().plusDays(7),
                createdBy = doctor
            )
        )

        // --- Care Tasks ---
        careTaskRepository.save(
            CareTask(
                patient = patient1,
                title = "Take morning medication",
                description = "Paracetamol 500mg with breakfast",
                dueDate = LocalDate.now().plusDays(1),
                dueTime = "08:00",
                status = TaskStatus.PENDING,
                frequency = TaskFrequency.DAILY
            )
        )

        careTaskRepository.save(
            CareTask(
                patient = patient1,
                title = "Blood pressure measurement",
                description = "Measure and record blood pressure. Report if above 140/90.",
                dueDate = LocalDate.now().plusDays(1),
                dueTime = "09:00",
                status = TaskStatus.PENDING,
                frequency = TaskFrequency.DAILY
            )
        )

        careTaskRepository.save(
            CareTask(
                patient = patient1,
                title = "Light walking exercise",
                description = "15 minute walk around the neighbourhood",
                dueDate = LocalDate.now().plusDays(1),
                dueTime = "16:00",
                status = TaskStatus.PENDING,
                frequency = TaskFrequency.DAILY
            )
        )

        careTaskRepository.save(
            CareTask(
                patient = patient2,
                title = "Take evening medication",
                description = "Insulin injection before dinner",
                dueDate = LocalDate.now().plusDays(1),
                dueTime = "18:00",
                status = TaskStatus.PENDING,
                frequency = TaskFrequency.DAILY
            )
        )

        careTaskRepository.save(
            CareTask(
                patient = patient2,
                title = "Blood sugar check",
                description = "Check blood sugar level and log result",
                dueDate = LocalDate.now().plusDays(1),
                dueTime = "07:00",
                status = TaskStatus.PENDING,
                frequency = TaskFrequency.DAILY
            )
        )

        // --- Staff Profiles ---
        staffProfileRepository.save(
            StaffProfile(
                user = doctor,
                department = "Home Care",
                specialization = "General Practice",
                licenseNumber = "FI-DOC-2020-1234",
                hireDate = LocalDate.of(2020, 3, 15)
            )
        )

        staffProfileRepository.save(
            StaffProfile(
                user = nurse,
                department = "Home Care",
                specialization = "Wound Care & Patient Monitoring",
                licenseNumber = "FI-NUR-2019-5678",
                hireDate = LocalDate.of(2019, 8, 1)
            )
        )

        // --- Care Assignments ---
        careAssignmentRepository.save(
            CareAssignment(patient = patient1, staff = doctor, isPrimary = true)
        )
        careAssignmentRepository.save(
            CareAssignment(patient = patient1, staff = nurse, isPrimary = true)
        )
        careAssignmentRepository.save(
            CareAssignment(patient = patient2, staff = doctor, isPrimary = true)
        )
        careAssignmentRepository.save(
            CareAssignment(patient = patient2, staff = nurse, isPrimary = true)
        )

        // --- Staff Availability (Doctor: Mon-Fri 09:00-16:00) ---
        for (day in 1..5) { // Monday=1 to Friday=5
            staffAvailabilityRepository.save(
                StaffAvailability(
                    staff = doctor,
                    dayOfWeek = day,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(16, 0),
                    isAvailable = true
                )
            )
        }

        // --- Staff Availability (Nurse: Mon-Fri 08:00-15:00) ---
        for (day in 1..5) {
            staffAvailabilityRepository.save(
                StaffAvailability(
                    staff = nurse,
                    dayOfWeek = day,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(15, 0),
                    isAvailable = true
                )
            )
        }

        // --- Time Off Request (Nurse requesting next Friday off) ---
        val nextFriday = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.FRIDAY)
        timeOffRequestRepository.save(
            TimeOffRequest(
                staff = nurse,
                startDate = nextFriday,
                endDate = nextFriday,
                reason = "Personal appointment",
                status = TimeOffStatus.PENDING
            )
        )

        // --- Recurring Appointments ---
        // Nurse visits patient1 weekly (wound care) for the next 4 weeks
        val nurseRecurringGroupId = java.util.UUID.randomUUID().toString()
        val nurseRecurringEnd = LocalDate.now().plusWeeks(4)
        var nurseVisitDate = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0)
        while (!nurseVisitDate.toLocalDate().isAfter(nurseRecurringEnd)) {
            appointmentRepository.save(
                Appointment(
                    patient = patient1,
                    staff = nurse,
                    scheduledAt = nurseVisitDate,
                    estimatedDurationMinutes = 30,
                    type = AppointmentType.HOME_VISIT,
                    status = AppointmentStatus.SCHEDULED,
                    notes = "Weekly wound care",
                    location = "Patient home - Oulu",
                    recurringGroupId = nurseRecurringGroupId,
                    recurringFrequency = RecurringFrequency.WEEKLY,
                    recurringEndDate = nurseRecurringEnd
                )
            )
            nurseVisitDate = nurseVisitDate.plusWeeks(1)
        }

        // Doctor visits patient2 biweekly (diabetes management) for 2 months
        val doctorRecurringGroupId = java.util.UUID.randomUUID().toString()
        val doctorRecurringEnd = LocalDate.now().plusMonths(2)
        var doctorVisitDate = LocalDateTime.now().plusDays(3).withHour(11).withMinute(0).withSecond(0).withNano(0)
        while (!doctorVisitDate.toLocalDate().isAfter(doctorRecurringEnd)) {
            appointmentRepository.save(
                Appointment(
                    patient = patient2,
                    staff = doctor,
                    scheduledAt = doctorVisitDate,
                    estimatedDurationMinutes = 30,
                    type = AppointmentType.HOME_VISIT,
                    status = AppointmentStatus.SCHEDULED,
                    notes = "Biweekly diabetes management checkup",
                    location = "Patient home - Oulu",
                    recurringGroupId = doctorRecurringGroupId,
                    recurringFrequency = RecurringFrequency.BIWEEKLY,
                    recurringEndDate = doctorRecurringEnd
                )
            )
            doctorVisitDate = doctorVisitDate.plusWeeks(2)
        }

        // --- Reschedule Request (Patient requests to move appointment) ---
        rescheduleRequestRepository.save(
            RescheduleRequest(
                appointment = appointment2,
                requestedBy = patient1,
                reason = "I have a hospital visit at that time",
                preferredDate1 = LocalDateTime.now().plusDays(6).withHour(10).withMinute(0),
                preferredDate2 = LocalDateTime.now().plusDays(7).withHour(14).withMinute(0),
                status = RescheduleStatus.PENDING
            )
        )

        println("=== Development data seeded successfully ===")
        println("Users: ${userRepository.count()}")
        println("Appointments: ${appointmentRepository.count()}")
        println("Care Tasks: ${careTaskRepository.count()}")
        println("Visit Summaries: ${visitSummaryRepository.count()}")
        println("Care Assignments: ${careAssignmentRepository.count()}")
        println("Staff Profiles: ${staffProfileRepository.count()}")
        println("Staff Availability Slots: ${staffAvailabilityRepository.count()}")
        println("Time Off Requests: ${timeOffRequestRepository.count()}")
        println("Reschedule Requests: ${rescheduleRequestRepository.count()}")
        println("============================================")
        println("Test accounts:")
        println("  Doctor:  dr.smith@hospital.fi / password123")
        println("  Nurse:   nurse.jones@hospital.fi / password123")
        println("  Patient: patient1@email.com / password123")
        println("  Patient: patient2@email.com / password123")
        println("  Family:  family1@email.com / password123")
        println("============================================")
    }
}
