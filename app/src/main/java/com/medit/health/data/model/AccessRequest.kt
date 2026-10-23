package com.medit.health.data.model

data class AccessRequest(
    val id: String,
    val patientId: String,
    val requesterId: String,
    val requesterName: String,
    val requesterRole: String, // "doctor", "provider"
    val requesterOrganization: String,
    val reason: String,
    val scope: String = "all_records",
    val status: String = "pending", // "pending", "approved", "rejected"
    val createdAt: String = "",
    val decisionDate: String = "",
    val decisionNotes: String = ""
)
