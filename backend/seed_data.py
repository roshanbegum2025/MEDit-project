import json
import sqlite3
from pathlib import Path
from database import get_db, init_db

def seed_database(db_path=None):
    init_db(db_path)
    conn = get_db(db_path)
    cursor = conn.cursor()

    # Clear existing data for fresh demo seed
    cursor.execute("DELETE FROM audit_logs;")
    cursor.execute("DELETE FROM access_permissions;")
    cursor.execute("DELETE FROM access_requests;")
    cursor.execute("DELETE FROM medical_records;")
    cursor.execute("DELETE FROM patients;")

    # 1. Seed Patients
    patients = [
        (
            "PAT-101",
            "Sarah Jenkins",
            "1988-04-12",
            "Female",
            "O+",
            "+1 (555) 234-5678",
            "sarah.jenkins@example.com",
            json.dumps(["Penicillin", "Amoxicillin"]),
            json.dumps(["Mild Asthma", "Essential Hypertension"]),
            "David Jenkins (Spouse) - +1 (555) 234-5679",
            "2024-01-10 09:00:00Z"
        ),
        (
            "PAT-102",
            "Robert Chen",
            "1972-09-24",
            "Male",
            "A+",
            "+1 (555) 876-5432",
            "robert.chen@example.com",
            json.dumps(["Sulfa antibiotics"]),
            json.dumps(["Type 2 Diabetes Mellitus", "Hyperlipidemia"]),
            "Grace Chen (Sister) - +1 (555) 876-5433",
            "2024-02-15 11:30:00Z"
        ),
        (
            "PAT-103",
            "Elena Rodriguez",
            "1995-11-03",
            "Female",
            "B+",
            "+1 (555) 432-1098",
            "elena.rodriguez@example.com",
            json.dumps(["Latex"]),
            json.dumps(["None"]),
            "Carlos Rodriguez (Brother) - +1 (555) 432-1099",
            "2024-03-01 14:00:00Z"
        )
    ]

    cursor.executemany("""
        INSERT INTO patients (id, name, dob, gender, blood_type, phone, email, allergies, chronic_conditions, emergency_contact, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, patients)

    # 2. Seed Medical Records (Fragmented sources unified)
    records = [
        # Patient 101 - Sarah Jenkins
        (
            "REC-001",
            "PAT-101",
            "prescription",
            "Lisinopril 10mg Oral Tablet",
            "St. Jude Medical Center",
            "2026-03-18",
            "Initiated once daily in the morning for newly diagnosed Stage 1 primary hypertension. Patient advised on hydration, limiting sodium intake, and monitoring for dry cough.",
            "Cardiology",
            json.dumps({"target_bp": "< 125/80 mmHg", "refills": "3", "supply_days": "30"}),
            json.dumps([{"name": "Lisinopril", "dosage": "10mg", "route": "Oral", "frequency": "Once daily morning"}]),
            "verified",
            "doctor",
            "DOC-201",
            "2026-03-18 14:45:00Z"
        ),
        (
            "REC-002",
            "PAT-101",
            "diagnosis",
            "Stage 1 Essential Hypertension",
            "St. Jude Medical Center",
            "2026-03-18",
            "Confirmed primary hypertension based on repeated clinic measurements (138/88 and 142/90) and home blood pressure log review. 12-lead ECG showed normal sinus rhythm without left ventricular hypertrophy.",
            "Cardiology",
            json.dumps({"clinic_bp": "142/90 mmHg", "heart_rate": "72 bpm", "ecg_result": "Normal Sinus Rhythm"}),
            json.dumps([]),
            "verified",
            "doctor",
            "DOC-201",
            "2026-03-18 14:30:00Z"
        ),
        (
            "REC-003",
            "PAT-101",
            "lab_report",
            "Comprehensive Metabolic Panel & Lipid Profile",
            "Apex Diagnostics Laboratory",
            "2026-03-12",
            "Fasting specimen analyzed. Serum electrolytes, BUN, creatinine, and liver enzymes within normal parameters. Lipid panel indicates mildly elevated total cholesterol and LDL requiring lifestyle counseling.",
            "Hematology & Biochemistry",
            json.dumps({
                "Total Cholesterol": "215 mg/dL (High)",
                "LDL Cholesterol": "138 mg/dL (Elevated)",
                "HDL Cholesterol": "48 mg/dL (Normal)",
                "Triglycerides": "145 mg/dL (Normal)",
                "Fasting Glucose": "94 mg/dL (Normal)",
                "Creatinine": "0.85 mg/dL (Normal)"
            }),
            json.dumps([]),
            "verified",
            "provider",
            "PROV-301",
            "2026-03-12 16:15:00Z"
        ),
        (
            "REC-004",
            "PAT-101",
            "visit",
            "Annual Health & Wellness Examination",
            "Metro Health Clinic",
            "2026-03-10",
            "Routine annual physical exam. Patient reports mild work-related stress. Lungs clear to auscultation bilaterally. Elevated resting blood pressure observed. Diagnostic blood panel and cardiology referral dispatched.",
            "General Medicine",
            json.dumps({"blood_pressure": "138/88 mmHg", "pulse": "74 bpm", "bmi": "23.4", "spo2": "99%"}),
            json.dumps([]),
            "verified",
            "doctor",
            "DOC-202",
            "2026-03-10 11:00:00Z"
        ),
        (
            "REC-005",
            "PAT-101",
            "allergy",
            "Severe Drug Allergy Documentation: Penicillin",
            "Metro Health Clinic",
            "2026-02-04",
            "Documented allergic reaction to Amoxicillin/Penicillin class antibiotics. Reaction manifested as acute urticaria, facial angioedema, and wheezing. Patient carries emergency epinephrine and must avoid beta-lactam antibiotics.",
            "Allergy & Immunology",
            json.dumps({"severity": "High / Anaphylactoid", "reaction_type": "Urticaria & Bronchospasm"}),
            json.dumps([]),
            "verified",
            "doctor",
            "DOC-202",
            "2026-02-04 15:20:00Z"
        ),
        (
            "REC-006",
            "PAT-101",
            "prescription",
            "Albuterol Sulfate HFA Inhaler 90mcg",
            "Metro Health Clinic",
            "2026-02-04",
            "1-2 puffs every 4-6 hours as needed for bronchospasm, shortness of breath, or prior to vigorous exercise. Refills authorized for 12 months.",
            "Pulmonology",
            json.dumps({"canister_puffs": "200", "refills": "2"}),
            json.dumps([{"name": "Albuterol Sulfate HFA", "dosage": "90mcg/actuation", "route": "Inhalation", "frequency": "PRN as needed"}]),
            "verified",
            "doctor",
            "DOC-202",
            "2026-02-04 15:30:00Z"
        ),
        (
            "REC-007",
            "PAT-101",
            "lab_report",
            "Complete Blood Count (CBC) with Differential",
            "Apex Diagnostics Laboratory",
            "2025-11-20",
            "Normal automated differential. White blood cell count, red cell indices, and platelet morphology show no abnormalities.",
            "Hematology",
            json.dumps({
                "WBC Count": "6.2 x10^3/uL",
                "Hemoglobin": "13.8 g/dL",
                "Hematocrit": "41.2%",
                "Platelet Count": "245 x10^3/uL",
                "Neutrophils": "58%"
            }),
            json.dumps([]),
            "verified",
            "provider",
            "PROV-301",
            "2025-11-20 10:45:00Z"
        ),
        (
            "REC-008",
            "PAT-101",
            "vital_signs",
            "Home Blood Pressure Log (Self-Monitored)",
            "Patient Self-Report",
            "2025-08-15",
            "Self-monitored 7-day morning averages using FDA-cleared Omron upper-arm cuff. Measurements logged prior to morning coffee.",
            "Cardiovascular Monitoring",
            json.dumps({"avg_systolic": "134 mmHg", "avg_diastolic": "85 mmHg", "avg_pulse": "71 bpm"}),
            json.dumps([]),
            "self_reported",
            "patient",
            "PAT-101",
            "2025-08-15 08:30:00Z"
        ),

        # Patient 102 - Robert Chen
        (
            "REC-009",
            "PAT-102",
            "visit",
            "Endocrinology Follow-up & Diabetes Review",
            "Metro Health Clinic",
            "2026-03-01",
            "Evaluation of glycemic control. Patient adhering to Mediterranean diet plan and daily walks. Foot inspection unremarkable with intact monofilament sensation.",
            "Endocrinology",
            json.dumps({"hba1c": "7.1%", "fasting_glucose": "128 mg/dL", "blood_pressure": "126/80 mmHg"}),
            json.dumps([]),
            "verified",
            "doctor",
            "DOC-202",
            "2026-03-01 10:15:00Z"
        ),
        (
            "REC-010",
            "PAT-102",
            "prescription",
            "Metformin HCl 500mg Extended Release",
            "Metro Health Clinic",
            "2026-03-01",
            "Take two tablets orally once daily with the evening meal. Monitor for gastrointestinal tolerance.",
            "Endocrinology",
            json.dumps({"supply": "90 days", "refills": "3"}),
            json.dumps([{"name": "Metformin ER", "dosage": "1000mg total", "frequency": "Daily with dinner"}]),
            "verified",
            "doctor",
            "DOC-202",
            "2026-03-01 10:25:00Z"
        ),
        (
            "REC-011",
            "PAT-102",
            "lab_report",
            "Hemoglobin A1c and Renal Function Panel",
            "Apex Diagnostics Laboratory",
            "2026-02-14",
            "HbA1c shows stable glycemic control at 7.1%. eGFR > 90 mL/min/1.73m2, indicating preserved renal clearance.",
            "Biochemistry",
            json.dumps({
                "HbA1c": "7.1%",
                "Estimated Avg Glucose": "157 mg/dL",
                "Serum Creatinine": "0.92 mg/dL",
                "eGFR": ">90 mL/min",
                "Microalbumin/Creatinine Ratio": "14 mg/g (Normal)"
            }),
            json.dumps([]),
            "verified",
            "provider",
            "PROV-301",
            "2026-02-14 11:00:00Z"
        ),

        # Patient 103 - Elena Rodriguez
        (
            "REC-012",
            "PAT-103",
            "visit",
            "Pre-Employment Occupational Health Screening",
            "St. Jude Medical Center",
            "2026-02-28",
            "Physical examination, audiometry, and vision check for hospital administrative role. Fit for all regular duties without restrictions.",
            "Occupational Medicine",
            json.dumps({"vision": "20/20 corrected", "blood_pressure": "118/74 mmHg", "pulse": "68 bpm"}),
            json.dumps([]),
            "verified",
            "doctor",
            "DOC-201",
            "2026-02-28 09:30:00Z"
        )
    ]

    cursor.executemany("""
        INSERT INTO medical_records (
            id, patient_id, record_type, title, provider_name, date, description,
            category, metrics, medications, verification_status, created_by_role, created_by_id, created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, records)

    # 3. Seed Access Requests
    # One pending request from Dr. Marcus Vance to Sarah Jenkins (PAT-101) to demonstrate the consent approval workflow
    # One approved request for Dr. Aisha Patel on PAT-102
    requests = [
        (
            "REQ-101",
            "PAT-101",
            "DOC-201",
            "Dr. Marcus Vance",
            "doctor",
            "City Heart Institute / St. Jude Cardiology",
            "Cardiovascular risk assessment, medication interaction audit, and follow-up cardiology evaluation following elevated BP referrals.",
            "all_records",
            "pending",
            None,
            None,
            "2026-03-19 08:30:00Z"
        ),
        (
            "REQ-102",
            "PAT-102",
            "DOC-202",
            "Dr. Aisha Patel",
            "doctor",
            "Metro Health Endocrinology Clinic",
            "Management of Type 2 Diabetes and quarterly metabolic panel review.",
            "all_records",
            "approved",
            "2026-03-01 09:00:00Z",
            "Patient granted full 1-year access during clinic check-in.",
            "2026-03-01 08:45:00Z"
        ),
        (
            "REQ-103",
            "PAT-101",
            "PROV-301",
            "Apex Diagnostics Laboratory",
            "provider",
            "Apex Diagnostics Central Facility",
            "Diagnostic historical baseline comparison for future hematology tests.",
            "lab_reports",
            "pending",
            None,
            None,
            "2026-03-20 10:15:00Z"
        )
    ]

    cursor.executemany("""
        INSERT INTO access_requests (
            id, patient_id, requester_id, requester_name, requester_role, requester_organization,
            reason, scope, status, decision_date, decision_notes, created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, requests)

    # 4. Seed Active Permissions
    permissions = [
        (
            "PERM-201",
            "PAT-102",
            "DOC-202",
            "Dr. Aisha Patel",
            "doctor",
            "all_records",
            "active",
            "2026-03-01 09:00:00Z",
            None
        )
    ]

    cursor.executemany("""
        INSERT INTO access_permissions (
            id, patient_id, grantee_id, grantee_name, grantee_role, scope, status, granted_at, revoked_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, permissions)

    # 5. Seed Audit Logs
    audit_logs = [
        (
            "AUD-INIT-001",
            "SYSTEM_SEED",
            "system",
            "SYS-SEED",
            "MedIt System",
            "PAT-101",
            "System database initialized with unified health record timeline",
            "2026-03-18 08:00:00Z"
        ),
        (
            "AUD-INIT-002",
            "RECORD_CREATED",
            "doctor",
            "DOC-201",
            "Dr. Marcus Vance",
            "PAT-101",
            "Doctor authored clinical prescription record for Lisinopril 10mg",
            "2026-03-18 14:45:00Z"
        ),
        (
            "AUD-INIT-003",
            "ACCESS_REQUESTED",
            "doctor",
            "DOC-201",
            "Dr. Marcus Vance",
            "PAT-101",
            "Access request REQ-101 submitted for full clinical record access",
            "2026-03-19 08:30:00Z"
        ),
        (
            "AUD-INIT-004",
            "CONSENT_GRANTED",
            "patient",
            "PAT-102",
            "Robert Chen",
            "PAT-102",
            "Patient authorized Dr. Aisha Patel for comprehensive clinical record access (PERM-201)",
            "2026-03-01 09:00:00Z"
        )
    ]

    cursor.executemany("""
        INSERT INTO audit_logs (id, action, actor_role, actor_id, actor_name, patient_id, details, timestamp)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """, audit_logs)

    conn.commit()
    conn.close()
    print("Seeded database with realistic fictional medical records and demo consent workflows.")

if __name__ == "__main__":
    seed_database()
