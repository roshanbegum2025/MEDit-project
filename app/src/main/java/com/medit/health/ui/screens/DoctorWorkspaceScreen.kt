package com.medit.health.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medit.health.data.model.*
import com.medit.health.data.repository.MedItRepository
import com.medit.health.ui.components.MedItTopBar
import com.medit.health.ui.components.RecordCard
import com.medit.health.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorWorkspaceScreen(
    doctorUser: User,
    repository: MedItRepository,
    onRecordClick: (MedicalRecord) -> Unit,
    onAddClinicalRecordClick: (String) -> Unit,
    onAuditTrailClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val refreshCount by repository.refreshTrigger.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var allPatients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }

    var hasConsent by remember { mutableStateOf(false) }
    var patientRecords by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var doctorRequests by remember { mutableStateOf<List<AccessRequest>>(emptyList()) }

    var selectedFilter by remember { mutableStateOf("all") }
    var requestReason by remember { mutableStateOf("Cardiac clinical review, medication reconciliation, and diagnostic interpretation.") }
    var isSubmittingRequest by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load initial doctor data
    LaunchedEffect(doctorUser.associatedId, refreshCount) {
        allPatients = repository.getAllPatients()
        if (selectedPatient == null && allPatients.isNotEmpty()) {
            selectedPatient = allPatients.first()
        }

        doctorRequests = repository.getAccessRequestsForDoctor(doctorUser.associatedId)
    }

    // When selected patient changes, re-evaluate authorization
    LaunchedEffect(selectedPatient, refreshCount) {
        val pat = selectedPatient
        if (pat != null) {
            val authorized = repository.checkDoctorAccess(pat.id, doctorUser.associatedId)
            hasConsent = authorized

            if (authorized) {
                val res = repository.getRecordsForPatient(pat.id, doctorUser)
                res.onSuccess {
                    patientRecords = it
                }.onFailure {
                    patientRecords = emptyList()
                    hasConsent = false
                }
            } else {
                patientRecords = emptyList()
            }
        }
    }

    // Filter patient records
    val filteredRecords = remember(patientRecords, selectedFilter) {
        patientRecords.filter { rec ->
            when (selectedFilter) {
                "all" -> true
                "prescription" -> rec.recordType == "prescription"
                "lab_report" -> rec.recordType == "lab_report"
                "visit" -> rec.recordType == "visit"
                "diagnosis" -> rec.recordType == "diagnosis"
                else -> true
            }
        }
    }

    val pendingForSelected = doctorRequests.find {
        it.patientId == selectedPatient?.id && it.status == "pending"
    }

    Scaffold(
        topBar = {
            MedItTopBar(
                title = "Doctor Clinical Workspace",
                currentUser = doctorUser,
                onAuditClick = onAuditTrailClick,
                onSignOutClick = onSignOutClick
            )
        },
        containerColor = BackgroundWarm
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Doctor Profile Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MedItMintAccent, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MedItMintSoft)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MedItDarkGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MedItMintAccent, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doctorUser.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MedItDarkGreen
                                )
                            )
                            Text(
                                text = "${doctorUser.specialty} • ${doctorUser.organization}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MedItPrimary800)
                            )
                        }
                    }
                }
            }

            // 2. Patient Search & Selector
            item {
                Column {
                    Text(
                        text = "Patient Registry Search",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            allPatients = if (it.isBlank()) repository.getAllPatients() else repository.searchPatients(it)
                        },
                        placeholder = { Text("Search by Patient ID, Name, or ABHA...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_doctor_search_patient"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Patient Quick Selection Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allPatients) { pat ->
                            val isSelected = selectedPatient?.id == pat.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MedItDarkGreen else SurfacePure,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MedItDarkGreen else SurfaceBorder),
                                modifier = Modifier
                                    .clickable { selectedPatient = pat }
                                    .testTag("select_patient_${pat.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) MedItMintAccent else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = pat.name,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else TextPrimary
                                            )
                                        )
                                        Text(
                                            text = pat.id,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 10.sp,
                                                color = if (isSelected) MedItMintLight else TextMuted
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Selected Patient Status Banner
            selectedPatient?.let { pat ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfacePure)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = pat.name,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "ID: ${pat.id} • ${pat.gender}, Age ${2026 - (pat.dob.take(4).toIntOrNull() ?: 1990)} • Blood: ${pat.bloodType}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (hasConsent) StatusVerifiedBg else StatusSelfReportedBg
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (hasConsent) Icons.Default.CheckCircle else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (hasConsent) StatusVerifiedGreen else StatusSelfReportedAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (hasConsent) "Active Consent" else "Consent Required",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasConsent) StatusVerifiedGreen else StatusSelfReportedAmber
                                            )
                                        )
                                    }
                                }
                            }

                            if (hasConsent) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Allergies: ${pat.allergies.joinToString(", ").ifBlank { "None" }}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = StatusErrorRed)
                                    )
                                    Text(
                                        text = "Chronic: ${pat.chronicConditions.joinToString(", ").ifBlank { "None" }}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Case A: Unauthorized -> Request Access Card
                if (!hasConsent) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFC2410C), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Clinical Consent Required",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF9A3412)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Under the Digital Personal Data Protection (DPDP) Act and MedIt consent policy, patient medical records are strictly protected. You must submit an explicit access request detailing clinical justification.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF7C2D12),
                                        lineHeight = 17.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                if (pendingForSelected != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFEF3C7),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFFB45309)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Access Request Pending (${pendingForSelected.id})",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF92400E)
                                                    )
                                                )
                                                Text(
                                                    text = "Submitted on ${DateUtils.formatToIndianDate(pendingForSelected.createdAt)}. Awaiting patient approval.",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF92400E)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = requestReason,
                                        onValueChange = { requestReason = it },
                                        label = { Text("Clinical Reason for Access *") },
                                        minLines = 2,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            if (requestReason.isBlank()) {
                                                errorMessage = "Please enter clinical justification."
                                                return@Button
                                            }
                                            isSubmittingRequest = true
                                            val res = repository.createAccessRequest(pat.id, doctorUser, requestReason)
                                            isSubmittingRequest = false
                                            res.onSuccess {
                                                statusMessage = "Access request ${it.id} submitted to patient."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MedItDarkGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("button_request_access"),
                                        enabled = !isSubmittingRequest
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Submit Clinical Access Request")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 5. Case B: Authorized -> Full Clinical Timeline
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Patient Clinical Timeline (${filteredRecords.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )

                            Button(
                                onClick = { onAddClinicalRecordClick(pat.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MedItDarkGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("button_doctor_add_record")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Clinical Note", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Timeline Category Filters
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val filters = listOf(
                                "all" to "All Records",
                                "prescription" to "Prescriptions",
                                "lab_report" to "Investigations",
                                "visit" to "Consultations",
                                "diagnosis" to "Diagnoses"
                            )
                            items(filters) { (key, label) ->
                                val isSelected = selectedFilter == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFilter = key },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MedItDarkGreen,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    if (filteredRecords.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp)),
                                colors = CardDefaults.cardColors(containerColor = SurfacePure)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("No clinical records in this category", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    } else {
                        items(filteredRecords, key = { it.id }) { record ->
                            RecordCard(
                                record = record,
                                onClick = { onRecordClick(record) }
                            )
                        }
                    }
                }
            }
        }
    }
}
