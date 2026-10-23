package com.medit.health.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.medit.health.data.model.AuditLog
import com.medit.health.data.model.User
import com.medit.health.data.model.UserRole
import com.medit.health.data.repository.MedItRepository
import com.medit.health.ui.components.MedItTopBar
import com.medit.health.ui.theme.*
import com.medit.health.util.DateUtils

@Composable
fun AuditTrailScreen(
    currentUser: User,
    repository: MedItRepository,
    onBackClick: () -> Unit
) {
    var logs by remember { mutableStateOf<List<AuditLog>>(emptyList()) }

    LaunchedEffect(currentUser) {
        val patientFilter = if (currentUser.role == UserRole.PATIENT) currentUser.associatedId else null
        logs = repository.getAuditLogs(patientFilter)
    }

    Scaffold(
        topBar = {
            MedItTopBar(
                title = "Security & Consent Audit Trail",
                currentUser = currentUser,
                onBackClick = onBackClick
            )
        },
        containerColor = BackgroundWarm
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Privacy Safeguards Notice
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MedItMintAccent, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MedItMintSoft)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MedItDarkGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Immutable Access & Consent Audit Trail",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MedItDarkGreen
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Every clinical record view, consent grant, revocation, and doctor request is logged with timestamp and actor identity. Sensitive clinical diagnoses and medication notes are never included in audit payloads to protect patient confidentiality.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MedItPrimary800,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Operational Event History (${logs.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (logs.isEmpty()) {
                item {
                    Text(
                        text = "No audit log events recorded yet.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    val (icon, tint, bg) = when (log.action) {
                        "CONSENT_GRANTED" -> Triple(Icons.Default.CheckCircle, StatusVerifiedGreen, StatusVerifiedBg)
                        "CONSENT_REVOKED" -> Triple(Icons.Default.Block, StatusErrorRed, StatusErrorBg)
                        "ACCESS_REQUESTED" -> Triple(Icons.Default.Security, Color(0xFFB45309), Color(0xFFFEF3C7))
                        "RECORD_CREATED" -> Triple(Icons.Default.AddCircle, MedItPrimary600, MedItMintSoft)
                        "RECORD_DELETED" -> Triple(Icons.Default.Delete, StatusErrorRed, StatusErrorBg)
                        "LOGIN" -> Triple(Icons.Default.Lock, MedItDarkGreen, MedItMintSoft)
                        else -> Triple(Icons.Default.Visibility, Color(0xFF0369A1), Color(0xFFF0F9FF))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                            .testTag("audit_item_${log.id}"),
                        colors = CardDefaults.cardColors(containerColor = SurfacePure)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.action.replace("_", " "),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = tint
                                        )
                                    )
                                    Text(
                                        text = DateUtils.formatTimestamp(log.timestamp),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary,
                                        fontSize = 13.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Actor: ${log.actorName} (${log.actorRole.uppercase()}) • Patient: ${log.patientId}",
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
}
