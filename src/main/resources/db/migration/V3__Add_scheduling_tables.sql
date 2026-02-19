-- ============================================
-- V3: Add Scheduling Engine Tables
-- Adds auto-scheduling support with workforce
-- policies, visit requirements, and plan mgmt
-- ============================================

-- ============================================
-- 1. SCHEDULE PLANS
-- ============================================

CREATE TABLE schedule_plans (
    id BIGSERIAL PRIMARY KEY,
    week_start_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'CONFIRMED')),
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    UNIQUE(week_start_date, status)
);

CREATE INDEX idx_schedule_plans_week ON schedule_plans(week_start_date);
CREATE INDEX idx_schedule_plans_status ON schedule_plans(status);

-- ============================================
-- 2. PATIENT VISIT REQUIREMENTS
-- ============================================

CREATE TABLE patient_visit_requirements (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('URGENT', 'HIGH', 'ROUTINE')),
    visits_per_week INT NOT NULL CHECK (visits_per_week > 0),
    duration_minutes INT NOT NULL CHECK (duration_minutes > 0),
    visit_type VARCHAR(30) NOT NULL CHECK (visit_type IN ('HOME_VISIT', 'HOSPITAL_VISIT', 'TELECONSULTATION', 'OFFICE_WORK')),
    preferred_time_start TIME,
    preferred_time_end TIME,
    location VARCHAR(255),
    notes VARCHAR(1000),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pvr_patient ON patient_visit_requirements(patient_id);
CREATE INDEX idx_pvr_priority ON patient_visit_requirements(priority);
CREATE INDEX idx_pvr_active ON patient_visit_requirements(is_active);

-- ============================================
-- 3. UPDATE APPOINTMENTS TABLE
-- Add scheduling engine columns
-- ============================================

ALTER TABLE appointments
    ADD COLUMN plan_id BIGINT REFERENCES schedule_plans(id) ON DELETE SET NULL,
    ADD COLUMN is_generated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN is_locked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_appointments_plan ON appointments(plan_id);

-- Update type CHECK to include OFFICE_WORK
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_type_check;
ALTER TABLE appointments ADD CONSTRAINT appointments_type_check
    CHECK (type IN ('HOME_VISIT', 'HOSPITAL_VISIT', 'TELECONSULTATION', 'OFFICE_WORK'));

-- ============================================
-- 4. ADD 26 MORE PATIENTS (V2 has 4: IDs 5-8)
-- Finnish names to match Oulu home care theme
-- Password: password123 (BCrypt encoded)
-- ============================================

