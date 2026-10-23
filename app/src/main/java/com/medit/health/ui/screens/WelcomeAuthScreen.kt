package com.medit.health.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medit.health.data.model.User
import com.medit.health.data.model.UserRole
import com.medit.health.data.repository.MedItRepository
import com.medit.health.ui.theme.*

@Composable
fun WelcomeAuthScreen(
    repository: MedItRepository,
    onAuthSuccess: (User) -> Unit
) {
    var isSignInTab by remember { mutableStateOf(true) }
    var selectedRole by remember { mutableStateOf(UserRole.PATIENT) }

    // Sign In form state
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Sign Up form state
    var regFullName by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regDob by remember { mutableStateOf("1992-06-15") }
    var regGender by remember { mutableStateOf("Female") }
    var regBloodType by remember { mutableStateOf("O+") }
    var regPhone by remember { mutableStateOf("+91 98765 43210") }
    var regEmergency by remember { mutableStateOf("Amit Kumar (Brother) - +91 98765 43211") }
    var regAbha by remember { mutableStateOf("91-5521-8843-1029") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm)
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // MedIt Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MedItDarkGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "MedIt Shield",
                    tint = MedItMintAccent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "MedIt",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MedItDarkGreen,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = "Unified Patient Health Records",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Educational Intro Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MedItMintAccent, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MedItMintSoft),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MedItDarkGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Patient-Controlled Consent Architecture",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MedItDarkGreen
                        )
                    )
                    Text(
                        text = "Medical records remain private. Doctors and clinics can view health history only after explicit patient consent.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MedItPrimary800,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Auth Tabs: Sign In vs Create Account
        TabRow(
            selectedTabIndex = if (isSignInTab) 0 else 1,
            containerColor = SurfaceSubtle,
            contentColor = MedItDarkGreen,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = isSignInTab,
                onClick = {
                    isSignInTab = true
                    errorMessage = null
                },
                text = {
                    Text(
                        "Sign In",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSignInTab) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                modifier = Modifier.testTag("tab_sign_in")
            )
            Tab(
                selected = !isSignInTab,
                onClick = {
                    isSignInTab = false
                    errorMessage = null
                },
                text = {
                    Text(
                        "Create Account",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (!isSignInTab) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                modifier = Modifier.testTag("tab_create_account")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSignInTab) {
            // Quick Demo Credentials Fill Chips (Hackathon Showcase)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfacePure)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MedItPrimary600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hackathon Presentation Credentials:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MedItMintSoft,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MedItMintAccent),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    usernameInput = "admin123"
                                    passwordInput = "Doctor@MedIt2026"
                                    errorMessage = null
                                }
                                .testTag("quick_fill_doctor")
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Doctor Workspace",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MedItDarkGreen
                                    )
                                )
                                Text(
                                    text = "admin123",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceSubtle,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    usernameInput = "sarah.jenkins"
                                    passwordInput = "patient123"
                                    errorMessage = null
                                }
                                .testTag("quick_fill_patient")
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Patient Portal",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "sarah.jenkins",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Username input
            OutlinedTextField(
                value = usernameInput,
                onValueChange = {
                    usernameInput = it
                    errorMessage = null
                },
                label = { Text("Username or Patient ID") },
                placeholder = { Text("e.g. sarah.jenkins or admin123") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_login_username"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password input
            OutlinedTextField(
                value = passwordInput,
                onValueChange = {
                    passwordInput = it
                    errorMessage = null
                },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = TextMuted
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_login_password"),
                shape = RoundedCornerShape(10.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusErrorBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = StatusErrorRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = StatusErrorRed)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (usernameInput.isBlank() || passwordInput.isBlank()) {
                        errorMessage = "Please enter both username and password."
                        return@Button
                    }
                    isSubmitting = true
                    val result = repository.login(usernameInput, passwordInput)
                    isSubmitting = false
                    result.onSuccess { user ->
                        onAuthSuccess(user)
                    }.onFailure { ex ->
                        errorMessage = ex.message ?: "Authentication failed."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MedItDarkGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("button_submit_login"),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Sign In to MedIt",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)
                    )
                }
            }
        } else {
            // Create Account Form (Patient Registration)
            Text(
                text = "New Patient Registration",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Your digital health records will be linked to your unique Patient ID & ABHA account.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = regFullName,
                onValueChange = { regFullName = it },
                label = { Text("Full Name *") },
                placeholder = { Text("e.g. Priya Sundaram") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_reg_name"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = regDob,
                    onValueChange = { regDob = it },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f).testTag("input_reg_dob"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = regBloodType,
                    onValueChange = { regBloodType = it },
                    label = { Text("Blood Group") },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f).testTag("input_reg_blood"),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = regPhone,
                onValueChange = { regPhone = it },
                label = { Text("Mobile Phone Number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth().testTag("input_reg_phone"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = regEmergency,
                onValueChange = { regEmergency = it },
                label = { Text("Emergency Contact") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_reg_emergency"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = regAbha,
                onValueChange = { regAbha = it },
                label = { Text("ABHA ID (Ayushman Bharat, Optional)") },
                placeholder = { Text("91-XXXX-XXXX-XXXX") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_reg_abha"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = regUsername,
                onValueChange = { regUsername = it },
                label = { Text("Desired Username *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_reg_username"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = regPassword,
                onValueChange = { regPassword = it },
                label = { Text("Password *") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_reg_password"),
                shape = RoundedCornerShape(10.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusErrorBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = StatusErrorRed),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (regFullName.isBlank() || regUsername.isBlank() || regPassword.isBlank()) {
                        errorMessage = "Please fill in all required fields (Name, Username, Password)."
                        return@Button
                    }
                    isSubmitting = true
                    val result = repository.registerPatient(
                        username = regUsername,
                        password = regPassword,
                        fullName = regFullName,
                        email = "${regUsername.trim()}@medit-demo.org",
                        dob = regDob,
                        gender = regGender,
                        bloodType = regBloodType,
                        phone = regPhone,
                        emergencyContact = regEmergency,
                        abhaId = regAbha
                    )
                    isSubmitting = false
                    result.onSuccess { user ->
                        onAuthSuccess(user)
                    }.onFailure { ex ->
                        errorMessage = ex.message ?: "Account registration failed."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MedItDarkGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("button_submit_register"),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Register & Open Patient Dashboard",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Privacy footnote
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MedItPrimary600, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "ABDM Compliant • End-to-End Consent Architecture",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = TextMuted
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
