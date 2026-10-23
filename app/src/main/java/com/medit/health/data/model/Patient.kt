package com.medit.health.data.model

data class Patient(
    val id: String, // e.g. PAT-101
    val name: String,
    val dob: String, // YYYY-MM-DD
    val gender: String, // Male, Female, Other
    val bloodType: String, // O+, A+, B+, AB+, etc.
    val phone: String = "",
    val email: String = "",
    val allergies: List<String> = emptyList(),
    val chronicConditions: List<String> = emptyList(),
    val emergencyContact: String = "",
    val abhaId: String = "", // Ayushman Bharat Health Account ID, e.g. "91-4452-9812-4019"
    val createdAt: String = ""
)
