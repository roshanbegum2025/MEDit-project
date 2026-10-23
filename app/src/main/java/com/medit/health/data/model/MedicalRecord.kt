package com.medit.health.data.model

data class MedicalRecord(
    val id: String,
    val patientId: String,
    val recordType: String, // "visit", "diagnosis", "prescription", "lab_report", "vital_signs", "allergy"
    val title: String,
    val providerName: String,
    val doctorName: String = "",
    val doctorSpecialty: String = "",
    val date: String, // YYYY-MM-DD
    val description: String,
    val category: String = "General Medicine",
    val metrics: Map<String, String> = emptyMap(),
    val medications: List<MedicationItem> = emptyList(),
    val verificationStatus: String = "verified", // "verified", "self_reported", "pending_review"
    val createdByRole: String = "doctor",
    val createdById: String = "",
    val followUpDate: String = "",
    val attachmentPath: String = "",
    val attachmentType: String = "",
    val ocrProcessed: Boolean = false,
    val costInr: Double = 0.0,
    val createdAt: String = ""
) {
    val isVerified: Boolean
        get() = verificationStatus.equals("verified", ignoreCase = true)
}
