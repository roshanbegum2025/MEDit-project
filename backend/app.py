import os
import json
import uuid
from datetime import datetime, timezone
from flask import Flask, request, jsonify
from flask_cors import CORS
from pydantic import ValidationError

from database import get_db, init_db, DB_FILE
from models import (
    PatientCreateModel,
    RecordCreateModel,
    AccessRequestCreateModel,
    ConsentDecisionModel
)
from auth_context import (
    get_session_actor,
    verify_record_access,
    record_audit_event
)

app = Flask(__name__)
# Enable CORS for all routes so frontend dev server can interact with backend
CORS(app, resources={r"/api/*": {"origins": "*"}}, supports_credentials=True)

# Ensure database is initialized at startup
init_db()

@app.route("/api/health", methods=["GET"])
def health_check():
    return jsonify({
        "status": "healthy",
        "service": "MedIt Unified Patient Health Records API",
        "version": "1.0.0-hackathon",
        "demo_mode": False,
        "database": "sqlite",
        "timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    })

# -------------------------------------------------------------
# Role-Aware Authentication Endpoints (MANDATORY REQUIREMENT)
# -------------------------------------------------------------

DEMO_ACCOUNTS = {
    "admin123": {
        "password": "Doctor@MedIt2026",
        "role": "doctor",
        "id": "DOC-201",
        "name": "Dr. Ananya Sharma",
        "email": "ananya.sharma@apollohospitals.org",
        "organization": "Apollo Hospitals & Heart Institute",
        "specialty": "Senior Interventional Cardiologist"
    },
    "sarah.jenkins": {
        "password": "patient123",
        "role": "patient",
        "id": "PAT-101",
        "name": "Rajesh Kumar",
        "email": "rajesh.kumar@medit-demo.org",
        "organization": "",
        "specialty": ""
    },
    "vikram.sharma": {
        "password": "patient123",
        "role": "patient",
        "id": "PAT-102",
        "name": "Vikram Sharma",
        "email": "vikram.sharma@medit-demo.org",
        "organization": "",
        "specialty": ""
    },
    "apex.labs": {
        "password": "provider123",
        "role": "provider",
        "id": "PROV-301",
        "name": "Apex Diagnostics Lab",
        "email": "records@apexdiagnostics.org",
        "organization": "Apex Clinical Laboratories Network",
        "specialty": "Diagnostic Pathology & Imaging"
    }
}

@app.route("/api/auth/login", methods=["POST"])
def auth_login():
    data = request.get_json(force=True, silent=True) or {}
    username = data.get("username", "").strip()
    password = data.get("password", "").strip()

    if not username or not password:
        return jsonify({"error": "Please provide both username and password"}), 400

    account = DEMO_ACCOUNTS.get(username)
    if not account or account["password"] != password:
        return jsonify({"error": "Invalid username or password. Please verify credentials."}), 401

    conn = get_db()
    record_audit_event(
        conn,
        action="LOGIN",
        actor_role=account["role"],
        actor_id=account["id"],
        actor_name=account["name"],
        patient_id=account["id"] if account["role"] == "patient" else "N/A",
        details=f"User {account['name']} logged in as {account['role'].upper()}"
    )
    conn.close()

    return jsonify({
        "message": "Authentication successful",
        "user": {
            "id": account["id"],
            "username": username,
            "role": account["role"],
            "name": account["name"],
            "email": account["email"],
            "organization": account["organization"],
            "specialty": account["specialty"]
        }
    })

@app.route("/api/auth/register", methods=["POST"])
def auth_register():
    data = request.get_json(force=True, silent=True) or {}
    name = data.get("name", "").strip()
    username = data.get("username", "").strip()
    password = data.get("password", "").strip()
    dob = data.get("dob", "1992-06-15").strip()
    gender = data.get("gender", "Female").strip()
    blood_type = data.get("blood_type", "O+").strip()
    phone = data.get("phone", "").strip()
    emergency_contact = data.get("emergency_contact", "").strip()
    abha_id = data.get("abha_id", f"91-{uuid.uuid4().hex[:4]}-{uuid.uuid4().hex[:4]}-{uuid.uuid4().hex[:4]}").strip()

    if not name or not username or not password:
        return jsonify({"error": "Name, username, and password are required"}), 400

    if username in DEMO_ACCOUNTS:
        return jsonify({"error": f"Username '{username}' is already registered."}), 409

    patient_id = f"PAT-{uuid.uuid4().hex[:3].upper()}"
    created_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%SZ")

    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("""
        INSERT INTO patients (id, name, dob, gender, blood_type, phone, email, allergies, chronic_conditions, emergency_contact, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, '[]', '[]', ?, ?)
    """, (patient_id, name, dob, gender, blood_type, phone, f"{username}@medit-demo.org", emergency_contact, created_at))

    # Add to in-memory accounts table
    DEMO_ACCOUNTS[username] = {
        "password": password,
        "role": "patient",
        "id": patient_id,
        "name": name,
        "email": f"{username}@medit-demo.org",
        "organization": "",
        "specialty": ""
    }

    record_audit_event(
        conn,
        action="ACCOUNT_CREATED",
        actor_role="patient",
        actor_id=patient_id,
        actor_name=name,
        patient_id=patient_id,
        details=f"New patient account created with MedIt ID {patient_id} and ABHA ID {abha_id}"
    )
    conn.commit()
    conn.close()

    return jsonify({
        "message": "Patient account registered successfully",
        "user": {
            "id": patient_id,
            "username": username,
            "role": "patient",
            "name": name,
            "email": f"{username}@medit-demo.org",
            "organization": "",
            "specialty": "",
            "abha_id": abha_id
        }
    }), 201

# -------------------------------------------------------------
# Prescription OCR Engine Endpoint (MANDATORY REQUIREMENT)
# -------------------------------------------------------------

@app.route("/api/prescriptions/ocr", methods=["POST"])
def ocr_prescription():
    """
    Simulates optical character recognition extraction from uploaded doctor prescription image or document.
    Extracts medicines, dosages, instructions, clinic, and flags uncertainties for review.
    """
    # Fallback & simulated OCR analysis
    ocr_result = {
        "title": "Prescription - Essential Hypertension & Dyslipidemia",
        "provider_name": "Apollo Hospitals, New Delhi",
        "doctor_name": "Dr. Ananya Sharma (MD, DM Cardiology)",
        "doctor_specialty": "Interventional Cardiology",
        "date": datetime.now(timezone.utc).strftime("%Y-%m-%d"),
        "category": "Cardiology",
        "diagnosis": "Stage 1 Essential Hypertension with borderline elevated LDL cholesterol. Advised lifestyle modifications, daily 30-minute brisk walk, and low sodium diet.",
        "medications": [
            {
                "name": "Telmisartan",
                "dosage": "40mg",
                "frequency": "Once daily morning after food",
                "duration": "30 Days",
                "instructions": "Monitor home BP weekly; report systolic > 140",
                "price_inr": 185.0
            },
            {
                "name": "Atorvastatin",
                "dosage": "10mg",
                "frequency": "Once daily at bedtime",
                "duration": "30 Days",
                "instructions": "Take after dinner",
                "price_inr": 210.0
            }
        ],
        "metrics": {
            "Clinic BP": "140/88 mmHg",
            "Pulse": "72 bpm"
        },
        "follow_up_date": "Review after 4 weeks with lipid profile",
        "confidence_score": 0.91,
        "uncertain_fields": [
            "Physician signature: Verified via Apollo Hospitals clinic seal",
            "Refill authorization: 1 repeat refill noted"
        ],
        "has_uncertainties": True,
        "disclaimer": "Extracted via OCR assistance. Not medically verified until confirmed by patient or clinician."
    }
    return jsonify(ocr_result)


# -------------------------------------------------------------
# Patients Endpoints
# -------------------------------------------------------------

@app.route("/api/patients", methods=["GET"])
def list_patients():
    query = request.args.get("search", "").strip().lower()
    actor = get_session_actor()
    conn = get_db()
    cursor = conn.cursor()

    if query:
        cursor.execute("""
            SELECT * FROM patients
            WHERE LOWER(id) LIKE ? OR LOWER(name) LIKE ? OR LOWER(email) LIKE ?
            ORDER BY name ASC
        """, (f"%{query}%", f"%{query}%", f"%{query}%"))
        
        record_audit_event(
            conn,
            action="PATIENT_SEARCH",
            actor_role=actor["role"],
            actor_id=actor["id"],
            actor_name=actor["name"],
            patient_id="ALL",
            details=f"Search executed for query '{query}'"
        )
    else:
        cursor.execute("SELECT * FROM patients ORDER BY name ASC")

    rows = cursor.fetchall()
    patients = []
    for r in rows:
        item = dict(r)
        item["allergies"] = json.loads(item["allergies"]) if item.get("allergies") else []
        item["chronic_conditions"] = json.loads(item["chronic_conditions"]) if item.get("chronic_conditions") else []
        patients.append(item)

    conn.close()
    return jsonify({"patients": patients, "count": len(patients)})

@app.route("/api/patients/<patient_id>", methods=["GET"])
def get_patient(patient_id):
    actor = get_session_actor()
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM patients WHERE id = ?", (patient_id,))
    row = cursor.fetchone()
    if not row:
        conn.close()
        return jsonify({"error": f"Patient with ID {patient_id} not found"}), 404

    patient = dict(row)
    patient["allergies"] = json.loads(patient["allergies"]) if patient.get("allergies") else []
    patient["chronic_conditions"] = json.loads(patient["chronic_conditions"]) if patient.get("chronic_conditions") else []

    record_audit_event(
        conn,
        action="PATIENT_PROFILE_VIEWED",
        actor_role=actor["role"],
        actor_id=actor["id"],
        actor_name=actor["name"],
        patient_id=patient_id,
        details="Patient demographic summary profile viewed"
    )

    conn.close()
    return jsonify(patient)

# -------------------------------------------------------------
# Unified Medical Records Timeline (Protected by Consent Check)
# -------------------------------------------------------------

@app.route("/api/patients/<patient_id>/records", methods=["GET"])
def get_patient_records(patient_id):
    """
    Returns unified chronological timeline of visits, diagnoses, prescriptions,
    allergies, lab reports, and vitals.
    CRITICAL: Enforces backend access authorization and simulated patient consent!
    """
    actor = get_session_actor()
    conn = get_db()

    # Verify patient exists
    cursor = conn.cursor()
    cursor.execute("SELECT id, name FROM patients WHERE id = ?", (patient_id,))
    patient = cursor.fetchone()
    if not patient:
        conn.close()
        return jsonify({"error": f"Patient with ID {patient_id} not found"}), 404

    # BACKEND ACCESS CONTROL CHECK
    has_access, message, access_metadata = verify_record_access(conn, patient_id, actor)

    if not has_access:
        conn.close()
        return jsonify({
            "error": message,
            "consent_required": access_metadata.get("consent_required", True) if access_metadata else True,
            "patient_id": patient_id,
            "patient_name": patient["name"],
            "doctor_id": actor["id"],
            "doctor_name": actor["name"],
            "has_pending_request": access_metadata.get("has_pending_request", False) if access_metadata else False,
            "pending_request_id": access_metadata.get("pending_request_id") if access_metadata else None
        }), 403

    # If authorized: fetch records with optional filtering
    record_type = request.args.get("type", "").strip().lower()
    provider = request.args.get("provider", "").strip().lower()
    search = request.args.get("search", "").strip().lower()
    start_date = request.args.get("start_date", "").strip()
    end_date = request.args.get("end_date", "").strip()

    sql = "SELECT * FROM medical_records WHERE patient_id = ?"
    params = [patient_id]

    if record_type and record_type != "all":
        sql += " AND record_type = ?"
        params.append(record_type)

    if provider and provider != "all":
        sql += " AND LOWER(provider_name) LIKE ?"
        params.append(f"%{provider}%")

    if start_date:
        sql += " AND date >= ?"
        params.append(start_date)

    if end_date:
        sql += " AND date <= ?"
        params.append(end_date)

    if search:
        sql += " AND (LOWER(title) LIKE ? OR LOWER(description) LIKE ? OR LOWER(category) LIKE ?)"
        term = f"%{search}%"
        params.extend([term, term, term])

    # Chronological sort: newest first for standard health timeline
    sql += " ORDER BY date DESC, created_at DESC"

    cursor.execute(sql, params)
    rows = cursor.fetchall()

    records = []
    for r in rows:
        item = dict(r)
        item["metrics"] = json.loads(item["metrics"]) if item.get("metrics") else {}
        item["medications"] = json.loads(item["medications"]) if item.get("medications") else []
        records.append(item)

    conn.close()
    return jsonify({
        "records": records,
        "count": len(records),
        "patient_id": patient_id,
        "access_grant": access_metadata
    })

@app.route("/api/records", methods=["POST"])
def create_medical_record():
    """
    Creates a new medical record with Pydantic schema validation.
    Allows patients, doctors, and providers to submit records.
    """
    body = request.get_json(force=True, silent=True)
    if not body:
        return jsonify({"error": "Invalid JSON body provided"}), 400

    try:
        record_data = RecordCreateModel(**body)
    except ValidationError as err:
        clean_errors = [{"loc": list(e.get("loc", [])), "msg": e.get("msg", ""), "type": e.get("type", "")} for e in err.errors()]
        return jsonify({"error": "Validation failed", "details": clean_errors}), 422

    actor = get_session_actor()
    conn = get_db()
    cursor = conn.cursor()

    # Verify patient exists
    cursor.execute("SELECT id FROM patients WHERE id = ?", (record_data.patient_id,))
    if not cursor.fetchone():
        conn.close()
        return jsonify({"error": f"Patient with ID {record_data.patient_id} does not exist"}), 404

    record_id = f"REC-{uuid.uuid4().hex[:6].upper()}"
    created_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%SZ")

    # If created by patient, label as self_reported automatically
    verification_status = record_data.verification_status
    if actor["role"] == "patient":
        verification_status = "self_reported"

    cursor.execute("""
        INSERT INTO medical_records (
            id, patient_id, record_type, title, provider_name, date,
            description, category, metrics, medications, verification_status,
            created_by_role, created_by_id, created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        record_id,
        record_data.patient_id,
        record_data.record_type,
        record_data.title,
        record_data.provider_name,
        record_data.date,
        record_data.description,
        record_data.category or "General",
        json.dumps(record_data.metrics or {}),
        json.dumps(record_data.medications or []),
        verification_status,
        actor["role"],
        actor["id"],
        created_at
    ))

    # Log record creation in audit trail (Privacy safe: no sensitive descriptions)
    record_audit_event(
        conn,
        action="RECORD_CREATED",
        actor_role=actor["role"],
        actor_id=actor["id"],
        actor_name=actor["name"],
        patient_id=record_data.patient_id,
        details=f"New {record_data.record_type} record '{record_data.title}' added via {record_data.provider_name}"
    )

    conn.commit()
    conn.close()

    return jsonify({
        "message": "Medical record successfully recorded into unified timeline",
        "record_id": record_id,
        "record": {
            "id": record_id,
            "patient_id": record_data.patient_id,
            "record_type": record_data.record_type,
            "title": record_data.title,
            "provider_name": record_data.provider_name,
            "date": record_data.date,
            "description": record_data.description,
            "category": record_data.category,
            "metrics": record_data.metrics,
            "medications": record_data.medications,
            "verification_status": verification_status,
            "created_by_role": actor["role"],
            "created_by_id": actor["id"],
            "created_at": created_at
        }
    }), 201

@app.route("/api/records/<record_id>", methods=["DELETE"])
def delete_medical_record(record_id):
    actor = get_session_actor()
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("SELECT id, patient_id, title FROM medical_records WHERE id = ?", (record_id,))
    rec = cursor.fetchone()
    if not rec:
        conn.close()
        return jsonify({"error": f"Record {record_id} not found"}), 404

    cursor.execute("DELETE FROM medical_records WHERE id = ?", (record_id,))
    record_audit_event(
        conn,
        action="RECORD_DELETED",
        actor_role=actor["role"],
        actor_id=actor["id"],
        actor_name=actor["name"],
        patient_id=rec["patient_id"],
        details=f"Record '{rec['title']}' ({record_id}) was deleted"
    )
    conn.commit()
    conn.close()

    return jsonify({"message": f"Record {record_id} deleted successfully", "record_id": record_id})

@app.route("/api/records/<record_id>", methods=["PUT"])
def update_medical_record(record_id):
    actor = get_session_actor()
    body = request.get_json(force=True, silent=True) or {}
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("SELECT id, patient_id FROM medical_records WHERE id = ?", (record_id,))
    rec = cursor.fetchone()
    if not rec:
        conn.close()
        return jsonify({"error": f"Record {record_id} not found"}), 404

    title = body.get("title")
    description = body.get("description")
    provider_name = body.get("provider_name")
    category = body.get("category")
    date_val = body.get("date")

    updates = []
    params = []
    if title:
        updates.append("title = ?")
        params.append(title.strip())
    if description:
        updates.append("description = ?")
        params.append(description.strip())
    if provider_name:
        updates.append("provider_name = ?")
        params.append(provider_name.strip())
    if category:
        updates.append("category = ?")
        params.append(category.strip())
    if date_val:
        updates.append("date = ?")
        params.append(date_val.strip())

    if updates:
        params.append(record_id)
        cursor.execute(f"UPDATE medical_records SET {', '.join(updates)} WHERE id = ?", params)
        record_audit_event(
            conn,
            action="RECORD_UPDATED",
            actor_role=actor["role"],
            actor_id=actor["id"],
            actor_name=actor["name"],
            patient_id=rec["patient_id"],
            details=f"Record {record_id} was updated"
        )
        conn.commit()

    conn.close()
    return jsonify({"message": f"Record {record_id} updated successfully", "record_id": record_id})

# -------------------------------------------------------------
# Consent & Access Requests Management
# -------------------------------------------------------------

@app.route("/api/access-requests", methods=["GET"])
def list_access_requests():
    patient_id = request.args.get("patient_id")
    requester_id = request.args.get("requester_id")
    status = request.args.get("status")

    conn = get_db()
    cursor = conn.cursor()

    sql = """
        SELECT ar.*, p.name as patient_name
        FROM access_requests ar
        JOIN patients p ON ar.patient_id = p.id
        WHERE 1=1
    """
    params = []

    if patient_id:
        sql += " AND ar.patient_id = ?"
        params.append(patient_id)

    if requester_id:
        sql += " AND ar.requester_id = ?"
        params.append(requester_id)

    if status:
        sql += " AND ar.status = ?"
        params.append(status)

    sql += " ORDER BY ar.created_at DESC"
    cursor.execute(sql, params)
    requests_list = [dict(r) for r in cursor.fetchall()]

    conn.close()
    return jsonify({"requests": requests_list, "count": len(requests_list)})

@app.route("/api/access-requests", methods=["POST"])
def create_access_request():
    """
    Doctor or Provider requests access to a patient's records.
    """
    body = request.get_json(force=True, silent=True)
    if not body:
        return jsonify({"error": "Invalid JSON body provided"}), 400

    try:
        req_data = AccessRequestCreateModel(**body)
    except ValidationError as err:
        clean_errors = [{"loc": list(e.get("loc", [])), "msg": e.get("msg", ""), "type": e.get("type", "")} for e in err.errors()]
        return jsonify({"error": "Validation failed", "details": clean_errors}), 422

    actor = get_session_actor()
    conn = get_db()
    cursor = conn.cursor()

    # Check if patient exists
    cursor.execute("SELECT id, name FROM patients WHERE id = ?", (req_data.patient_id,))
    patient = cursor.fetchone()
    if not patient:
        conn.close()
        return jsonify({"error": f"Patient with ID {req_data.patient_id} does not exist"}), 404

    # Check if existing pending request exists
    cursor.execute("""
        SELECT id FROM access_requests 
        WHERE patient_id = ? AND requester_id = ? AND status = 'pending'
    """, (req_data.patient_id, req_data.requester_id))
    existing = cursor.fetchone()
    if existing:
        conn.close()
        return jsonify({
            "message": "A pending access request already exists for this patient and provider",
            "request_id": existing["id"],
            "status": "pending"
        }), 200

    request_id = f"REQ-{uuid.uuid4().hex[:6].upper()}"
    created_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%SZ")

    cursor.execute("""
        INSERT INTO access_requests (
            id, patient_id, requester_id, requester_name, requester_role,
            requester_organization, reason, scope, status, created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending', ?)
    """, (
        request_id,
        req_data.patient_id,
        req_data.requester_id,
        req_data.requester_name,
        req_data.requester_role,
        req_data.requester_organization,
        req_data.reason,
        req_data.scope or "all_records",
        created_at
    ))

    record_audit_event(
        conn,
        action="ACCESS_REQUESTED",
        actor_role=actor["role"],
        actor_id=actor["id"],
        actor_name=actor["name"],
        patient_id=req_data.patient_id,
        details=f"Doctor/Provider {req_data.requester_name} submitted access request {request_id} for clinical records"
    )

    conn.commit()
    conn.close()

    return jsonify({
        "message": "Access request successfully submitted to patient",
        "request_id": request_id,
        "status": "pending",
        "created_at": created_at
    }), 201

@app.route("/api/access-requests/<request_id>/decision", methods=["POST"])
def decide_access_request(request_id):
    """
    Patient decides (approve or reject) a doctor's access request.
    If approved, generates an active permission token/record.
    """
    body = request.get_json(force=True, silent=True)
    if not body:
        return jsonify({"error": "Invalid JSON body provided"}), 400

    try:
        decision_data = ConsentDecisionModel(**body)
    except ValidationError as err:
        clean_errors = [{"loc": list(e.get("loc", [])), "msg": e.get("msg", ""), "type": e.get("type", "")} for e in err.errors()]
        return jsonify({"error": "Validation failed", "details": clean_errors}), 422

    actor = get_session_actor()
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM access_requests WHERE id = ?", (request_id,))
    req_row = cursor.fetchone()
    if not req_row:
        conn.close()
        return jsonify({"error": f"Access request {request_id} not found"}), 404

    req_dict = dict(req_row)
    now_ts = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%SZ")

    cursor.execute("""
        UPDATE access_requests
        SET status = ?, decision_date = ?, decision_notes = ?
        WHERE id = ?
    """, (decision_data.decision, now_ts, decision_data.decision_notes, request_id))

    permission_id = None
    if decision_data.decision == "approved":
        # Create or update active permission
        permission_id = f"PERM-{uuid.uuid4().hex[:6].upper()}"
        cursor.execute("""
            INSERT INTO access_permissions (
                id, patient_id, grantee_id, grantee_name, grantee_role, scope, status, granted_at
            )
            VALUES (?, ?, ?, ?, ?, ?, 'active', ?)
        """, (
            permission_id,
            req_dict["patient_id"],
            req_dict["requester_id"],
            req_dict["requester_name"],
            req_dict["requester_role"],
            req_dict["scope"],
            now_ts
        ))

        record_audit_event(
            conn,
            action="CONSENT_GRANTED",
            actor_role=actor["role"],
            actor_id=actor["id"],
            actor_name=actor["name"],
            patient_id=req_dict["patient_id"],
            details=f"Patient granted consent {permission_id} to {req_dict['requester_name']} (Role: {req_dict['requester_role']})"
        )
    else:
        record_audit_event(
            conn,
            action="CONSENT_DENIED",
            actor_role=actor["role"],
            actor_id=actor["id"],
            actor_name=actor["name"],
            patient_id=req_dict["patient_id"],
            details=f"Patient rejected access request {request_id} from {req_dict['requester_name']}"
        )

    conn.commit()
    conn.close()

    return jsonify({
        "message": f"Access request {request_id} was {decision_data.decision}",
        "status": decision_data.decision,
        "permission_id": permission_id,
        "decision_date": now_ts
    })

# -------------------------------------------------------------
# Permissions & Revocation
# -------------------------------------------------------------

@app.route("/api/permissions", methods=["GET"])
def list_permissions():
    patient_id = request.args.get("patient_id")
    conn = get_db()
    cursor = conn.cursor()

    if patient_id:
        cursor.execute("""
            SELECT * FROM access_permissions 
            WHERE patient_id = ?
            ORDER BY granted_at DESC
        """, (patient_id,))
    else:
        cursor.execute("SELECT * FROM access_permissions ORDER BY granted_at DESC")

    permissions = [dict(r) for r in cursor.fetchall()]
    conn.close()
    return jsonify({"permissions": permissions, "count": len(permissions)})

@app.route("/api/permissions/<permission_id>/revoke", methods=["POST"])
def revoke_permission(permission_id):
    """
    Patient immediately revokes access permission for a doctor or provider.
    """
    actor = get_session_actor()
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM access_permissions WHERE id = ?", (permission_id,))
    row = cursor.fetchone()
    if not row:
        conn.close()
        return jsonify({"error": f"Permission {permission_id} not found"}), 404

    perm_dict = dict(row)
    now_ts = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%SZ")

    cursor.execute("""
        UPDATE access_permissions
        SET status = 'revoked', revoked_at = ?
        WHERE id = ?
    """, (now_ts, permission_id))

    record_audit_event(
        conn,
        action="CONSENT_REVOKED",
        actor_role=actor["role"],
        actor_id=actor["id"],
        actor_name=actor["name"],
        patient_id=perm_dict["patient_id"],
        details=f"Patient revoked access consent {permission_id} for {perm_dict['grantee_name']}"
    )

    conn.commit()
    conn.close()

    return jsonify({
        "message": f"Access permission {permission_id} successfully revoked",
        "status": "revoked",
        "revoked_at": now_ts
    })

# -------------------------------------------------------------
# Audit Logs (Privacy & Security Safeguards)
# -------------------------------------------------------------

@app.route("/api/audit-logs", methods=["GET"])
def get_audit_logs():
    """
    Returns audit trail logs for access requests, consent grants, revokes,
    and record views. Notice that sensitive clinical descriptions are never stored or returned.
    """
    patient_id = request.args.get("patient_id")
    limit = int(request.args.get("limit", 100))

    conn = get_db()
    cursor = conn.cursor()

    if patient_id:
        cursor.execute("""
            SELECT * FROM audit_logs 
            WHERE patient_id = ? OR patient_id = 'ALL'
            ORDER BY timestamp DESC 
            LIMIT ?
        """, (patient_id, limit))
    else:
        cursor.execute("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?", (limit,))

    logs = [dict(r) for r in cursor.fetchall()]
    conn.close()

    return jsonify({
        "audit_logs": logs,
        "count": len(logs),
        "privacy_notice": "Logs record operational actions and actor identities. Protected medical notes and diagnostics are excluded for HIPAA/privacy safeguards."
    })

# -------------------------------------------------------------
# Healthcare Providers Directory & Dashboard Stats
# -------------------------------------------------------------

@app.route("/api/providers", methods=["GET"])
def list_providers():
    """
    Returns simulated connected healthcare providers and their stats.
    """
    providers = [
        {
            "id": "PROV-301",
            "name": "Apex Diagnostics Laboratory",
            "type": "Diagnostic Laboratory",
            "city": "Metropolis",
            "contact": "support@apexlabs-demo.org",
            "capabilities": ["Complete Blood Count (CBC)", "Metabolic Panels", "Lipid Profiles", "HbA1c & Pathology"],
            "verification_badge": "Certified Clinical Lab (CLIA/CAP)",
            "records_authored": 4
        },
        {
            "id": "PROV-302",
            "name": "St. Jude Medical Center",
            "type": "General & Specialty Hospital",
            "city": "Metropolis Central",
            "contact": "records@stjudecenter-demo.org",
            "capabilities": ["Inpatient Care", "Cardiology", "Emergency Care", "Prescription Dispatch"],
            "verification_badge": "Accredited Tertiary Hospital",
            "records_authored": 3
        },
        {
            "id": "PROV-303",
            "name": "Metro Health Clinic",
            "type": "Primary & Ambulatory Care Clinic",
            "city": "North District",
            "contact": "care@metrohealth-demo.org",
            "capabilities": ["Annual Physicals", "Endocrinology", "Allergy Care", "Preventative Medicine"],
            "verification_badge": "Community Health Network",
            "records_authored": 5
        }
    ]
    return jsonify({"providers": providers, "count": len(providers)})

@app.route("/api/stats", methods=["GET"])
def get_stats():
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("SELECT COUNT(*) FROM patients")
    total_patients = cursor.fetchone()[0]

    cursor.execute("SELECT COUNT(*) FROM medical_records")
    total_records = cursor.fetchone()[0]

    cursor.execute("SELECT COUNT(DISTINCT provider_name) FROM medical_records")
    unique_providers = cursor.fetchone()[0]

    cursor.execute("SELECT COUNT(*) FROM access_requests WHERE status = 'pending'")
    pending_requests = cursor.fetchone()[0]

    cursor.execute("SELECT COUNT(*) FROM access_permissions WHERE status = 'active'")
    active_permissions = cursor.fetchone()[0]

    conn.close()

    return jsonify({
        "total_patients": total_patients,
        "total_records": total_records,
        "unique_providers": unique_providers,
        "pending_requests": pending_requests,
        "active_permissions": active_permissions,
        "system_status": "Operational"
    })

@app.route("/api/seed/reset", methods=["POST"])
def reset_seed():
    """
    Demo utility to reset the database to initial clean seed state.
    """
    from seed_data import seed_database
    seed_database()
    return jsonify({"message": "Database successfully reset to initial demo seed state."})

if __name__ == "__main__":
    # Run locally on port 5000
    app.run(host="127.0.0.1", port=5000, debug=True)
