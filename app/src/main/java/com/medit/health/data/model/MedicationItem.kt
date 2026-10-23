package com.medit.health.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MedicationItem(
    val name: String,
    val dosage: String = "",
    val frequency: String = "", // e.g. "Once Daily", "Twice Daily (Morning & Evening)"
    val duration: String = "",  // e.g. "14 Days", "30 Days"
    val instructions: String = "", // e.g. "Take after food"
    val priceInr: Double = 0.0
)
