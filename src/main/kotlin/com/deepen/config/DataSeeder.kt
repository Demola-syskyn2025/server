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
        patientVisitRequirementRepository: PatientVisitRequirementRepository,
        familyPatientLinkRepository: FamilyPatientLinkRepository,
        passwordEncoder: PasswordEncoder
    ): CommandLineRunner = CommandLineRunner {

        if (userRepository.count() > 0) return@CommandLineRunner

        // Encode password once and reuse for all seed users
        val encodedPassword = passwordEncoder.encode("password123")

        // ══════════════════════════════════════════
        // STAFF (2 doctors + 2 nurses = 4 staff)
        // ══════════════════════════════════════════

        val doctor1 = userRepository.save(User(email = "dr.smith@hospital.fi", password = encodedPassword, firstName = "John", lastName = "Smith", phoneNumber = "+358401234001", role = UserRole.DOCTOR))
        val doctor2 = userRepository.save(User(email = "dr.lehtonen@hospital.fi", password = encodedPassword, firstName = "Anna", lastName = "Lehtonen", phoneNumber = "+358401234002", role = UserRole.DOCTOR))
        val nurse1 = userRepository.save(User(email = "nurse.jones@hospital.fi", password = encodedPassword, firstName = "Sarah", lastName = "Jones", phoneNumber = "+358401234003", role = UserRole.NURSE))
        val nurse2 = userRepository.save(User(email = "nurse.makinen@hospital.fi", password = encodedPassword, firstName = "Maria", lastName = "Mäkinen", phoneNumber = "+358401234004", role = UserRole.NURSE))

        val staff = listOf(doctor1, doctor2, nurse1, nurse2)

        // ══════════════════════════════════════════
        // PATIENTS (30 patients with Finnish names)
        // ══════════════════════════════════════════

        data class PatientSeed(val first: String, val last: String, val phone: String)
        val patientSeeds = listOf(
            PatientSeed("Matti", "Virtanen", "+358401234101"),
            PatientSeed("Liisa", "Korhonen", "+358401234102"),
            PatientSeed("Jukka", "Nieminen", "+358401234103"),
            PatientSeed("Aino", "Mäkelä", "+358401234104"),
            PatientSeed("Mikko", "Hämäläinen", "+358401234105"),
            PatientSeed("Tuula", "Laine", "+358401234106"),
            PatientSeed("Antti", "Heikkinen", "+358401234107"),
            PatientSeed("Sari", "Koskinen", "+358401234108"),
            PatientSeed("Pekka", "Järvinen", "+358401234109"),
            PatientSeed("Kaarina", "Lehtinen", "+358401234110"),
            PatientSeed("Eero", "Salminen", "+358401234111"),
            PatientSeed("Riitta", "Lahtinen", "+358401234112"),
            PatientSeed("Veikko", "Ahonen", "+358401234113"),
            PatientSeed("Maija", "Niemi", "+358401234114"),
            PatientSeed("Heikki", "Rantanen", "+358401234115"),
            PatientSeed("Anneli", "Lähteenmäki", "+358401234116"),
            PatientSeed("Juha", "Hiltunen", "+358401234117"),
            PatientSeed("Pirjo", "Karjalainen", "+358401234118"),
            PatientSeed("Markku", "Ojala", "+358401234119"),
            PatientSeed("Helena", "Manninen", "+358401234120"),
            PatientSeed("Seppo", "Savolainen", "+358401234121"),
            PatientSeed("Raija", "Laaksonen", "+358401234122"),
            PatientSeed("Timo", "Lindström", "+358401234123"),
            PatientSeed("Sirpa", "Kallio", "+358401234124"),
            PatientSeed("Kari", "Salonen", "+358401234125"),
            PatientSeed("Leena", "Tuominen", "+358401234126"),
            PatientSeed("Hannu", "Yli-Harja", "+358401234127"),
            PatientSeed("Marjatta", "Pukkila", "+358401234128"),
            PatientSeed("Ilkka", "Toivonen", "+358401234129"),
            PatientSeed("Ulla", "Paavola", "+358401234130")
        )

        val patients = patientSeeds.mapIndexed { i, seed ->
            userRepository.save(User(
                email = "patient${i + 1}@email.com",
                password = encodedPassword,
                firstName = seed.first,
                lastName = seed.last,
                phoneNumber = seed.phone,
                role = UserRole.PATIENT
            ))
        }

        // ── Family members (2) ───────────────────
        val family1 = userRepository.save(User(email = "family1@email.com", password = encodedPassword, firstName = "Pekka", lastName = "Virtanen", phoneNumber = "+358401234201", role = UserRole.FAMILY_MEMBER))
        val family2 = userRepository.save(User(email = "family2@email.com", password = encodedPassword, firstName = "Laura", lastName = "Korhonen", phoneNumber = "+358401234202", role = UserRole.FAMILY_MEMBER))

        // ══════════════════════════════════════════
        // FAMILY-PATIENT LINKS
        // Link family members to some patients
        // ══════════════════════════════════════════
        familyPatientLinkRepository.save(FamilyPatientLink(familyMember = family1, patient = patients[0], relationship = "Spouse"))
        familyPatientLinkRepository.save(FamilyPatientLink(familyMember = family1, patient = patients[1], relationship = "Parent"))
        familyPatientLinkRepository.save(FamilyPatientLink(familyMember = family2, patient = patients[2], relationship = "Child"))
        familyPatientLinkRepository.save(FamilyPatientLink(familyMember = family2, patient = patients[3], relationship = "Sibling"))

        // ══════════════════════════════════════════
        // STAFF PROFILES
        // ══════════════════════════════════════════

        staffProfileRepository.save(StaffProfile(user = doctor1, department = "Home Care", specialization = "General Practice", licenseNumber = "FI-DOC-2020-1234", hireDate = LocalDate.of(2020, 3, 15)))
        staffProfileRepository.save(StaffProfile(user = doctor2, department = "Home Care", specialization = "Geriatric Medicine", licenseNumber = "FI-DOC-2018-5678", hireDate = LocalDate.of(2018, 9, 1)))
        staffProfileRepository.save(StaffProfile(user = nurse1, department = "Home Care", specialization = "Wound Care & Patient Monitoring", licenseNumber = "FI-NUR-2019-9012", hireDate = LocalDate.of(2019, 8, 1)))
        staffProfileRepository.save(StaffProfile(user = nurse2, department = "Home Care", specialization = "Chronic Disease Management", licenseNumber = "FI-NUR-2021-3456", hireDate = LocalDate.of(2021, 1, 10)))

        // ══════════════════════════════════════════
        // STAFF AVAILABILITY (Mon-Fri 08:00-16:00, one day off each)
        // Doctor1: Monday off, Doctor2: Tuesday off
        // Nurse1: Wednesday off, Nurse2: Thursday off
        // ══════════════════════════════════════════

        val dayOffMap = mapOf(doctor1 to 1, doctor2 to 2, nurse1 to 3, nurse2 to 4) // day of week numbers

        for (s in staff) {
            val dayOff = dayOffMap[s]!!
            for (day in 1..5) {
                staffAvailabilityRepository.save(
                    StaffAvailability(
                        staff = s,
                        dayOfWeek = day,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        isAvailable = day != dayOff
                    )
                )
            }
        }

        // ══════════════════════════════════════════
        // CARE ASSIGNMENTS (each patient gets a primary staff)
        // Distribute ~8 patients per staff member
        // ══════════════════════════════════════════

        for ((i, patient) in patients.withIndex()) {
            val primaryStaff = staff[i % staff.size]
            careAssignmentRepository.save(CareAssignment(patient = patient, staff = primaryStaff, isPrimary = true))
        }

        // ══════════════════════════════════════════
        // PATIENT VISIT REQUIREMENTS
        // Mix of priorities, frequencies, types, and time windows
        // ══════════════════════════════════════════

        data class VisitReqSeed(
            val priority: VisitPriority,
            val visitsPerWeek: Int,
            val duration: Int,
            val type: AppointmentType,
            val timeStart: LocalTime?,
            val timeEnd: LocalTime?,
            val location: String?,
            val notes: String?
        )

        val visitReqSeeds = listOf(
            // Patients 0-2: URGENT, 1 home visit/week
            VisitReqSeed(VisitPriority.URGENT, 1, 45, AppointmentType.HOME_VISIT, LocalTime.of(8, 0), LocalTime.of(12, 0), "Kaijonharju, Oulu", "Post-surgery wound care"),
            VisitReqSeed(VisitPriority.URGENT, 1, 40, AppointmentType.HOME_VISIT, null, null, "Tuira, Oulu", "Insulin management, unstable levels"),
            VisitReqSeed(VisitPriority.URGENT, 1, 60, AppointmentType.HOME_VISIT, LocalTime.of(9, 0), LocalTime.of(11, 0), "Keskusta, Oulu", "Palliative care assessment"),
            // Patients 3-7: HIGH priority, 1 home visit/week
            VisitReqSeed(VisitPriority.HIGH, 1, 30, AppointmentType.HOME_VISIT, null, null, "Linnanmaa, Oulu", "COPD monitoring"),
            VisitReqSeed(VisitPriority.HIGH, 1, 45, AppointmentType.HOME_VISIT, LocalTime.of(10, 0), LocalTime.of(14, 0), "Myllyoja, Oulu", "Cardiac rehabilitation"),
            VisitReqSeed(VisitPriority.HIGH, 1, 30, AppointmentType.HOME_VISIT, null, null, "Kontinkangas, Oulu", "Blood pressure monitoring"),
            VisitReqSeed(VisitPriority.HIGH, 1, 30, AppointmentType.HOME_VISIT, LocalTime.of(8, 0), LocalTime.of(10, 0), "Pateniemi, Oulu", "Diabetes wound care"),
            VisitReqSeed(VisitPriority.HIGH, 1, 40, AppointmentType.HOME_VISIT, null, null, "Kaukovainio, Oulu", "Fall risk assessment"),
            // Patients 8-19: ROUTINE, 1 home visit/week
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Haukipudas, Oulu", "Routine checkup"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, LocalTime.of(13, 0), LocalTime.of(16, 0), "Tuira, Oulu", "Medication review"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Oulunsalo, Oulu", "Blood sugar monitoring"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Kempele", "Physiotherapy follow-up"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Keskusta, Oulu", "Mental health check-in"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, LocalTime.of(8, 0), LocalTime.of(12, 0), "Liminka", "Chronic pain management"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Ii", "Respiratory therapy"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Linnanmaa, Oulu", "Weight management counseling"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Muhos", "Mobility assessment"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, LocalTime.of(12, 0), LocalTime.of(16, 0), "Kiiminki", "Elderly care routine visit"),
            // Patients 20-24: ROUTINE, 1 home visit/week
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Pateniemi, Oulu", "Prescription renewal visit"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Ylikiiminki", "Post-stroke rehabilitation"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Haukipudas, Oulu", "Dementia care visit"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, LocalTime.of(14, 0), LocalTime.of(16, 0), "Kaukovainio, Oulu", "Lab results follow-up"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Oulunsalo, Oulu", "Nutritional assessment"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Kempele", "Skin integrity check"),
            // Patients 25-29: ROUTINE, 1 home visit/week
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Kontinkangas, Oulu", "Sleep disorder follow-up"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Muhos", "Catheter maintenance"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Liminka", "General wellness check"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Ii", "Anxiety management follow-up"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Ii", "Osteoporosis care"),
            VisitReqSeed(VisitPriority.ROUTINE, 1, 30, AppointmentType.HOME_VISIT, null, null, "Haukipudas, Oulu", "Hypertension follow-up")
        )

        for ((i, patient) in patients.withIndex()) {
            val seed = visitReqSeeds[i]
            patientVisitRequirementRepository.save(
                PatientVisitRequirement(
                    patient = patient,
                    priority = seed.priority,
                    visitsPerWeek = seed.visitsPerWeek,
                    durationMinutes = seed.duration,
                    visitType = seed.type,
                    preferredTimeStart = seed.timeStart,
                    preferredTimeEnd = seed.timeEnd,
                    location = seed.location,
                    notes = seed.notes
                )
            )
        }

        // ══════════════════════════════════════════
        // SAMPLE APPOINTMENTS (a few completed for history)
        // ══════════════════════════════════════════

        val completedAppt1 = appointmentRepository.save(Appointment(
            patient = patients[0], staff = doctor1,
            scheduledAt = LocalDateTime.now().minusDays(3).withHour(10).withMinute(0),
            estimatedDurationMinutes = 30, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.COMPLETED, notes = "Initial assessment",
            location = "Kaijonharju, Oulu"
        ))

        appointmentRepository.save(Appointment(
            patient = patients[1], staff = nurse1,
            scheduledAt = LocalDateTime.now().minusDays(2).withHour(9).withMinute(0),
            estimatedDurationMinutes = 40, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.COMPLETED, notes = "Insulin level check",
            location = "Tuira, Oulu"
        ))

        // --- Visit Summary ---
        visitSummaryRepository.save(VisitSummary(
            appointment = completedAppt1,
            summary = "Patient recovering well after surgery. Vital signs stable. Blood pressure 120/80.",
            recommendations = "Continue current medication. Light exercise recommended.",
            medications = "Paracetamol 500mg twice daily, Vitamin D supplement",
            nextVisitRecommendation = LocalDateTime.now().plusDays(7),
            createdBy = doctor1
        ))

        // ══════════════════════════════════════════
        // CARE TASKS (sample tasks for first few patients)
        // ══════════════════════════════════════════

        careTaskRepository.save(CareTask(patient = patients[0], title = "Take morning medication", description = "Paracetamol 500mg with breakfast", dueDate = LocalDate.now().plusDays(1), dueTime = "08:00", status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY))
        careTaskRepository.save(CareTask(patient = patients[0], title = "Blood pressure measurement", description = "Measure and record BP. Report if above 140/90.", dueDate = LocalDate.now().plusDays(1), dueTime = "09:00", status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY))
        careTaskRepository.save(CareTask(patient = patients[1], title = "Blood sugar check", description = "Check blood sugar level before meals", dueDate = LocalDate.now().plusDays(1), dueTime = "07:00", status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY))
        careTaskRepository.save(CareTask(patient = patients[1], title = "Insulin injection", description = "Insulin injection before dinner", dueDate = LocalDate.now().plusDays(1), dueTime = "18:00", status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY))
        careTaskRepository.save(CareTask(patient = patients[2], title = "Pain diary", description = "Record pain level 1-10 and location", dueDate = LocalDate.now().plusDays(1), dueTime = "20:00", status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY))

        // ══════════════════════════════════════════
        // TIME OFF REQUEST
        // ══════════════════════════════════════════

        val nextFriday = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.FRIDAY)
        timeOffRequestRepository.save(TimeOffRequest(staff = nurse1, startDate = nextFriday, endDate = nextFriday, reason = "Personal appointment", status = TimeOffStatus.PENDING))

        // ══════════════════════════════════════════
        // PRINT SUMMARY
        // ══════════════════════════════════════════

        println("=== Development data seeded successfully ===")
        println("Staff: ${staff.size} (2 doctors, 2 nurses)")
        println("Patients: ${patients.size}")
        println("Users total: ${userRepository.count()}")
        println("Care Assignments: ${careAssignmentRepository.count()}")
        println("Visit Requirements: ${patientVisitRequirementRepository.count()}")
        println("Staff Availability Slots: ${staffAvailabilityRepository.count()}")
        println("Appointments: ${appointmentRepository.count()}")
        println("Care Tasks: ${careTaskRepository.count()}")
        println("============================================")
        println("Test accounts (password: password123):")
        println("  Doctor1: dr.smith@hospital.fi")
        println("  Doctor2: dr.lehtonen@hospital.fi")
        println("  Nurse1:  nurse.jones@hospital.fi")
        println("  Nurse2:  nurse.makinen@hospital.fi")
        println("  Patient: patient1@email.com ... patient30@email.com")
        println("  Family:  family1@email.com, family2@email.com")
        println("============================================")
    }
}
