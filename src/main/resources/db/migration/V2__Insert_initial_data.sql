-- ============================================
-- Deepen Database Schema - V2
-- Add initial data for development/production
-- ============================================

-- Insert default users (passwords are BCrypt encoded)
-- Password for all users: "password123" (encoded: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi)

-- Insert doctors
INSERT INTO users (email, password, first_name, last_name, phone_number, role) VALUES
('dr.sarah.johnson@hospital.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Sarah', 'Johnson', '+1-555-0101', 'DOCTOR'),
('dr.michael.chen@hospital.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Michael', 'Chen', '+1-555-0102', 'DOCTOR');

-- Insert nurses
INSERT INTO users (email, password, first_name, last_name, phone_number, role) VALUES
('nurse.emily.davis@hospital.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Emily', 'Davis', '+1-555-0103', 'NURSE'),
('nurse.james.wilson@hospital.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'James', 'Wilson', '+1-555-0104', 'NURSE');

-- Insert patients
INSERT INTO users (email, password, first_name, last_name, phone_number, role) VALUES
('patient.john.smith@email.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'John', 'Smith', '+1-555-0201', 'PATIENT'),
('patient.mary.jones@email.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Mary', 'Jones', '+1-555-0202', 'PATIENT'),
('patient.robert.brown@email.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Robert', 'Brown', '+1-555-0203', 'PATIENT'),
('patient.patricia.davis@email.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Patricia', 'Davis', '+1-555-0204', 'PATIENT');

-- Insert family members
INSERT INTO users (email, password, first_name, last_name, phone_number, role) VALUES
('family.jane.smith@email.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Jane', 'Smith', '+1-555-0301', 'FAMILY_MEMBER'),
('family.mike.jones@email.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Mike', 'Jones', '+1-555-0302', 'FAMILY_MEMBER');

-- Insert staff profiles
INSERT INTO staff_profiles (user_id, department, specialization, license_number, hire_date) VALUES
(1, 'Internal Medicine', 'Primary Care', 'MD123456', '2020-01-15'),
(2, 'Cardiology', 'Heart Disease', 'MD789012', '2019-03-20'),
(3, 'General Nursing', 'Patient Care', 'RN345678', '2021-06-10'),
(4, 'Critical Care', 'ICU Nursing', 'RN901234', '2020-09-05');

-- Insert staff availability (Monday-Friday, 9AM-5PM)
INSERT INTO staff_availability (staff_id, day_of_week, start_time, end_time, is_available) VALUES
-- Dr. Sarah Johnson
(1, 1, '09:00:00', '17:00:00', TRUE),  -- Monday
(1, 2, '09:00:00', '17:00:00', TRUE),  -- Tuesday
(1, 3, '09:00:00', '17:00:00', TRUE),  -- Wednesday
(1, 4, '09:00:00', '17:00:00', TRUE),  -- Thursday
(1, 5, '09:00:00', '17:00:00', TRUE),  -- Friday
-- Dr. Michael Chen
(2, 1, '08:00:00', '16:00:00', TRUE),  -- Monday
(2, 2, '08:00:00', '16:00:00', TRUE),  -- Tuesday
(2, 3, '08:00:00', '16:00:00', TRUE),  -- Wednesday
(2, 4, '08:00:00', '16:00:00', TRUE),  -- Thursday
(2, 5, '08:00:00', '16:00:00', TRUE),  -- Friday
-- Nurse Emily Davis
(3, 1, '07:00:00', '19:00:00', TRUE),  -- Monday
(3, 2, '07:00:00', '19:00:00', TRUE),  -- Tuesday
(3, 3, '07:00:00', '19:00:00', TRUE),  -- Wednesday
(3, 4, '07:00:00', '19:00:00', TRUE),  -- Thursday
(3, 5, '07:00:00', '19:00:00', TRUE),  -- Friday
-- Nurse James Wilson
(4, 1, '07:00:00', '19:00:00', TRUE),  -- Monday
(4, 2, '07:00:00', '19:00:00', TRUE),  -- Tuesday
(4, 3, '07:00:00', '19:00:00', TRUE),  -- Wednesday
(4, 4, '07:00:00', '19:00:00', TRUE),  -- Thursday
(4, 5, '07:00:00', '19:00:00', TRUE);  -- Friday

-- Insert care assignments
INSERT INTO care_assignments (patient_id, staff_id, is_primary) VALUES
-- Dr. Sarah Johnson's patients
(5, 1, TRUE),   -- John Smith
(6, 1, TRUE),   -- Mary Jones
-- Dr. Michael Chen's patients
(7, 2, TRUE),   -- Robert Brown
(8, 2, TRUE),   -- Patricia Davis
-- Nurse assignments (supporting)
(5, 3, FALSE),  -- John Smith - Nurse Emily
(6, 3, FALSE),  -- Mary Jones - Nurse Emily
(7, 4, FALSE),  -- Robert Brown - Nurse James
(8, 4, FALSE);  -- Patricia Davis - Nurse James

-- Insert sample appointments
INSERT INTO appointments (patient_id, staff_id, scheduled_at, estimated_duration_minutes, type, status, notes, location) VALUES
(5, 1, CURRENT_TIMESTAMP + INTERVAL '1 day', 60, 'HOME_VISIT', 'SCHEDULED', 'Regular checkup', 'Patient Home'),
(6, 1, CURRENT_TIMESTAMP + INTERVAL '2 days', 45, 'TELECONSULTATION', 'SCHEDULED', 'Follow-up consultation', 'Virtual'),
(7, 2, CURRENT_TIMESTAMP + INTERVAL '3 days', 90, 'HOSPITAL_VISIT', 'SCHEDULED', 'Cardiac evaluation', 'Hospital Room 301'),
(8, 2, CURRENT_TIMESTAMP + INTERVAL '4 days', 60, 'HOME_VISIT', 'SCHEDULED', 'Post-discharge check', 'Patient Home');

-- Insert sample care tasks
INSERT INTO care_tasks (patient_id, title, description, due_date, frequency, status) VALUES
(5, 'Morning Medication', 'Take blood pressure medication', CURRENT_DATE, 'DAILY', 'PENDING'),
(5, 'Blood Pressure Check', 'Check and record blood pressure', CURRENT_DATE, 'DAILY', 'PENDING'),
(6, 'Glucose Monitoring', 'Check blood sugar levels', CURRENT_DATE, 'DAILY', 'PENDING'),
(7, 'Heart Medication', 'Take prescribed heart medication', CURRENT_DATE, 'DAILY', 'PENDING'),
(8, 'Wound Care', 'Change dressing on surgical wound', CURRENT_DATE, 'DAILY', 'PENDING');

-- Insert patient preferences
INSERT INTO patient_preferences (patient_id, preferred_location, preferred_visit_type, preferred_day_of_week, preferred_time_start, preferred_time_end, avoid_mornings, avoid_evenings, notes) VALUES
(5, 'Patient Home', 0, 2, '10:00:00', '14:00:00', TRUE, FALSE, 'Prefers late morning appointments'),
(6, 'Virtual', 2, 3, '14:00:00', '16:00:00', TRUE, TRUE, 'Retired, flexible schedule'),
(7, 'Hospital', 1, 1, '09:00:00', '11:00:00', FALSE, TRUE, 'Has mobility issues, prefers hospital'),
(8, 'Patient Home', 0, 4, '11:00:00', '15:00:00', TRUE, FALSE, 'Lives alone, needs home visits');