INSERT INTO users (email, password, first_name, last_name, phone_number, role) VALUES
('patient.aino.korhonen@email.com',    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Aino',    'Korhonen',    '+358401001005', 'PATIENT'),
('patient.veikko.virtanen@email.com',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Veikko',  'Virtanen',    '+358401001006', 'PATIENT'),
('patient.liisa.makela@email.com',     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Liisa',   'Mäkelä',      '+358401001007', 'PATIENT'),
('patient.eero.nieminen@email.com',    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Eero',    'Nieminen',    '+358401001008', 'PATIENT'),
('patient.helmi.hamalainen@email.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Helmi',   'Hämäläinen',  '+358401001009', 'PATIENT'),
('patient.onni.laine@email.com',       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Onni',    'Laine',       '+358401001010', 'PATIENT'),
('patient.martta.heikkinen@email.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Martta',  'Heikkinen',   '+358401001011', 'PATIENT'),
('patient.vilho.koskinen@email.com',   '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Vilho',   'Koskinen',    '+358401001012', 'PATIENT'),
('patient.kaarina.jarvinen@email.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Kaarina', 'Järvinen',    '+358401001013', 'PATIENT'),
('patient.tapio.lehtinen@email.com',   '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Tapio',   'Lehtinen',    '+358401001014', 'PATIENT'),
('patient.sirkka.salminen@email.com',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Sirkka',  'Salminen',    '+358401001015', 'PATIENT'),
('patient.pentti.heinonen@email.com',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Pentti',  'Heinonen',    '+358401001016', 'PATIENT'),
('patient.raija.niemi@email.com',      '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Raija',   'Niemi',       '+358401001017', 'PATIENT'),
('patient.erkki.makinen@email.com',    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Erkki',   'Mäkinen',     '+358401001018', 'PATIENT'),
('patient.tuula.hakala@email.com',     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Tuula',   'Hakala',      '+358401001019', 'PATIENT'),
('patient.jorma.lahtinen@email.com',   '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Jorma',   'Lahtinen',    '+358401001020', 'PATIENT'),
('patient.maija.ahonen@email.com',     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Maija',   'Ahonen',      '+358401001021', 'PATIENT'),
('patient.kalevi.leinonen@email.com',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Kalevi',  'Leinonen',    '+358401001022', 'PATIENT'),
('patient.anneli.hiltunen@email.com',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Anneli',  'Hiltunen',    '+358401001023', 'PATIENT'),
('patient.paavo.karjalainen@email.com','$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Paavo',   'Karjalainen', '+358401001024', 'PATIENT'),
('patient.ritva.savolainen@email.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Ritva',   'Savolainen',  '+358401001025', 'PATIENT'),
('patient.unto.laaksonen@email.com',   '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Unto',    'Laaksonen',   '+358401001026', 'PATIENT'),
('patient.irma.mattila@email.com',     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Irma',    'Mattila',     '+358401001027', 'PATIENT'),
('patient.seppo.rantanen@email.com',   '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Seppo',   'Rantanen',    '+358401001028', 'PATIENT'),
('patient.terttu.ylonen@email.com',    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Terttu',  'Ylönen',      '+358401001029', 'PATIENT'),
('patient.heikki.aaltonen@email.com',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Heikki',  'Aaltonen',    '+358401001030', 'PATIENT');

-- ============================================
-- 5. CARE ASSIGNMENTS for new patients
-- Round-robin across 4 staff (IDs 1-4)
-- V2 already assigned: 5→1, 6→1, 7→2, 8→2
-- New patients (11-36) distributed evenly
-- ============================================

INSERT INTO care_assignments (patient_id, staff_id, is_primary) VALUES
-- Staff 1 (Dr. Johnson): patients 11,15,19,23,27,31,35
(11, 1, TRUE), (15, 1, TRUE), (19, 1, TRUE), (23, 1, TRUE), (27, 1, TRUE), (31, 1, TRUE), (35, 1, TRUE),
-- Staff 2 (Dr. Chen): patients 12,16,20,24,28,32,36
(12, 2, TRUE), (16, 2, TRUE), (20, 2, TRUE), (24, 2, TRUE), (28, 2, TRUE), (32, 2, TRUE), (36, 2, TRUE),
-- Staff 3 (Nurse Davis): patients 13,17,21,25,29,33
(13, 3, TRUE), (17, 3, TRUE), (21, 3, TRUE), (25, 3, TRUE), (29, 3, TRUE), (33, 3, TRUE),
-- Staff 4 (Nurse Wilson): patients 14,18,22,26,30,34
(14, 4, TRUE), (18, 4, TRUE), (22, 4, TRUE), (26, 4, TRUE), (30, 4, TRUE), (34, 4, TRUE);

-- ============================================
-- 6. PATIENT VISIT REQUIREMENTS (30 patients)
-- 3 URGENT + 5 HIGH + 22 ROUTINE = 30
-- All: 1 visit/week, HOME_VISIT type
-- ============================================

INSERT INTO patient_visit_requirements (patient_id, priority, visits_per_week, duration_minutes, visit_type, preferred_time_start, preferred_time_end, location, notes) VALUES
-- URGENT (patients 5, 6, 7) — V2 patients
(5,  'URGENT', 1, 45, 'HOME_VISIT', '08:00', '12:00', 'Kaijonharju, Oulu',  'Post-surgery wound care'),
(6,  'URGENT', 1, 40, 'HOME_VISIT', NULL,    NULL,    'Tuira, Oulu',         'Insulin management, unstable levels'),
(7,  'URGENT', 1, 60, 'HOME_VISIT', '09:00', '11:00', 'Keskusta, Oulu',     'Palliative care assessment'),

-- HIGH (patients 8, 11, 12, 13, 14)
(8,  'HIGH', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Linnanmaa, Oulu',    'COPD monitoring'),
(11, 'HIGH', 1, 45, 'HOME_VISIT', '10:00', '14:00', 'Myllyoja, Oulu',     'Cardiac rehabilitation'),
(12, 'HIGH', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Kontinkangas, Oulu', 'Blood pressure monitoring'),
(13, 'HIGH', 1, 30, 'HOME_VISIT', '08:00', '10:00', 'Pateniemi, Oulu',    'Diabetes wound care'),
(14, 'HIGH', 1, 40, 'HOME_VISIT', NULL,    NULL,    'Kaukovainio, Oulu',  'Fall risk assessment'),

-- ROUTINE (patients 15-36)
(15, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Haukipudas, Oulu',   'Routine checkup'),
(16, 'ROUTINE', 1, 30, 'HOME_VISIT', '13:00', '16:00', 'Tuira, Oulu',        'Medication review'),
(17, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Oulunsalo, Oulu',    'Blood sugar monitoring'),
(18, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Kempele',            'Physiotherapy follow-up'),
(19, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Keskusta, Oulu',     'Mental health check-in'),
(20, 'ROUTINE', 1, 30, 'HOME_VISIT', '08:00', '12:00', 'Liminka',            'Chronic pain management'),
(21, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Ii',                 'Respiratory therapy'),
(22, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Linnanmaa, Oulu',    'Weight management counseling'),
(23, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Muhos',              'Mobility assessment'),
(24, 'ROUTINE', 1, 30, 'HOME_VISIT', '12:00', '16:00', 'Kiiminki',           'Elderly care routine visit'),
(25, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Pateniemi, Oulu',    'Prescription renewal visit'),
(26, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Ylikiiminki',        'Post-stroke rehabilitation'),
(27, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Haukipudas, Oulu',   'Dementia care visit'),
(28, 'ROUTINE', 1, 30, 'HOME_VISIT', '14:00', '16:00', 'Kaukovainio, Oulu',  'Lab results follow-up'),
(29, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Oulunsalo, Oulu',    'Nutritional assessment'),
(30, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Kempele',            'Skin integrity check'),
(31, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Kontinkangas, Oulu', 'Sleep disorder follow-up'),
(32, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Muhos',              'Catheter maintenance'),
(33, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Liminka',            'General wellness check'),
(34, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Ii',                 'Anxiety management follow-up'),
(35, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Ii',                 'Osteoporosis care'),
(36, 'ROUTINE', 1, 30, 'HOME_VISIT', NULL,    NULL,    'Haukipudas, Oulu',   'Hypertension follow-up');
