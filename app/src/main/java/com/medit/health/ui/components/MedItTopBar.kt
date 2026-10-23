package com.medit.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medit.health.data.model.User
import com.medit.health.data.model.UserRole
import com.medit.health.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedItTopBar(
    title: String,
    currentUser: User?,
    onBackClick: (() -> Unit)? = null,
    onAuditClick: (() -> Unit)? = null,
    onSignOutClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1
                    )
                    if (currentUser != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val roleBg = when (currentUser.role) {
                            UserRole.DOCTOR -> MedItMintAccent
                            UserRole.PROVIDER -> Color(0xFF93C5FD)
                            UserRole.PATIENT -> MedItMintLight
                            UserRole.SYSTEM -> Color.LightGray
                        }
                        val roleTextColor = when (currentUser.role) {
                            UserRole.DOCTOR -> MedItDarkGreen
                            UserRole.PROVIDER -> Color(0xFF1E3A8A)
                            UserRole.PATIENT -> MedItDarkGreen
                            UserRole.SYSTEM -> Color.Black
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = roleBg,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = currentUser.role.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = roleTextColor
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (currentUser != null && currentUser.organization.isNotBlank()) {
                    Text(
                        text = currentUser.organization,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MedItMintAccent,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = Color.White
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MedItPrimary700),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "MedIt Shield",
                        tint = MedItMintAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        actions = {
            if (onAuditClick != null) {
                IconButton(
                    onClick = onAuditClick,
                    modifier = Modifier.testTag("top_bar_audit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "View Audit Logs",
                        tint = Color.White
                    )
                }
            }
            if (onSignOutClick != null) {
                IconButton(
                    onClick = onSignOutClick,
                    modifier = Modifier.testTag("top_bar_signout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = Color.White
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MedItDarkGreen,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        modifier = modifier
    )
}
