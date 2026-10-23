package com.medit.health.data.model

enum class UserRole {
    PATIENT,
    DOCTOR,
    PROVIDER,
    SYSTEM;

    companion object {
        fun fromString(value: String): UserRole {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                PATIENT
            }
        }
    }
}

data class User(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String,
    val role: UserRole,
    val associatedId: String, // e.g. PAT-101 for patient or DOC-201 for doctor
    val organization: String = "",
    val specialty: String = ""
)
