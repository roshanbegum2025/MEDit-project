package com.medit.health.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.medit.health.data.local.MedItDatabaseHelper
import com.medit.health.data.model.*
import com.medit.health.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID

class MedItRepository(
    private val dbHelper: MedItDatabaseHelper,
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger: StateFlow<Int> = _refreshTrigger.asStateFlow()

    fun triggerRefresh() {
        _refreshTrigger.value += 1
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // -------------------------------------------------------------
    // Authentication
    // -------------------------------------------------------------

    fun login(username: String, password: String): Result<User> {
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        val hash = hashPassword(cleanPass)

        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE LOWER(username) = LOWER(?) AND password_hash = ?",
            arrayOf(cleanUser, hash)
        )

        return cursor.use { c ->
            if (c.moveToFirst()) {
                val user = parseUser(c)
                _currentUser.value = user
                recordAuditEvent(
                    action = "LOGIN",
                    actor = user,
                    patientId = if (user.role == UserRole.PATIENT) user.associatedId else "N/A",
                    details = "User ${user.fullName} (${user.role}) authenticated successfully"
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Invalid username or password. Please verify credentials."))
            }
        }
    }

    fun registerPatient(
        username: String,
        password: String,
        fullName: String,
        email: String,
        dob: String,
        gender: String,
        bloodType: String,
        phone: String,
        emergencyContact: String,
        abhaId: String
    ): Result<User> {
        val db = dbHelper.writableDatabase

        // Check if username exists
        val checkCursor = db.rawQuery("SELECT id FROM users WHERE LOWER(username) = LOWER(?)", arrayOf(username.trim()))
        val exists = checkCursor.use { it.count > 0 }
        if (exists) {
            return Result.failure(Exception("Username '$username' is already taken. Please choose another."))
        }

        val patientId = "PAT-${(100..999).random()}"
        val userId = "USR-${UUID.randomUUID().toString().take(8).uppercase()}"
        val hash = hashPassword(password.trim())
        val createdAt = DateUtils.currentTimestamp()

        db.beginTransaction()
        try {
            // Insert Patient record
            val patValues = ContentValues().apply {
                put("id", patientId)
                put("name", fullName.trim())
                put("dob", dob.trim())
                put("gender", gender)
                put("blood_type", bloodType)
                put("phone", phone.trim())
                put("email", email.trim())
                put("allergies", "[]")
                put("chronic_conditions", "[]")
                put("emergency_contact", emergencyContact.trim())
                put("abha_id", if (abhaId.isNotBlank()) abhaId.trim() else "91-${(1000..9999).random()}-${(1000..9999).random()}-${(1000..9999).random()}")
                put("created_at", createdAt)
            }
            db.insertOrThrow("patients", null, patValues)

            // Insert User record
            val userValues = ContentValues().apply {
                put("id", userId)
                put("username", username.trim())
                put("password_hash", hash)
                put("full_name", fullName.trim())
                put("email", email.trim())
                put("role", UserRole.PATIENT.name)
                put("associated_id", patientId)
                put("organization", "")
                put("specialty", "")
            }
            db.insertOrThrow("users", null, userValues)

            db.setTransactionSuccessful()

            val newUser = User(
                id = userId,
                username = username.trim(),
                fullName = fullName.trim(),
                email = email.trim(),
                role = UserRole.PATIENT,
                associatedId = patientId
            )
            _currentUser.value = newUser

            recordAuditEvent(
                action = "ACCOUNT_CREATED",
                actor = newUser,
                patientId = patientId,
                details = "Patient account created with MedIt ID $patientId"
            )

            triggerRefresh()
            return Result.success(newUser)
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            db.endTransaction()
        }
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) {
            recordAuditEvent(
                action = "LOGOUT",
                actor = user,
                patientId = if (user.role == UserRole.PATIENT) user.associatedId else "N/A",
                details = "User ${user.fullName} signed out"
            )
        }
        _currentUser.value = null
    }

    // -------------------------------------------------------------
    // Patients
    // -------------------------------------------------------------

    fun getAllPatients(): List<Patient> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM patients ORDER BY name ASC", null)
        return cursor.use { c ->
            val list = mutableListOf<Patient>()
            while (c.moveToNext()) {
                list.add(parsePatient(c))
            }
            list
        }
    }

    fun getPatient(patientId: String): Patient? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM patients WHERE id = ?", arrayOf(patientId))
        return cursor.use { c ->
            if (c.moveToFirst()) parsePatient(c) else null
        }
    }

    fun searchPatients(query: String): List<Patient> {
        val db = dbHelper.readableDatabase
        val q = "%${query.trim().lowercase()}%"
        val cursor = db.rawQuery(
            """
            SELECT * FROM patients 
            WHERE LOWER(id) LIKE ? OR LOWER(name) LIKE ? OR LOWER(email) LIKE ? OR LOWER(phone) LIKE ? OR LOWER(abha_id) LIKE ?
            ORDER BY name ASC
            """.trimIndent(),
            arrayOf(q, q, q, q, q)
        )
        return cursor.use { c ->
            val list = mutableListOf<Patient>()
            while (c.moveToNext()) {
                list.add(parsePatient(c))
            }
            list
        }
    }

    // -------------------------------------------------------------
    // Medical Records (Enforcing Role & Patient Consent)
    // -------------------------------------------------------------

    /**
     * Fetches unified chronological medical timeline.
     * Enforces real role authorization and patient consent!
     */
    fun getRecordsForPatient(patientId: String, currentActor: User?): Result<List<MedicalRecord>> {
        if (currentActor == null) {
            return Result.failure(SecurityException("Unauthorized: Please sign in to view clinical records."))
        }

        // 1. If Patient: Can only view their OWN records!
        if (currentActor.role == UserRole.PATIENT) {
            if (currentActor.associatedId != patientId) {
                return Result.failure(SecurityException("Access Denied: Patient is not authorized to view another patient's medical records."))
            }
        }

        // 2. If Doctor: MUST have an active patient consent permission!
        if (currentActor.role == UserRole.DOCTOR) {
            val hasActiveConsent = checkDoctorAccess(patientId, currentActor.associatedId)
            if (!hasActiveConsent) {
                return Result.failure(
                    SecurityException("Consent Required: Patient has not authorized Dr. ${currentActor.fullName} to access their health records. Please submit an access request.")
                )
            }
        }

        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM medical_records WHERE patient_id = ? ORDER BY date DESC, created_at DESC",
            arrayOf(patientId)
        )

        return cursor.use { c ->
            val records = mutableListOf<MedicalRecord>()
            while (c.moveToNext()) {
                records.add(parseMedicalRecord(c))
            }

            // Log access event
            recordAuditEvent(
                action = "RECORD_VIEWED",
                actor = currentActor,
                patientId = patientId,
                details = "${currentActor.fullName} (${currentActor.role}) viewed ${records.size} clinical records in timeline"
            )

            Result.success(records)
        }
    }

    fun getRecordById(recordId: String): MedicalRecord? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM medical_records WHERE id = ?", arrayOf(recordId))
        return cursor.use { c ->
            if (c.moveToFirst()) parseMedicalRecord(c) else null
        }
    }

    fun createRecord(record: MedicalRecord, currentActor: User): Result<MedicalRecord> {
        val db = dbHelper.writableDatabase

        // Verification status determination:
        // Patient self-created records are marked "self_reported"
        // Doctor / Hospital created records are "verified"
        val status = if (currentActor.role == UserRole.PATIENT) "self_reported" else record.verificationStatus

        val recordId = if (record.id.isNotBlank()) record.id else "REC-${(100..999).random()}"
        val createdAt = DateUtils.currentTimestamp()

        val values = ContentValues().apply {
            put("id", recordId)
            put("patient_id", record.patientId)
            put("record_type", record.recordType)
            put("title", record.title.trim())
            put("provider_name", record.providerName.trim())
            put("doctor_name", record.doctorName.trim())
            put("doctor_specialty", record.doctorSpecialty.trim())
            put("date", record.date.trim())
            put("description", record.description.trim())
            put("category", record.category.trim())
            put("metrics", json.encodeToString(record.metrics))
            put("medications", json.encodeToString(record.medications))
            put("verification_status", status)
            put("created_by_role", currentActor.role.name.lowercase())
            put("created_by_id", currentActor.associatedId)
            put("follow_up_date", record.followUpDate.trim())
            put("attachment_path", record.attachmentPath)
            put("attachment_type", record.attachmentType)
            put("ocr_processed", if (record.ocrProcessed) 1 else 0)
            put("cost_inr", record.costInr)
            put("created_at", createdAt)
        }

        return try {
            db.insertOrThrow("medical_records", null, values)

            val created = record.copy(
                id = recordId,
                verificationStatus = status,
                createdByRole = currentActor.role.name.lowercase(),
                createdById = currentActor.associatedId,
                createdAt = createdAt
            )

            recordAuditEvent(
                action = "RECORD_CREATED",
                actor = currentActor,
                patientId = record.patientId,
                details = "New ${record.recordType} record '${record.title}' added via ${record.providerName}"
            )

            triggerRefresh()
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateRecord(record: MedicalRecord, currentActor: User): Result<MedicalRecord> {
        val db = dbHelper.writableDatabase

        // Authorization check: Patients can only edit self-reported or patient-created records
        val existing = getRecordById(record.id) ?: return Result.failure(Exception("Record not found"))
        if (currentActor.role == UserRole.PATIENT && existing.createdByRole != "patient") {
            return Result.failure(SecurityException("Patients cannot alter hospital-verified records directly."))
        }

        val values = ContentValues().apply {
            put("title", record.title.trim())
            put("provider_name", record.providerName.trim())
            put("doctor_name", record.doctorName.trim())
            put("doctor_specialty", record.doctorSpecialty.trim())
            put("date", record.date.trim())
            put("description", record.description.trim())
            put("category", record.category.trim())
            put("metrics", json.encodeToString(record.metrics))
            put("medications", json.encodeToString(record.medications))
            put("follow_up_date", record.followUpDate.trim())
            put("cost_inr", record.costInr)
            if (record.attachmentPath.isNotBlank()) {
                put("attachment_path", record.attachmentPath)
            }
        }

        val updatedRows = db.update("medical_records", values, "id = ?", arrayOf(record.id))
        return if (updatedRows > 0) {
            recordAuditEvent(
                action = "RECORD_UPDATED",
                actor = currentActor,
                patientId = record.patientId,
                details = "Updated record '${record.title}' (ID: ${record.id})"
            )
            triggerRefresh()
            Result.success(record)
        } else {
            Result.failure(Exception("Failed to update record"))
        }
    }

    fun deleteRecord(recordId: String, currentActor: User): Result<Boolean> {
        val existing = getRecordById(recordId) ?: return Result.failure(Exception("Record not found"))

        // Authorization check
        if (currentActor.role == UserRole.PATIENT && existing.patientId != currentActor.associatedId) {
            return Result.failure(SecurityException("Unauthorized to delete record."))
        }

        val db = dbHelper.writableDatabase
        val rows = db.delete("medical_records", "id = ?", arrayOf(recordId))
        return if (rows > 0) {
            recordAuditEvent(
                action = "RECORD_DELETED",
                actor = currentActor,
                patientId = existing.patientId,
                details = "Deleted record '${existing.title}' (ID: $recordId)"
            )
            triggerRefresh()
            Result.success(true)
        } else {
            Result.failure(Exception("Could not delete record"))
        }
    }

    // -------------------------------------------------------------
    // Access Requests & Permissions
    // -------------------------------------------------------------

    fun checkDoctorAccess(patientId: String, doctorId: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT id FROM access_permissions 
            WHERE patient_id = ? AND grantee_id = ? AND status = 'active'
            """.trimIndent(),
            arrayOf(patientId, doctorId)
        )
        return cursor.use { it.count > 0 }
    }

    fun getAccessRequestsForPatient(patientId: String): List<AccessRequest> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM access_requests WHERE patient_id = ? ORDER BY created_at DESC",
            arrayOf(patientId)
        )
        return cursor.use { c ->
            val list = mutableListOf<AccessRequest>()
            while (c.moveToNext()) {
                list.add(parseAccessRequest(c))
            }
            list
        }
    }

    fun getAccessRequestsForDoctor(doctorId: String): List<AccessRequest> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM access_requests WHERE requester_id = ? ORDER BY created_at DESC",
            arrayOf(doctorId)
        )
        return cursor.use { c ->
            val list = mutableListOf<AccessRequest>()
            while (c.moveToNext()) {
                list.add(parseAccessRequest(c))
            }
            list
        }
    }

    fun createAccessRequest(
        patientId: String,
        doctor: User,
        reason: String,
        scope: String = "all_records"
    ): Result<AccessRequest> {
        val db = dbHelper.writableDatabase

        // Check if pending already exists
        val existingCursor = db.rawQuery(
            "SELECT * FROM access_requests WHERE patient_id = ? AND requester_id = ? AND status = 'pending'",
            arrayOf(patientId, doctor.associatedId)
        )
        val existing = existingCursor.use { c ->
            if (c.moveToFirst()) parseAccessRequest(c) else null
        }
        if (existing != null) {
            return Result.success(existing)
        }

        val requestId = "REQ-${(100..999).random()}"
        val createdAt = DateUtils.currentTimestamp()

        val values = ContentValues().apply {
            put("id", requestId)
            put("patient_id", patientId)
            put("requester_id", doctor.associatedId)
            put("requester_name", doctor.fullName)
            put("requester_role", doctor.role.name.lowercase())
            put("requester_organization", if (doctor.organization.isNotBlank()) doctor.organization else "Independent Medical Practice")
            put("reason", reason.trim())
            put("scope", scope)
            put("status", "pending")
            put("created_at", createdAt)
        }

        return try {
            db.insertOrThrow("access_requests", null, values)
            val newReq = AccessRequest(
                id = requestId,
                patientId = patientId,
                requesterId = doctor.associatedId,
                requesterName = doctor.fullName,
                requesterRole = doctor.role.name.lowercase(),
                requesterOrganization = doctor.organization,
                reason = reason,
                scope = scope,
                status = "pending",
                createdAt = createdAt
            )

            recordAuditEvent(
                action = "ACCESS_REQUESTED",
                actor = doctor,
                patientId = patientId,
                details = "Dr. ${doctor.fullName} requested consent to access patient records. Reason: '$reason'"
            )

            triggerRefresh()
            Result.success(newReq)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decideAccessRequest(
        requestId: String,
        decision: String, // "approved" or "rejected"
        notes: String,
        patientUser: User
    ): Result<Boolean> {
        val db = dbHelper.writableDatabase
        val reqCursor = db.rawQuery("SELECT * FROM access_requests WHERE id = ?", arrayOf(requestId))
        val req = reqCursor.use { c ->
            if (c.moveToFirst()) parseAccessRequest(c) else null
        } ?: return Result.failure(Exception("Access request $requestId not found"))

        val decisionDate = DateUtils.currentTimestamp()

        db.beginTransaction()
        try {
            // Update request status
            val reqValues = ContentValues().apply {
                put("status", decision)
                put("decision_date", decisionDate)
                put("decision_notes", notes.trim())
            }
            db.update("access_requests", reqValues, "id = ?", arrayOf(requestId))

            // If approved, create active permission
            if (decision == "approved") {
                val permId = "PERM-${(100..999).random()}"
                val permValues = ContentValues().apply {
                    put("id", permId)
                    put("patient_id", req.patientId)
                    put("grantee_id", req.requesterId)
                    put("grantee_name", req.requesterName)
                    put("grantee_role", req.requesterRole)
                    put("scope", req.scope)
                    put("status", "active")
                    put("granted_at", decisionDate)
                }
                db.insertOrThrow("access_permissions", null, permValues)

                recordAuditEvent(
                    action = "CONSENT_GRANTED",
                    actor = patientUser,
                    patientId = req.patientId,
                    details = "Patient granted clinical record consent to ${req.requesterName} (${req.requesterRole})"
                )
            } else {
                recordAuditEvent(
                    action = "CONSENT_DENIED",
                    actor = patientUser,
                    patientId = req.patientId,
                    details = "Patient declined clinical record access request from ${req.requesterName}"
                )
            }

            db.setTransactionSuccessful()
            triggerRefresh()
            return Result.success(true)
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            db.endTransaction()
        }
    }

    fun getPermissionsForPatient(patientId: String): List<AccessPermission> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM access_permissions WHERE patient_id = ? ORDER BY granted_at DESC",
            arrayOf(patientId)
        )
        return cursor.use { c ->
            val list = mutableListOf<AccessPermission>()
            while (c.moveToNext()) {
                list.add(parseAccessPermission(c))
            }
            list
        }
    }

    fun revokePermission(permissionId: String, patientUser: User): Result<Boolean> {
        val db = dbHelper.writableDatabase
        val permCursor = db.rawQuery("SELECT * FROM access_permissions WHERE id = ?", arrayOf(permissionId))
        val perm = permCursor.use { c ->
            if (c.moveToFirst()) parseAccessPermission(c) else null
        } ?: return Result.failure(Exception("Permission record not found"))

        val revokedAt = DateUtils.currentTimestamp()
        val values = ContentValues().apply {
            put("status", "revoked")
            put("revoked_at", revokedAt)
        }

        val rows = db.update("access_permissions", values, "id = ?", arrayOf(permissionId))
        return if (rows > 0) {
            recordAuditEvent(
                action = "CONSENT_REVOKED",
                actor = patientUser,
                patientId = perm.patientId,
                details = "Patient revoked clinical access permission for ${perm.granteeName}"
            )
            triggerRefresh()
            Result.success(true)
        } else {
            Result.failure(Exception("Failed to revoke permission"))
        }
    }

    // -------------------------------------------------------------
    // Audit Logs
    // -------------------------------------------------------------

    fun getAuditLogs(patientId: String? = null, limit: Int = 100): List<AuditLog> {
        val db = dbHelper.readableDatabase
        val cursor = if (patientId != null) {
            db.rawQuery(
                "SELECT * FROM audit_logs WHERE patient_id = ? OR patient_id = 'ALL' OR patient_id = 'N/A' ORDER BY timestamp DESC LIMIT ?",
                arrayOf(patientId, limit.toString())
            )
        } else {
            db.rawQuery(
                "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?",
                arrayOf(limit.toString())
            )
        }

        return cursor.use { c ->
            val list = mutableListOf<AuditLog>()
            while (c.moveToNext()) {
                list.add(parseAuditLog(c))
            }
            list
        }
    }

    fun recordAuditEvent(action: String, actor: User, patientId: String, details: String) {
        try {
            val db = dbHelper.writableDatabase
            val logId = "AUD-${(100..999).random()}"
            val timestamp = DateUtils.currentTimestamp()
            val values = ContentValues().apply {
                put("id", logId)
                put("timestamp", timestamp)
                put("action", action)
                put("actor_role", actor.role.name.lowercase())
                put("actor_id", actor.associatedId.ifBlank { actor.id })
                put("actor_name", actor.fullName)
                put("patient_id", patientId)
                put("details", details)
            }
            db.insert("audit_logs", null, values)
        } catch (e: Exception) {
            // Silently swallow audit insert failures
        }
    }

    // -------------------------------------------------------------
    // Entity Parsers
    // -------------------------------------------------------------

    private fun parseUser(c: Cursor): User {
        return User(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            username = c.getString(c.getColumnIndexOrThrow("username")),
            fullName = c.getString(c.getColumnIndexOrThrow("full_name")),
            email = c.getString(c.getColumnIndexOrThrow("email")),
            role = UserRole.fromString(c.getString(c.getColumnIndexOrThrow("role"))),
            associatedId = c.getString(c.getColumnIndexOrThrow("associated_id")),
            organization = c.getString(c.getColumnIndexOrThrow("organization")) ?: "",
            specialty = c.getString(c.getColumnIndexOrThrow("specialty")) ?: ""
        )
    }

    private fun parsePatient(c: Cursor): Patient {
        val allergiesStr = c.getString(c.getColumnIndexOrThrow("allergies")) ?: "[]"
        val chronicStr = c.getString(c.getColumnIndexOrThrow("chronic_conditions")) ?: "[]"

        val allergies = try {
            json.decodeFromString<List<String>>(allergiesStr)
        } catch (e: Exception) {
            emptyList()
        }
        val chronic = try {
            json.decodeFromString<List<String>>(chronicStr)
        } catch (e: Exception) {
            emptyList()
        }

        return Patient(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            name = c.getString(c.getColumnIndexOrThrow("name")),
            dob = c.getString(c.getColumnIndexOrThrow("dob")),
            gender = c.getString(c.getColumnIndexOrThrow("gender")),
            bloodType = c.getString(c.getColumnIndexOrThrow("blood_type")),
            phone = c.getString(c.getColumnIndexOrThrow("phone")) ?: "",
            email = c.getString(c.getColumnIndexOrThrow("email")) ?: "",
            allergies = allergies,
            chronicConditions = chronic,
            emergencyContact = c.getString(c.getColumnIndexOrThrow("emergency_contact")) ?: "",
            abhaId = c.getString(c.getColumnIndexOrThrow("abha_id")) ?: "",
            createdAt = c.getString(c.getColumnIndexOrThrow("created_at")) ?: ""
        )
    }

    private fun parseMedicalRecord(c: Cursor): MedicalRecord {
        val metricsStr = c.getString(c.getColumnIndexOrThrow("metrics")) ?: "{}"
        val medsStr = c.getString(c.getColumnIndexOrThrow("medications")) ?: "[]"

        val metrics = try {
            json.decodeFromString<Map<String, String>>(metricsStr)
        } catch (e: Exception) {
            emptyMap()
        }
        val medications = try {
            json.decodeFromString<List<MedicationItem>>(medsStr)
        } catch (e: Exception) {
            emptyList()
        }

        return MedicalRecord(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            patientId = c.getString(c.getColumnIndexOrThrow("patient_id")),
            recordType = c.getString(c.getColumnIndexOrThrow("record_type")),
            title = c.getString(c.getColumnIndexOrThrow("title")),
            providerName = c.getString(c.getColumnIndexOrThrow("provider_name")),
            doctorName = c.getString(c.getColumnIndexOrThrow("doctor_name")) ?: "",
            doctorSpecialty = c.getString(c.getColumnIndexOrThrow("doctor_specialty")) ?: "",
            date = c.getString(c.getColumnIndexOrThrow("date")),
            description = c.getString(c.getColumnIndexOrThrow("description")),
            category = c.getString(c.getColumnIndexOrThrow("category")) ?: "General Medicine",
            metrics = metrics,
            medications = medications,
            verificationStatus = c.getString(c.getColumnIndexOrThrow("verification_status")) ?: "verified",
            createdByRole = c.getString(c.getColumnIndexOrThrow("created_by_role")) ?: "doctor",
            createdById = c.getString(c.getColumnIndexOrThrow("created_by_id")) ?: "",
            followUpDate = c.getString(c.getColumnIndexOrThrow("follow_up_date")) ?: "",
            attachmentPath = c.getString(c.getColumnIndexOrThrow("attachment_path")) ?: "",
            attachmentType = c.getString(c.getColumnIndexOrThrow("attachment_type")) ?: "",
            ocrProcessed = c.getInt(c.getColumnIndexOrThrow("ocr_processed")) == 1,
            costInr = c.getDouble(c.getColumnIndexOrThrow("cost_inr")),
            createdAt = c.getString(c.getColumnIndexOrThrow("created_at")) ?: ""
        )
    }

    private fun parseAccessRequest(c: Cursor): AccessRequest {
        return AccessRequest(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            patientId = c.getString(c.getColumnIndexOrThrow("patient_id")),
            requesterId = c.getString(c.getColumnIndexOrThrow("requester_id")),
            requesterName = c.getString(c.getColumnIndexOrThrow("requester_name")),
            requesterRole = c.getString(c.getColumnIndexOrThrow("requester_role")),
            requesterOrganization = c.getString(c.getColumnIndexOrThrow("requester_organization")) ?: "",
            reason = c.getString(c.getColumnIndexOrThrow("reason")),
            scope = c.getString(c.getColumnIndexOrThrow("scope")) ?: "all_records",
            status = c.getString(c.getColumnIndexOrThrow("status")),
            createdAt = c.getString(c.getColumnIndexOrThrow("created_at")) ?: "",
            decisionDate = c.getString(c.getColumnIndexOrThrow("decision_date")) ?: "",
            decisionNotes = c.getString(c.getColumnIndexOrThrow("decision_notes")) ?: ""
        )
    }

    private fun parseAccessPermission(c: Cursor): AccessPermission {
        return AccessPermission(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            patientId = c.getString(c.getColumnIndexOrThrow("patient_id")),
            granteeId = c.getString(c.getColumnIndexOrThrow("grantee_id")),
            granteeName = c.getString(c.getColumnIndexOrThrow("grantee_name")),
            granteeRole = c.getString(c.getColumnIndexOrThrow("grantee_role")),
            scope = c.getString(c.getColumnIndexOrThrow("scope")) ?: "all_records",
            status = c.getString(c.getColumnIndexOrThrow("status")),
            grantedAt = c.getString(c.getColumnIndexOrThrow("granted_at")) ?: "",
            revokedAt = c.getString(c.getColumnIndexOrThrow("revoked_at")) ?: ""
        )
    }

    private fun parseAuditLog(c: Cursor): AuditLog {
        return AuditLog(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            timestamp = c.getString(c.getColumnIndexOrThrow("timestamp")),
            action = c.getString(c.getColumnIndexOrThrow("action")),
            actorRole = c.getString(c.getColumnIndexOrThrow("actor_role")),
            actorId = c.getString(c.getColumnIndexOrThrow("actor_id")),
            actorName = c.getString(c.getColumnIndexOrThrow("actor_name")),
            patientId = c.getString(c.getColumnIndexOrThrow("patient_id")),
            details = c.getString(c.getColumnIndexOrThrow("details"))
        )
    }
}
