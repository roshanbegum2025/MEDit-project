import sqlite3
import os
import json
from pathlib import Path

DB_FILE = os.environ.get("MEDIT_DB_PATH", str(Path(__file__).parent / "medit.db"))

def get_db(db_path=None):
    target = db_path or DB_FILE
    conn = sqlite3.connect(target)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON;")
    return conn

def init_db(db_path=None):
    conn = get_db(db_path)
    cursor = conn.cursor()

    # Patients table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS patients (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        dob TEXT NOT NULL,
        gender TEXT NOT NULL,
        blood_type TEXT NOT NULL,
        phone TEXT,
        email TEXT,
        allergies TEXT, -- JSON array of strings
        chronic_conditions TEXT, -- JSON array of strings
        emergency_contact TEXT,
        created_at TEXT NOT NULL
    );
    """)

    # Medical Records table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS medical_records (
        id TEXT PRIMARY KEY,
        patient_id TEXT NOT NULL,
        record_type TEXT NOT NULL, -- 'visit', 'diagnosis', 'prescription', 'allergy', 'lab_report', 'vital_signs'
        title TEXT NOT NULL,
        provider_name TEXT NOT NULL,
        date TEXT NOT NULL, -- YYYY-MM-DD
        description TEXT NOT NULL,
        category TEXT, -- e.g. Cardiology, Hematology, General Practice
        metrics TEXT, -- JSON object, e.g. {"bp": "120/80", "glucose": "95"}
        medications TEXT, -- JSON array of medications
        verification_status TEXT NOT NULL, -- 'verified', 'self_reported', 'pending_review'
        created_by_role TEXT NOT NULL, -- 'patient', 'doctor', 'provider'
        created_by_id TEXT,
        created_at TEXT NOT NULL,
        FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
    );
    """)

    # Access Requests table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS access_requests (
        id TEXT PRIMARY KEY,
        patient_id TEXT NOT NULL,
        requester_id TEXT NOT NULL,
        requester_name TEXT NOT NULL,
        requester_role TEXT NOT NULL, -- 'doctor', 'provider'
        requester_organization TEXT,
        reason TEXT NOT NULL,
        scope TEXT NOT NULL, -- 'all_records', 'lab_reports', 'prescriptions', etc.
        status TEXT NOT NULL DEFAULT 'pending', -- 'pending', 'approved', 'rejected'
        decision_date TEXT,
        decision_notes TEXT,
        created_at TEXT NOT NULL,
        FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
    );
    """)

    # Access Permissions table (granted consent grants)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS access_permissions (
        id TEXT PRIMARY KEY,
        patient_id TEXT NOT NULL,
        grantee_id TEXT NOT NULL,
        grantee_name TEXT NOT NULL,
        grantee_role TEXT NOT NULL,
        scope TEXT NOT NULL,
        status TEXT NOT NULL DEFAULT 'active', -- 'active', 'revoked', 'expired'
        granted_at TEXT NOT NULL,
        revoked_at TEXT,
        FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
    );
    """)

    # Audit Logs table
    # Privacy safeguard: logs action metadata and actor identity, NEVER raw clinical notes or protected health descriptions
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS audit_logs (
        id TEXT PRIMARY KEY,
        action TEXT NOT NULL, -- 'PATIENT_SEARCH', 'RECORD_VIEWED', 'RECORD_CREATED', 'ACCESS_REQUESTED', 'CONSENT_GRANTED', 'CONSENT_DENIED', 'CONSENT_REVOKED', 'RECORD_SHARED'
        actor_role TEXT NOT NULL, -- 'patient', 'doctor', 'provider', 'system'
        actor_id TEXT NOT NULL,
        actor_name TEXT NOT NULL,
        patient_id TEXT NOT NULL,
        details TEXT NOT NULL,
        timestamp TEXT NOT NULL
    );
    """)

    # Useful indexes for query performance
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_records_patient_date ON medical_records(patient_id, date DESC);")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_requests_patient ON access_requests(patient_id, status);")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_permissions_patient_grantee ON access_permissions(patient_id, grantee_id, status);")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_audit_patient_time ON audit_logs(patient_id, timestamp DESC);")

    conn.commit()
    conn.close()

if __name__ == "__main__":
    init_db()
    print("Database schema initialized successfully.")
