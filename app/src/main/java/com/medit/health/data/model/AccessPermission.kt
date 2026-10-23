package com.medit.health.data.model

data class AccessPermission(
    val id: String,
    val patientId: String,
    val granteeId: String,
    val granteeName: String,
    val granteeRole: String,
    val scope: String = "all_records",
    val status: String = "active", // "active", "revoked"
    val grantedAt: String = "",
    val revokedAt: String = ""
) {
    val isActive: Boolean
        get() = status.equals("active", ignoreCase = true)
}
