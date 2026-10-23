package com.medit.health.data.model

data class AuditLog(
    val id: String,
    val timestamp: String,
    val action: String, // "RECORD_VIEWED", "RECORD_CREATED", "ACCESS_REQUESTED", "CONSENT_GRANTED", "CONSENT_REVOKED", "LOGIN"
    val actorRole: String,
    val actorId: String,
    val actorName: String,
    val patientId: String,
    val details: String
)
