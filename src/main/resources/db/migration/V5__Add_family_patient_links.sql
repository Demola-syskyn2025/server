-- Create family_patient_links table for linking family members to patients
CREATE TABLE family_patient_links (
    id BIGSERIAL PRIMARY KEY,
    family_member_id BIGINT NOT NULL REFERENCES users(id),
    patient_id BIGINT NOT NULL REFERENCES users(id),
    relationship VARCHAR(100) NOT NULL DEFAULT 'Family Member',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(family_member_id, patient_id)
);

CREATE INDEX idx_family_patient_links_family_member ON family_patient_links(family_member_id);
CREATE INDEX idx_family_patient_links_patient ON family_patient_links(patient_id);
