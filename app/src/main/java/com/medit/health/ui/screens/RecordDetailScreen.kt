package com.medit.health.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medit.health.data.model.MedicalRecord
import com.medit.health.data.model.User
import com.medit.health.data.model.UserRole
import com.medit.health.data.repository.MedItRepository
import com.medit.health.ui.components.MedItTopBar
import com.medit.health.ui.theme.*
import com.medit.health.util.DateUtils
import java.io.File

@Composable
fun RecordDetailScreen(
    recordId: String,
    currentUser: User,
    repository: MedItRepository,
    onEditClick: (MedicalRecord) -> Unit,
    onDeleteSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    var record by remember { mutableStateOf<MedicalRecord?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(recordId) {
        record = repository.getRecordById(recordId)
    }

    val currentRecord = record
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            MedItTopBar(
                title = "Medical Record Details",
                currentUser = currentUser,
                onBackClick = onBackClick
            )
        },
        containerColor = BackgroundWarm
    ) { paddingValues ->
        if (currentRecord == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MedItDarkGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(18.dp)
            ) {
                // Header Card
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
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (currentRecord.isVerified) StatusVerifiedBg else StatusSelfReportedBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (currentRecord.isVerified) Icons.Default.Verified else Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = if (currentRecord.isVerified) StatusVerifiedGreen else StatusSelfReportedAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (currentRecord.isVerified) "Verified Hospital Record" else "Patient Self-Reported",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentRecord.isVerified) StatusVerifiedGreen else StatusSelfReportedAmber
                                        )
                                    )
                                }
                            }

                            Text(
                                text = DateUtils.formatToIndianDate(currentRecord.date),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentRecord.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = MedItPrimary600, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentRecord.providerName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            )
                        }

                        if (currentRecord.doctorName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${currentRecord.doctorName} ${if (currentRecord.doctorSpecialty.isNotBlank()) "(${currentRecord.doctorSpecialty})" else ""}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                        }

                        if (currentRecord.costInr > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Consultation / Test Fee: ₹${currentRecord.costInr.toInt()}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MedItDarkGreen
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clinical Narrative / Diagnosis
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfacePure)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Diagnosis & Clinical Notes",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentRecord.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                lineHeight = 21.sp
                            )
                        )

                        if (currentRecord.followUpDate.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MedItMintSoft
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MedItDarkGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Follow-up Advice: ${currentRecord.followUpDate}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MedItDarkGreen
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Prescribed Medicines Section
                if (currentRecord.medications.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfacePure)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Prescribed Medicines (${currentRecord.medications.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            currentRecord.medications.forEachIndexed { i, med ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceSubtle,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${i + 1}. ${med.name} ${med.dosage}",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MedItDarkGreen
                                                )
                                            )
                                            if (med.priceInr > 0) {
                                                Text(
                                                    text = "₹${med.priceInr.toInt()}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                )
                                            }
                                        }
                                        if (med.frequency.isNotBlank() || med.duration.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Schedule: ${med.frequency} • Duration: ${med.duration}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                            )
                                        }
                                        if (med.instructions.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Note: ${med.instructions}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextMuted,
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

                // Vitals & Metrics Section
                if (currentRecord.metrics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfacePure)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Recorded Vitals & Lab Measurements",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            currentRecord.metrics.forEach { (metricKey, metricVal) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = metricKey, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                                    Text(
                                        text = metricVal,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                }
                                HorizontalDivider(color = SurfaceBorderLight, thickness = 0.5.dp)
                            }
                        }
                    }
                }

                // Attachment Preview (Prescription Document)
                if (currentRecord.attachmentPath.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
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
                                Text(
                                    text = "Attached Prescription Scan",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (currentRecord.ocrProcessed) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MedItMintSoft
                                    ) {
                                        Text(
                                            text = "OCR Verified",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MedItDarkGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val attachFile = File(currentRecord.attachmentPath)
                            if (attachFile.exists()) {
                                val bmp = BitmapFactory.decodeFile(attachFile.absolutePath)
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Prescription Attachment",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SurfaceSubtle,
                                        modifier = Modifier.fillMaxWidth().height(100.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("PDF Prescription Document Stored Locally")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions: Edit & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusErrorRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("button_delete_record")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Record")
                    }

                    Button(
                        onClick = { onEditClick(currentRecord) },
                        colors = ButtonDefaults.buttonColors(containerColor = MedItDarkGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("button_edit_record")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Record")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Medical Record?") },
                text = {
                    Text("Are you sure you want to delete '${currentRecord?.title}' from your unified timeline? This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isDeleting = true
                            currentRecord?.id?.let { id ->
                                repository.deleteRecord(id, currentUser)
                            }
                            isDeleting = false
                            showDeleteDialog = false
                            onDeleteSuccess()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusErrorRed),
                        modifier = Modifier.testTag("button_confirm_delete")
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
