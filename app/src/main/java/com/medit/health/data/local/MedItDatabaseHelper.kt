package com.medit.health.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.medit.health.data.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

class MedItDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL,
                role TEXT NOT NULL,
                associated_id TEXT NOT NULL,
                organization TEXT,
                specialty TEXT
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE patients (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                dob TEXT NOT NULL,
                gender TEXT NOT NULL,
                blood_type TEXT NOT NULL,
                phone TEXT,
                email TEXT,
                allergies TEXT,
                chronic_conditions TEXT,
                emergency_contact TEXT,
                abha_id TEXT,
                created_at TEXT
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE medical_records (
                id TEXT PRIMARY KEY,
                patient_id TEXT NOT NULL,
                record_type TEXT NOT NULL,
                title TEXT NOT NULL,
                provider_name TEXT NOT NULL,
                doctor_name TEXT,
                doctor_specialty TEXT,
                date TEXT NOT NULL,
                description TEXT NOT NULL,
                category TEXT,
                metrics TEXT,
                medications TEXT,
                verification_status TEXT,
                created_by_role TEXT,
                created_by_id TEXT,
                follow_up_date TEXT,
                attachment_path TEXT,
                attachment_type TEXT,
                ocr_processed INTEGER DEFAULT 0,
                cost_inr REAL DEFAULT 0.0,
                created_at TEXT
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE access_requests (
                id TEXT PRIMARY KEY,
                patient_id TEXT NOT NULL,
                requester_id TEXT NOT NULL,
                requester_name TEXT NOT NULL,
                requester_role TEXT NOT NULL,
                requester_organization TEXT,
                reason TEXT NOT NULL,
                scope TEXT,
                status TEXT NOT NULL,
                created_at TEXT,
                decision_date TEXT,
                decision_notes TEXT
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE access_permissions (
                id TEXT PRIMARY KEY,
                patient_id TEXT NOT NULL,
                grantee_id TEXT NOT NULL,
                grantee_name TEXT NOT NULL,
                grantee_role TEXT NOT NULL,
                scope TEXT,
                status TEXT NOT NULL,
                granted_at TEXT,
                revoked_at TEXT
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE audit_logs (
                id TEXT PRIMARY KEY,
                timestamp TEXT NOT NULL,
                action TEXT NOT NULL,
                actor_role TEXT NOT NULL,
                actor_id TEXT NOT NULL,
                actor_name TEXT NOT NULL,
                patient_id TEXT NOT NULL,
                details TEXT NOT NULL
            );
            """.trimIndent()
        )

        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS audit_logs")
        db.execSQL("DROP TABLE IF EXISTS access_permissions")
        db.execSQL("DROP TABLE IF EXISTS access_requests")
        db.execSQL("DROP TABLE IF EXISTS medical_records")
        db.execSQL("DROP TABLE IF EXISTS patients")
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        // 1. Seed Users
        // Doctor: admin123 / Doctor@MedIt2026
        val docHash = hashPassword("Doctor@MedIt2026")
        db.execSQL(
            """
            INSERT INTO users (id, username, password_hash, full_name, email, role, associated_id, organization, specialty)
            VALUES ('USR-DOC-01', 'admin123', '$docHash', 'Dr. Ananya Sharma', 'ananya.sharma@apollohospitals.org', 'DOCTOR', 'DOC-201', 'Apollo Hospitals & Heart Institute', 'Senior Interventional Cardiologist');
            """.trimIndent()
        )

        // Patient: sarah.jenkins / patient123 -> mapped to PAT-101 (Rajesh Kumar)
        val patHash = hashPassword("patient123")
        db.execSQL(
            """
            INSERT INTO users (id, username, password_hash, full_name, email, role, associated_id, organization, specialty)
            VALUES ('USR-PAT-01', 'sarah.jenkins', '$patHash', 'Rajesh Kumar', 'rajesh.kumar@medit-demo.org', 'PATIENT', 'PAT-101', '', '');
            """.trimIndent()
        )

        // Secondary Patient: vikram.sharma / patient123
        db.execSQL(
            """
            INSERT INTO users (id, username, password_hash, full_name, email, role, associated_id, organization, specialty)
            VALUES ('USR-PAT-02', 'vikram.sharma', '$patHash', 'Vikram Sharma', 'vikram.sharma@medit-demo.org', 'PATIENT', 'PAT-102', '', '');
            """.trimIndent()
        )

        // Provider: apex.labs / provider123
        val provHash = hashPassword("provider123")
        db.execSQL(
            """
            INSERT INTO users (id, username, password_hash, full_name, email, role, associated_id, organization, specialty)
            VALUES ('USR-PROV-01', 'apex.labs', '$provHash', 'Apex Diagnostics Lab', 'records@apexdiagnostics.org', 'PROVIDER', 'PROV-301', 'Apex Clinical Laboratories Network', 'Diagnostic Pathology & Imaging');
            """.trimIndent()
        )

        // 2. Seed Patients
        db.execSQL(
            """
            INSERT INTO patients (id, name, dob, gender, blood_type, phone, email, allergies, chronic_conditions, emergency_contact, abha_id, created_at)
            VALUES (
                'PAT-101',
                'Rajesh Kumar',
                '1988-04-12',
                'Male',
                'O+',
                '+91 98201 44521',
                'rajesh.kumar@medit-demo.org',
                '["Penicillin", "Amoxicillin"]',
                '["Mild Bronchial Asthma", "Essential Hypertension (Stage 1)"]',
                'Sunita Kumar (Spouse) - +91 98201 44522',
                '91-4452-9812-4019',
                '2024-01-10 09:00:00Z'
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO patients (id, name, dob, gender, blood_type, phone, email, allergies, chronic_conditions, emergency_contact, abha_id, created_at)
            VALUES (
                'PAT-102',
                'Vikram Sharma',
                '1972-09-24',
                'Male',
                'A+',
                '+91 98110 33451',
                'vikram.sharma@medit-demo.org',
                '["Sulfa antibiotics"]',
                '["Type 2 Diabetes Mellitus", "Hyperlipidemia"]',
                'Meera Sharma (Sister) - +91 98110 33452',
                '91-3321-7764-1102',
                '2024-02-15 11:30:00Z'
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO patients (id, name, dob, gender, blood_type, phone, email, allergies, chronic_conditions, emergency_contact, abha_id, created_at)
            VALUES (
                'PAT-103',
                'Priya Sundaram',
                '1995-11-03',
                'Female',
                'B+',
                '+91 98402 77810',
                'priya.sundaram@medit-demo.org',
                '["Latex"]',
                '["None"]',
                'Karthik Sundaram (Brother) - +91 98402 77811',
                '91-6654-2231-9087',
                '2024-03-01 14:00:00Z'
            );
            """.trimIndent()
        )

        // 3. Seed Medical Records for PAT-101
        val pat101Meds = json.encodeToString(
            listOf(
                MedicationItem("Telmisartan", "40mg", "Once daily morning after food", "30 Days", "Check home BP weekly", 185.0),
                MedicationItem("Amlodipine", "5mg", "Once daily morning", "30 Days", "Take with water", 110.0)
            )
        )
        val pat101Metrics1 = json.encodeToString(mapOf("Clinic BP" to "142/90 mmHg", "Heart Rate" to "72 bpm", "ECG" to "Normal Sinus Rhythm"))
        db.execSQL(
            """
            INSERT INTO medical_records (
                id, patient_id, record_type, title, provider_name, doctor_name, doctor_specialty,
                date, description, category, metrics, medications, verification_status,
                created_by_role, created_by_id, follow_up_date, cost_inr, created_at
            ) VALUES (
                'REC-001',
                'PAT-101',
                'prescription',
                'Antihypertensive Combination Therapy (Telmisartan + Amlodipine)',
                'Apollo Hospitals, New Delhi',
                'Dr. Ananya Sharma',
                'Interventional Cardiology',
                '2026-03-18',
                'Initiated dual therapy for persistent Stage 1 Essential Hypertension. Emphasized dietary sodium restriction, 30 min daily brisk walking, and avoidance of NSAIDs.',
                'Cardiology',
                '$pat101Metrics1',
                '$pat101Meds',
                'verified',
                'doctor',
                'DOC-201',
                '2026-04-18',
                295.0,
                '2026-03-18 14:45:00Z'
            );
            """.trimIndent()
        )

        val pat101Metrics2 = json.encodeToString(
            mapOf("Total Cholesterol" to "218 mg/dL", "LDL-C" to "138 mg/dL", "HDL-C" to "44 mg/dL", "Triglycerides" to "178 mg/dL", "HbA1c" to "5.8%")
        )
        db.execSQL(
            """
            INSERT INTO medical_records (
                id, patient_id, record_type, title, provider_name, doctor_name, doctor_specialty,
                date, description, category, metrics, medications, verification_status,
                created_by_role, created_by_id, follow_up_date, cost_inr, created_at
            ) VALUES (
                'REC-002',
                'PAT-101',
                'lab_report',
                'Comprehensive Lipid Profile & Fasting Glycemic Panel',
                'Dr. Lal PathLabs & Diagnostics',
                'Dr. S. K. Sen',
                'Clinical Pathology',
                '2026-03-10',
                'Fasting lipid panel demonstrates borderline elevated LDL and moderate hypertriglyceridemia. Glycemic control is within pre-diabetic target range (HbA1c 5.8%). Follow dietary modifications.',
                'Hematology & Biochemistry',
                '$pat101Metrics2',
                '[]',
                'verified',
                'provider',
                'PROV-301',
                '2026-06-10',
                1250.0,
                '2026-03-10 11:20:00Z'
            );
            """.trimIndent()
        )

        val inhalerMeds = json.encodeToString(
            listOf(
                MedicationItem("Budesonide + Formoterol", "200/6 mcg", "Two puffs twice daily", "30 Days", "Rinse mouth thoroughly with water after inhalation", 620.0),
                MedicationItem("Levocetirizine", "5mg", "Once daily at bedtime as needed", "10 Days", "For seasonal allergic flare-ups", 85.0)
            )
        )
        db.execSQL(
            """
            INSERT INTO medical_records (
                id, patient_id, record_type, title, provider_name, doctor_name, doctor_specialty,
                date, description, category, metrics, medications, verification_status,
                created_by_role, created_by_id, follow_up_date, cost_inr, created_at
            ) VALUES (
                'REC-003',
                'PAT-101',
                'prescription',
                'Inhaled Corticosteroid & LABA Maintenance Inhaler',
                'Fortis Escorts Heart & Lung Institute',
                'Dr. Priya Sundaram',
                'Pulmonology',
                '2025-11-05',
                'Prescribed maintenance inhaler for moderate seasonal asthma exacerbation triggered by winter air pollution in Delhi NCR. Peak Expiratory Flow rate improved from 320 to 440 L/min post-bronchodilator.',
                'Pulmonology',
                '{"Pre-PEFR": "320 L/min", "Post-PEFR": "440 L/min", "SpO2": "98% on room air"}',
                '$inhalerMeds',
                'verified',
                'doctor',
                'DOC-203',
                '2025-12-05',
                705.0,
                '2025-11-05 16:15:00Z'
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO medical_records (
                id, patient_id, record_type, title, provider_name, doctor_name, doctor_specialty,
                date, description, category, metrics, medications, verification_status,
                created_by_role, created_by_id, follow_up_date, cost_inr, created_at
            ) VALUES (
                'REC-004',
                'PAT-101',
                'vital_signs',
                'Self-Reported Morning Blood Pressure & Vitals Log',
                'Patient Self-Report (Omron Home Monitor)',
                'Rajesh Kumar (Patient)',
                '',
                '2026-03-24',
                'Recorded resting sitting BP after 5 minutes quiet rest. Average of two readings taken 2 minutes apart. Patient feeling energetic with no postural dizziness or cough.',
                'General Medicine',
                '{"Systolic BP": "128 mmHg", "Diastolic BP": "82 mmHg", "Pulse": "68 bpm", "Weight": "74.5 kg"}',
                '[]',
                'self_reported',
                'patient',
                'PAT-101',
                '',
                0.0,
                '2026-03-24 07:30:00Z'
            );
            """.trimIndent()
        )

        // 4. Seed Permissions
        // Doctor Ananya Sharma has active consent for PAT-101
        db.execSQL(
            """
            INSERT INTO access_permissions (
                id, patient_id, grantee_id, grantee_name, grantee_role, scope, status, granted_at
            ) VALUES (
                'PERM-001',
                'PAT-101',
                'DOC-201',
                'Dr. Ananya Sharma',
                'doctor',
                'all_records',
                'active',
                '2026-03-18 14:00:00Z'
            );
            """.trimIndent()
        )

        // 5. Seed Access Requests
        // Dr. Rajesh Gupta (DOC-202) has a pending request for PAT-101
        db.execSQL(
            """
            INSERT INTO access_requests (
                id, patient_id, requester_id, requester_name, requester_role,
                requester_organization, reason, scope, status, created_at
            ) VALUES (
                'REQ-101',
                'PAT-101',
                'DOC-202',
                'Dr. Rajesh Gupta',
                'doctor',
                'Max Super Speciality Hospital, Saket',
                'Pre-consultation cardiac evaluation and clinical medication reconciliation.',
                'all_records',
                'pending',
                '2026-03-24 10:15:00Z'
            );
            """.trimIndent()
        )

        // 6. Seed Audit Logs
        db.execSQL(
            """
            INSERT INTO audit_logs (id, timestamp, action, actor_role, actor_id, actor_name, patient_id, details)
            VALUES (
                'AUD-01',
                '2026-03-18 14:00:00Z',
                'CONSENT_GRANTED',
                'patient',
                'PAT-101',
                'Rajesh Kumar',
                'PAT-101',
                'Patient approved digital clinical record consent PERM-001 for Dr. Ananya Sharma'
            );
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO audit_logs (id, timestamp, action, actor_role, actor_id, actor_name, patient_id, details)
            VALUES (
                'AUD-02',
                '2026-03-18 14:35:00Z',
                'RECORD_VIEWED',
                'doctor',
                'DOC-201',
                'Dr. Ananya Sharma',
                'PAT-101',
                'Dr. Ananya Sharma accessed unified chronological medical timeline under active consent'
            );
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO audit_logs (id, timestamp, action, actor_role, actor_id, actor_name, patient_id, details)
            VALUES (
                'AUD-03',
                '2026-03-24 10:15:00Z',
                'ACCESS_REQUESTED',
                'doctor',
                'DOC-202',
                'Dr. Rajesh Gupta',
                'PAT-101',
                'Dr. Rajesh Gupta submitted access request REQ-101 for cardiac clinical history'
            );
            """.trimIndent()
        )
    }

    companion object {
        const val DATABASE_NAME = "medit_records.db"
        const val DATABASE_VERSION = 1

        @Volatile
        private var instance: MedItDatabaseHelper? = null

        fun getInstance(context: Context): MedItDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: MedItDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }
}
