import os
import sys
import tempfile
import pytest
import json

# Add backend directory to sys.path
backend_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if backend_dir not in sys.path:
    sys.path.insert(0, backend_dir)

from app import app
from database import init_db
from seed_data import seed_database

@pytest.fixture
def client():
    # Create temporary database for tests
    db_fd, temp_db_path = tempfile.mkstemp(suffix=".db")
    os.close(db_fd)
    os.environ["MEDIT_DB_PATH"] = temp_db_path

    import database
    database.DB_FILE = temp_db_path

    # Initialize and seed temporary test database
    seed_database(temp_db_path)

    app.config["TESTING"] = True
    with app.test_client() as client:
        yield client

    # Cleanup
    if os.path.exists(temp_db_path):
        try:
            os.remove(temp_db_path)
        except OSError:
            pass

def test_patient_search_and_lookup(client):
    """Test searching for patients by name or ID and retrieving profile."""
    # 1. Search by name
    res = client.get("/api/patients?search=Sarah", headers={"X-Demo-Role": "doctor", "X-Demo-User-Id": "DOC-201"})
    assert res.status_code == 200
    data = res.get_json()
    assert data["count"] >= 1
    assert any(p["id"] == "PAT-101" for p in data["patients"])

    # 2. Search by ID
    res = client.get("/api/patients?search=PAT-102")
    assert res.status_code == 200
    data = res.get_json()
    assert len(data["patients"]) == 1
    assert data["patients"][0]["name"] == "Robert Chen"

    # 3. Direct lookup
    res = client.get("/api/patients/PAT-101")
    assert res.status_code == 200
    p = res.get_json()
    assert p["id"] == "PAT-101"
    assert "Penicillin" in p["allergies"]

def test_medical_record_creation_and_validation(client):
    """Test creating medical records with valid and invalid Pydantic payloads."""
    # Valid record creation
    valid_payload = {
        "patient_id": "PAT-101",
        "record_type": "lab_report",
        "title": "Lipid Panel Fasting Re-check",
        "provider_name": "Apex Diagnostics Laboratory",
        "date": "2026-03-22",
        "description": "Repeat fasting lipid panel indicates improvement following dietary modifications.",
        "category": "Biochemistry",
        "metrics": {"total_cholesterol": "195 mg/dL", "triglycerides": "130 mg/dL"},
        "medications": []
    }
    res = client.post("/api/records", json=valid_payload, headers={"X-Demo-Role": "provider", "X-Demo-User-Id": "PROV-301"})
    assert res.status_code == 201
    data = res.get_json()
    assert "record_id" in data
    assert data["record"]["title"] == "Lipid Panel Fasting Re-check"

    # Invalid payload - blank title
    invalid_payload = valid_payload.copy()
    invalid_payload["title"] = "   "
    res = client.post("/api/records", json=invalid_payload)
    assert res.status_code == 422
    err_data = res.get_json()
    assert "details" in err_data

    # Invalid payload - invalid date format
    invalid_date_payload = valid_payload.copy()
    invalid_date_payload["date"] = "03/22/2026"
    res = client.post("/api/records", json=invalid_date_payload)
    assert res.status_code == 422

    # Non-existent patient
    ghost_payload = valid_payload.copy()
    ghost_payload["patient_id"] = "PAT-999"
    res = client.post("/api/records", json=ghost_payload)
    assert res.status_code == 404

def test_chronological_sorting(client):
    """Test that medical timeline records are returned in descending chronological order."""
    res = client.get("/api/patients/PAT-101/records", headers={"X-Demo-Role": "patient", "X-Demo-User-Id": "PAT-101"})
    assert res.status_code == 200
    data = res.get_json()
    records = data["records"]
    assert len(records) > 1

    dates = [r["date"] for r in records]
    assert dates == sorted(dates, reverse=True), "Records must be sorted chronologically descending"

