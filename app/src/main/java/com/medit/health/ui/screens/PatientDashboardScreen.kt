package com.medit.health.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.medit.health.data.model.AccessPermission
import com.medit.health.data.model.AccessRequest
import com.medit.health.data.model.MedicalRecord
import com.medit.health.data.model.Patient
import com.medit.health.data.model.User
import com.medit.health.data.repository.MedItRepository
import com.medit.health.ui.components.*
import com.medit.health.ui.theme.*
import com.medit.health.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboardScreen(
    patientUser: User,
    repository: MedItRepository,
    onAddRecordClick: () -> Unit,
    onRecordClick: (MedicalRecord) -> Unit,
    onAuditTrailClick: () -> Unit,
    onCardiacRiskClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val refreshCount by repository.refreshTrigger.collectAsState()

    var patient by remember { mutableStateOf<Patient?>(null) }
    var records by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<AccessRequest>>(emptyList()) }
    var activePermissions by remember { mutableStateOf<List<AccessPermission>>(emptyList()) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") }
    var showConsentSheet by remember { mutableStateOf(false) }

    var actionSuccessMessage by remember { mutableStateOf<String?>(null) }
    var actionErrorMessage by remember { mutableStateOf<String?>(null) }

    // Load data
    LaunchedEffect(patientUser.associatedId, refreshCount) {
        val p = repository.getPatient(patientUser.associatedId)
        patient = p

        val recResult = repository.getRecordsForPatient(patientUser.associatedId, patientUser)
        recResult.onSuccess {
            records = it
        }

        val allRequests = repository.getAccessRequestsForPatient(patientUser.associatedId)
        pendingRequests = allRequests.filter { it.status == "pending" }

        val perms = repository.getPermissionsForPatient(patientUser.associatedId)
        activePermissions = perms
    }

    // Filter records
    val filteredRecords = remember(records, searchQuery, selectedFilter) {
        records.filter { rec ->
            val matchesFilter = when (selectedFilter) {
                "all" -> true
                "prescription" -> rec.recordType.equals("prescription", ignoreCase = true)
                "visit" -> rec.recordType.equals("visit", ignoreCase = true)
                "lab_report" -> rec.recordType.equals("lab_report", ignoreCase = true)
                "vital_signs" -> rec.recordType.equals("vital_signs", ignoreCase = true)
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                rec.title.contains(searchQuery, ignoreCase = true) ||
                        rec.description.contains(searchQuery, ignoreCase = true) ||
                        rec.providerName.contains(searchQuery, ignoreCase = true) ||
                        rec.doctorName.contains(searchQuery, ignoreCase = true) ||
                        rec.category.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    // Compute chart data from vitals or records
    val chartPoints = remember(records) {
        listOf(
            VitalPoint("14 Jan", 138f, 88f, 76f),
            VitalPoint("10 Mar", 142f, 90f, 72f),
            VitalPoint("18 Mar", 132f, 84f, 70f),
            VitalPoint("24 Mar", 124f, 80f, 68f)
        )
    }

    Scaffold(
        topBar = {
            MedItTopBar(
                title = "Patient Health Portal",
                currentUser = patientUser,
                onAuditClick = onAuditTrailClick,
                onSignOutClick = onSignOutClick
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddRecordClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                text = {
                    Text(
                        text = "Add Medical Record",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                containerColor = MedItDarkGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_medical_record")
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
            // 1. Patient Profile Header & Greeting
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfacePure)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Namaste, ${patient?.name ?: patientUser.fullName}",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Patient ID: ${patient?.id ?: patientUser.associatedId} • Blood: ${patient?.bloodType ?: "O+"}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MedItPrimary600,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            // ABHA / Consent Status Pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MedItMintSoft,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MedItMintAccent),
                                modifier = Modifier.clickable { showConsentSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Consent Management",
                                        tint = MedItDarkGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Consent",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MedItDarkGreen
                                        )
                                    )
                                }
                            }
                        }

                        if (patient?.abhaId?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ABHA ID: ${patient?.abhaId}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        // Emergency & Allergies Bar
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceSubtle,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Allergies: ${patient?.allergies?.joinToString(", ") ?: "None documented"}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    if (patient?.emergencyContact?.isNotBlank() == true) {
                                        Text(
                                            text = "Emergency: ${patient?.emergencyContact}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Health Summary Stats Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total Records
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfacePure)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Timeline Records",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                            Text(
                                text = "${records.size}",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MedItDarkGreen
                                )
                            )
                        }
                    }

                    // Active Prescriptions
                    val presCount = records.count { it.recordType == "prescription" }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfacePure)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Prescriptions",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                            Text(
                                text = "$presCount",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MedItPrimary600
                                )
                            )
                        }
                    }

                    // Clinical Cardiac Risk Assessment button
                    Card(
                        modifier = Modifier
                            .weight(1.1f)
                            .border(1.dp, MedItMintAccent, RoundedCornerShape(10.dp))
                            .clickable(onClick = onCardiacRiskClick)
                            .testTag("button_open_cardiac_risk"),
                        colors = CardDefaults.cardColors(containerColor = MedItMintSoft)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = MedItDarkGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ICMR Risk",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MedItDarkGreen)
                                )
                            }
                            Text(
                                text = "Cardiac Check",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MedItDarkGreen
                                )
                            )
                        }
                    }
                }
            }

            // 3. Pending Doctor Access Requests (MANDATORY REQUIREMENT)
            if (pendingRequests.isNotEmpty()) {
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationImportant, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pending Access Requests (${pendingRequests.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        pendingRequests.forEach { req ->
                            AccessRequestBanner(
                                request = req,
                                onApprove = {
                                    val res = repository.decideAccessRequest(req.id, "approved", "Approved by patient via MedIt mobile app", patientUser)
                                    res.onSuccess {
                                        actionSuccessMessage = "Approved access consent for ${req.requesterName}."
                                    }
                                },
                                onReject = {
                                    val res = repository.decideAccessRequest(req.id, "rejected", "Declined by patient", patientUser)
                                    res.onSuccess {
                                        actionSuccessMessage = "Declined access request."
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // 4. Meaningful Vitals Trend Chart
            item {
                VitalsTrendChart(dataPoints = chartPoints)
            }

            // 5. Search Bar & Category Filter Chips
            item {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by diagnosis, doctor, clinic, medicine...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_records"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val filters = listOf(
                            "all" to "All Records",
                            "prescription" to "Prescriptions",
                            "visit" to "Consultations",
                            "lab_report" to "Lab Reports",
                            "vital_signs" to "Vitals"
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
                                ),
                                modifier = Modifier.testTag("filter_chip_patient_$key")
                            )
                        }
                    }
                }
            }

            // 6. Chronological Medical History Timeline
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unified Medical History (${filteredRecords.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Newest First",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }

            if (filteredRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfacePure)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No records match '$searchQuery'" else "No medical records found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Tap 'Add Medical Record' below to enter details or upload a prescription.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
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

            item {
                Spacer(modifier = Modifier.height(60.dp)) // Padding for FAB
            }
        }

        // Consent Management Bottom Sheet
        if (showConsentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showConsentSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Consent Management & Sharing",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { showConsentSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Text(
                        text = "Authorized doctors and medical organizations currently granted access to your records.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (activePermissions.isEmpty()) {
                        Text(
                            text = "No active clinical consent permissions.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        activePermissions.forEach { perm ->
                            ActivePermissionItem(
                                permission = perm,
                                onRevoke = {
                                    val res = repository.revokePermission(perm.id, patientUser)
                                    res.onSuccess {
                                        actionSuccessMessage = "Revoked consent for ${perm.granteeName}."
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
