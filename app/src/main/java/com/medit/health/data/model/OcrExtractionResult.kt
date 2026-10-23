package com.medit.health.data.model

data class OcrExtractionResult(
    val title: String,
    val providerName: String,
    val doctorName: String,
    val date: String,
    val category: String,
    val diagnosis: String,
    val medications: List<MedicationItem>,
    val followUpDate: String,
    val rawText: String,
    val confidenceScore: Float, // 0.0 to 1.0
    val uncertainFields: List<String> = emptyList(), // e.g. ["Dosage for Paracetamol unclear", "Doctor signature unverified"]
    val hasUncertainties: Boolean = uncertainFields.isNotEmpty()
)