def test_unauthorized_doctor_record_access(client):
    """Test that an unauthorized doctor cannot view records without consent."""
    # Doctor DOC-201 does not have active consent for PAT-101 initially
    res = client.get("/api/patients/PAT-101/records", headers={
        "X-Demo-Role": "doctor",
        "X-Demo-User-Id": "DOC-201",
        "X-Demo-User-Name": "Dr. Marcus Vance"
    })
    assert res.status_code == 403
    data = res.get_json()
    assert data["consent_required"] is True
    assert "error" in data

def test_access_denial_and_consent_based_access_flow(client):
    """Test the complete workflow: request access -> rejection -> new request -> approval -> authorized view -> revocation."""
    # 1. Doctor requests access for PAT-103 (Elena Rodriguez)
    req_payload = {
        "patient_id": "PAT-103",
        "requester_id": "DOC-201",
        "requester_name": "Dr. Marcus Vance",
        "requester_role": "doctor",
        "requester_organization": "City Heart Institute",
        "reason": "Cardiology screening consultation",
        "scope": "all_records"
    }
    res = client.post("/api/access-requests", json=req_payload, headers={"X-Demo-Role": "doctor", "X-Demo-User-Id": "DOC-201"})
    assert res.status_code == 201
    req_id = res.get_json()["request_id"]

    # 2. Patient rejects this request
    res = client.post(f"/api/access-requests/{req_id}/decision", json={"decision": "rejected", "decision_notes": "Not needed currently"}, headers={"X-Demo-Role": "patient", "X-Demo-User-Id": "PAT-103"})
    assert res.status_code == 200
    assert res.get_json()["status"] == "rejected"

    # Doctor still cannot access records
    res = client.get("/api/patients/PAT-103/records", headers={"X-Demo-Role": "doctor", "X-Demo-User-Id": "DOC-201"})
    assert res.status_code == 403

    # 3. Doctor submits another request
    res = client.post("/api/access-requests", json=req_payload, headers={"X-Demo-Role": "doctor", "X-Demo-User-Id": "DOC-201"})
    assert res.status_code == 201
    req_id_2 = res.get_json()["request_id"]

    # 4. Patient APPROVES the second request
    res = client.post(f"/api/access-requests/{req_id_2}/decision", json={"decision": "approved", "decision_notes": "Approved for 1 year"}, headers={"X-Demo-Role": "patient", "X-Demo-User-Id": "PAT-103"})
    assert res.status_code == 200
    perm_id = res.get_json()["permission_id"]
    assert perm_id is not None

    # 5. Doctor can now successfully retrieve protected records!
    res = client.get("/api/patients/PAT-103/records", headers={"X-Demo-Role": "doctor", "X-Demo-User-Id": "DOC-201"})
    assert res.status_code == 200
    data = res.get_json()
    assert "records" in data

    # 6. Patient REVOKES access
    res = client.post(f"/api/permissions/{perm_id}/revoke", headers={"X-Demo-Role": "patient", "X-Demo-User-Id": "PAT-103"})
    assert res.status_code == 200
    assert res.get_json()["status"] == "revoked"

    # 7. Doctor access is immediately blocked again (403)
    res = client.get("/api/patients/PAT-103/records", headers={"X-Demo-Role": "doctor", "X-Demo-User-Id": "DOC-201"})
    assert res.status_code == 403

def test_audit_log_creation_and_privacy(client):
    """Verify that operational events are logged and sensitive clinical notes are excluded."""
    # Perform an action
    client.get("/api/patients?search=Elena", headers={"X-Demo-Role": "doctor", "X-Demo-User-Id": "DOC-201"})

    res = client.get("/api/audit-logs")
    assert res.status_code == 200
    data = res.get_json()
    logs = data["audit_logs"]
    assert len(logs) > 0

    # Verify audit logs don't leak raw clinical notes or protected text
    for entry in logs:
        assert "action" in entry
        assert "actor_role" in entry
        assert "timestamp" in entry
        # Check that clinical free-text descriptions are not dumped in audit details
        assert "Initiated once daily in the morning" not in entry["details"]
