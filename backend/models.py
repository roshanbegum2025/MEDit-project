from typing import Optional, List, Dict, Any, Literal
from pydantic import BaseModel, Field, field_validator
import re
from datetime import datetime

RecordType = Literal['visit', 'diagnosis', 'prescription', 'allergy', 'lab_report', 'vital_signs']
VerificationStatus = Literal['verified', 'self_reported', 'pending_review']
ActorRole = Literal['patient', 'doctor', 'provider', 'system']
RequestStatus = Literal['pending', 'approved', 'rejected']
ScopeType = Literal['all_records', 'lab_reports', 'prescriptions', 'consultation_summary']

class PatientCreateModel(BaseModel):
    id: str = Field(..., min_length=3, max_length=30, description="Patient identifier, e.g. PAT-101")
    name: str = Field(..., min_length=2, max_length=100)
    dob: str = Field(..., pattern=r"^\d{4}-\d{2}-\d{2}$", description="Date of birth YYYY-MM-DD")
    gender: Literal['Male', 'Female', 'Non-Binary', 'Other']
    blood_type: Literal['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']
    phone: Optional[str] = Field(None, max_length=25)
    email: Optional[str] = Field(None, max_length=100)
    allergies: Optional[List[str]] = Field(default_factory=list)
    chronic_conditions: Optional[List[str]] = Field(default_factory=list)
    emergency_contact: Optional[str] = Field(None, max_length=150)

class RecordCreateModel(BaseModel):
    patient_id: str = Field(..., min_length=3, max_length=30)
    record_type: RecordType
    title: str = Field(..., min_length=3, max_length=150)
    provider_name: str = Field(..., min_length=2, max_length=120)
    date: str = Field(..., pattern=r"^\d{4}-\d{2}-\d{2}$", description="Clinical record date YYYY-MM-DD")
    description: str = Field(..., min_length=5, max_length=3000)
    category: Optional[str] = Field("General", max_length=60)
    metrics: Optional[Dict[str, Any]] = Field(default_factory=dict)
    medications: Optional[List[Dict[str, Any]]] = Field(default_factory=list)
    verification_status: Optional[VerificationStatus] = "verified"
    created_by_role: Optional[ActorRole] = "doctor"
    created_by_id: Optional[str] = Field(None, max_length=50)

    @field_validator('title', 'provider_name', 'description')
    @classmethod
    def strip_whitespace(cls, v: str) -> str:
        stripped = v.strip()
        if not stripped:
            raise ValueError("Field cannot be blank or only whitespace")
        return stripped

class AccessRequestCreateModel(BaseModel):
    patient_id: str = Field(..., min_length=3, max_length=30)
    requester_id: str = Field(..., min_length=2, max_length=50)
    requester_name: str = Field(..., min_length=2, max_length=100)
    requester_role: Literal['doctor', 'provider']
    requester_organization: Optional[str] = Field("Independent Practice", max_length=120)
    reason: str = Field(..., min_length=5, max_length=300)
    scope: Optional[ScopeType] = "all_records"

    @field_validator('reason')
    @classmethod
    def strip_reason(cls, v: str) -> str:
        stripped = v.strip()
        if len(stripped) < 5:
            raise ValueError("Reason must be at least 5 characters")
        return stripped

class ConsentDecisionModel(BaseModel):
    decision: Literal['approved', 'rejected']
    decision_notes: Optional[str] = Field(None, max_length=200)

class AuditLogCreateModel(BaseModel):
    action: str = Field(..., min_length=3, max_length=50)
    actor_role: ActorRole
    actor_id: str = Field(..., min_length=2, max_length=50)
    actor_name: str = Field(..., min_length=2, max_length=100)
    patient_id: str = Field(..., min_length=3, max_length=30)
    details: str = Field(..., min_length=3, max_length=300)
