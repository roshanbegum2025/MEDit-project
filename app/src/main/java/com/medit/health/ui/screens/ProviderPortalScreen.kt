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
import com.medit.health.data.model.MedicalRecord
import com.medit.health.data.model.Patient
import com.medit.health.data.model.User
import com.medit.health.data.repository.MedItRepository
import com.medit.health.ui.components.MedItTopBar
import com.medit.health.ui.components.RecordCard
import com.medit.health.ui.theme.*

@Composable
fun ProviderPortalScreen(
    providerUser: User,
    repository: MedItRepository,
    onAddRecordClick: (String) -> Unit,
    onRecordClick: (MedicalRecord) -> Unit,
    onAuditTrailClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val refreshCount by repository.refreshTrigger.collectAsState()

    var allPatients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var authoredRecords by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }

    LaunchedEffect(providerUser.associatedId, refreshCount) {
        allPatients = repository.getAllPatients()
        if (selectedPatient == null && allPatients.isNotEmpty()) {
            selectedPatient = allPatients.first()
        }
        val recResult = repository.getRecordsForPatient(selectedPatient?.id ?: "PAT-101", providerUser)
        recResult.onSuccess {
            authoredRecords = it
        }.onFailure {
            authoredRecords = emptyList()
        }
    }

    LaunchedEffect(selectedPatient, refreshCount) {
        val pat = selectedPatient
        if (pat != null) {
            val recResult = repository.getRecordsForPatient(pat.id, providerUser)
            recResult.onSuccess {
                authoredRecords = it
            }.onFailure {
                authoredRecords = emptyList()
            }
        }
    }

    Scaffold(
        topBar = {
            MedItTopBar(
                title = "Healthcare Provider Portal",
                currentUser = providerUser,
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
            // Provider Header
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfacePure)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Biotech, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = providerUser.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "${providerUser.specialty} • Certified ABDM Diagnostic Facility",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }
            }

            // Patient Selection
            item {
                Text(
                    text = "Select Patient for Investigation Entry",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allPatients) { pat ->
                        val isSelected = selectedPatient?.id == pat.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MedItDarkGreen else SurfacePure,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MedItDarkGreen else SurfaceBorder),
                            modifier = Modifier.clickable { selectedPatient = pat }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
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

            // Authored Records Section & Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Clinical Records for ${selectedPatient?.name ?: "Patient"}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Button(
                        onClick = { selectedPatient?.let { onAddRecordClick(it.id) } },
                        colors = ButtonDefaults.buttonColors(containerColor = MedItDarkGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("button_provider_add_report")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Lab Report", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (authoredRecords.isEmpty()) {
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
                            Icon(Icons.Default.Science, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No reports authored yet for this patient", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(authoredRecords, key = { it.id }) { record ->
                    RecordCard(
                        record = record,
                        onClick = { onRecordClick(record) }
                    )
                }
            }
        }
    }
}
