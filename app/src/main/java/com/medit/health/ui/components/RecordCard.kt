package com.medit.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medit.health.data.model.MedicalRecord
import com.medit.health.ui.theme.*
import com.medit.health.util.DateUtils

@Composable
fun RecordCard(
    record: MedicalRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (typeColor, typeBg, typeIcon, typeLabel) = when (record.recordType.lowercase()) {
        "prescription" -> Quad(MedItPrimary700, MedItMintSoft, Icons.Default.Medication, "Prescription")
        "lab_report" -> Quad(Color(0xFF0369A1), Color(0xFFF0F9FF), Icons.Default.Science, "Laboratory Report")
        "diagnosis" -> Quad(Color(0xFF7C2D12), Color(0xFFFFF7ED), Icons.Default.AssignmentLate, "Clinical Diagnosis")
        "vital_signs" -> Quad(Color(0xFF0F766E), Color(0xFFF0FDFA), Icons.Default.Favorite, "Vitals & Biometrics")
        "allergy" -> Quad(Color(0xFF991B1B), Color(0xFFFEF2F2), Icons.Default.Warning, "Allergy Alert")
        else -> Quad(MedItDarkGreen, MedItMintSoft, Icons.Default.LocalHospital, "Clinical Consultation")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("record_card_${record.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfacePure),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Type badge + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(typeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = typeLabel,
                            tint = typeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = typeColor
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceSubtle
                    ) {
                        Text(
                            text = record.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = DateUtils.formatToIndianDate(record.date),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Provider & Doctor attribution
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = "Provider",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = record.providerName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (record.doctorName.isNotBlank()) {
                    Text(
                        text = " • ${record.doctorName}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Description preview
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = record.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    lineHeight = 19.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Medications or Vitals preview chips
            if (record.medications.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    record.medications.take(2).forEach { med ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MedItMintSoft,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MedItMintAccent)
                        ) {
                            Text(
                                text = "${med.name} ${med.dosage}".trim(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MedItDarkGreen
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (record.medications.size > 2) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceSubtle
                        ) {
                            Text(
                                text = "+${record.medications.size - 2} more",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Footer Row: Verification badge & Attachment indicator
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Verification pill
                val isVerified = record.isVerified
                val verBg = if (isVerified) StatusVerifiedBg else StatusSelfReportedBg
                val verColor = if (isVerified) StatusVerifiedGreen else StatusSelfReportedAmber
                val verText = if (isVerified) "Verified Clinical Record" else "Self-Reported"
                val verIcon = if (isVerified) Icons.Default.Verified else Icons.Default.EditNote

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = verBg
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = verIcon,
                            contentDescription = "Status",
                            tint = verColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = verText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = verColor,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (record.attachmentPath.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = "Attachment present",
                            tint = MedItPrimary600,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Prescription",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MedItPrimary600,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (record.costInr > 0) {
                        Text(
                            text = "₹${record.costInr.toInt()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
