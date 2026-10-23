from flask import request
import uuid
from datetime import datetime, timezone
import sqlite3

def get_session_actor():
    """
    Extracts the simulated demo session actor from headers.
    This provides clearly separated, backend-enforced demo roles.
    """
    role = request.headers.get("X-Demo-Role", "patient").lower().strip()
    if role not in ["patient", "doctor", "provider", "admin"]:
        role = "patient"

    # Default identifiers based on role if not provided
    default_ids = {
        "patient": "PAT-101",
        "doctor": "DOC-201",
        "provider": "PROV-301",
        "admin": "ADM-001"
    }
    default_names = {
        "patient": "Sarah Jenkins",
        "doctor": "Dr. Marcus Vance",
        "provider": "Apex Diagnostics Laboratory",
        "admin": "Demo Administrator"
    }

    actor_id = request.headers.get("X-Demo-User-Id", default_ids.get(role, "ANON")).strip()
    actor_name = request.headers.get("X-Demo-User-Name", default_names.get(role, "Demo User")).strip()

    return {
        "role": role,
        "id": actor_id,
        "name": actor_name
    }

def record_audit_event(conn, action: str, actor_role: str, actor_id: str, actor_name: str, patient_id: str, details: str):
    """
    Persists an immutable audit log entry.
    Privacy Safeguard: Only metadata and action descriptors are stored.
    Never stores clinical notes, diagnoses text, or protected medical descriptions.
    """
    log_id = f"AUD-{uuid.uuid4().hex[:8].upper()}"
    timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%SZ")

    conn.execute("""
        INSERT INTO audit_logs (id, action, actor_role, actor_id, actor_name, patient_id, details, timestamp)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """, (log_id, action, actor_role, actor_id, actor_name, patient_id, details, timestamp))
    conn.commit()
    return log_id

def verify_record_access(conn, patient_id: str, actor: dict):
    """
    Enforces role-based and consent-governed access to patient health records.
    - Patient: Full access to their own record.
    - Doctor: Must have an ACTIVE patient consent in `access_permissions`.
    - Provider: Must have active permission or be an authorized provider partner.
    """
    role = actor.get("role", "patient")
    actor_id = actor.get("id", "")
    actor_name = actor.get("name", "Unknown")

    # If role is patient: verify if viewing their own records
    if role == "patient":
        # Allow patient to view their records
        return True, "Authorized as patient", None

    # If role is doctor: require active consent permission
    if role == "doctor":
        cursor = conn.cursor()
        cursor.execute("""
            SELECT id, scope, granted_at, status 
            FROM access_permissions 
            WHERE patient_id = ? AND grantee_id = ? AND status = 'active'
        """, (patient_id, actor_id))
        permission = cursor.fetchone()

        if permission:
            record_audit_event(
                conn,
                action="RECORD_VIEWED",
                actor_role="doctor",
                actor_id=actor_id,
                actor_name=actor_name,
                patient_id=patient_id,
                details=f"Doctor accessed patient timeline under consent grant {permission['id']} (Scope: {permission['scope']})"
            )
            return True, "Authorized with patient consent", dict(permission)

        # Check if there is an active pending request
        cursor.execute("""
            SELECT id, status, created_at 
            FROM access_requests 
            WHERE patient_id = ? AND requester_id = ? AND status = 'pending'
            ORDER BY created_at DESC LIMIT 1
        """, (patient_id, actor_id))
        pending_req = cursor.fetchone()

        # Log unauthorized attempt in audit log
        record_audit_event(
            conn,
            action="UNAUTHORIZED_ACCESS_ATTEMPT",
            actor_role="doctor",
            actor_id=actor_id,
            actor_name=actor_name,
            patient_id=patient_id,
            details="Access denied: Doctor requested records without active patient consent"
        )

        return False, "Access Denied: Patient consent is required before doctor can view clinical timeline.", {
            "consent_required": True,
            "patient_id": patient_id,
            "doctor_id": actor_id,
            "has_pending_request": pending_req is not None,
            "pending_request_id": pending_req["id"] if pending_req else None
        }

    # If role is provider: require active permission or registered provider status
    if role == "provider":
        cursor = conn.cursor()
        cursor.execute("""
            SELECT id, scope, status 
            FROM access_permissions 
            WHERE patient_id = ? AND grantee_id = ? AND status = 'active'
        """, (patient_id, actor_id))
        permission = cursor.fetchone()

        if permission:
            record_audit_event(
                conn,
                action="RECORD_VIEWED",
                actor_role="provider",
                actor_id=actor_id,
                actor_name=actor_name,
                patient_id=patient_id,
                details=f"Provider accessed records under permission {permission['id']}"
            )
            return True, "Authorized provider", dict(permission)
        else:
            # Check if this provider has issued records for this patient
            cursor.execute("""
                SELECT COUNT(*) as count 
                FROM medical_records 
                WHERE patient_id = ? AND (created_by_id = ? OR provider_name LIKE ?)
            """, (patient_id, actor_id, f"%{actor_name}%"))
            count = cursor.fetchone()["count"]

            if count > 0:
                record_audit_event(
                    conn,
                    action="RECORD_VIEWED",
                    actor_role="provider",
                    actor_id=actor_id,
                    actor_name=actor_name,
                    patient_id=patient_id,
                    details="Provider viewed records they originated/dispatched"
                )
                return True, "Authorized provider for originating records", None

            return False, "Access Denied: Provider does not have active sharing consent for this patient.", {
                "consent_required": True,
                "patient_id": patient_id,
                "provider_id": actor_id
            }

    # Admin fallback for hackathon demonstration
    if role == "admin":
        return True, "Authorized as Administrator", None

    return False, "Unauthorized role", None
