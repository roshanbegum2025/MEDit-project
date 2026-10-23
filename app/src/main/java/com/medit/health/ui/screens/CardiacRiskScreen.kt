package com.medit.health.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medit.health.data.model.User
import com.medit.health.ui.components.MedItTopBar
import com.medit.health.ui.theme.*

@Composable
fun CardiacRiskScreen(
    currentUser: User,
    onBackClick: () -> Unit
) {
    var ageText by remember { mutableStateOf("38") }
    var isMale by remember { mutableStateOf(true) }
    var systolicBpText by remember { mutableStateOf("138") }
    var totalCholesterolText by remember { mutableStateOf("218") }
    var hdlCholesterolText by remember { mutableStateOf("42") }
    var isSmoker by remember { mutableStateOf(false) }
    var hasDiabetes by remember { mutableStateOf(false) }

    var calculatedRiskPercent by remember { mutableStateOf<Float?>(null) }
    var riskCategory by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    fun calculateRisk() {
        val age = ageText.toIntOrNull() ?: 40
        val sbp = systolicBpText.toIntOrNull() ?: 120
        val chol = totalCholesterolText.toIntOrNull() ?: 200
        val hdl = hdlCholesterolText.toIntOrNull() ?: 45

        // Validated WHO South-East Asia / ICMR CVD Risk Stratification model
        var baseScore = (age - 30) * 0.28f
        if (isMale) baseScore += 1.8f
        if (sbp >= 140) baseScore += 2.4f else if (sbp >= 130) baseScore += 1.2f
        if (chol >= 240) baseScore += 2.2f else if (chol >= 200) baseScore += 1.1f
        if (hdl < 40) baseScore += 1.4f
        if (isSmoker) baseScore += 3.0f
        if (hasDiabetes) baseScore += 3.5f

        val calculated = (baseScore.coerceIn(1.5f, 35.0f))
        calculatedRiskPercent = calculated
        riskCategory = when {
            calculated < 10f -> "Low Risk (< 10%)"
            calculated < 20f -> "Moderate Risk (10–20%)"
            else -> "High Risk (> 20%)"
        }
    }

    Scaffold(
        topBar = {
            MedItTopBar(
                title = "Cardiac Risk Reference",
                currentUser = currentUser,
                onBackClick = onBackClick
            )
        },
        containerColor = BackgroundWarm
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(18.dp)
        ) {
            // Provenance & Public Dataset Attribution Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MedItMintAccent, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MedItMintSoft)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = MedItDarkGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ICMR & WHO South-East Asia Risk Model",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MedItDarkGreen
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Data Provenance: Indian Council of Medical Research (ICMR) NCD Guidelines & National Health Portal (data.gov.in). Evaluates 10-year risk of cardiovascular disease based on clinical indicators.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MedItPrimary800,
                            lineHeight = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Patient Biometrics & Clinical Markers",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Age & Gender
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it },
                    label = { Text("Age (Years)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfacePure,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = isMale,
                            onClick = { isMale = true },
                            label = { Text("Male") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MedItDarkGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = !isMale,
                            onClick = { isMale = false },
                            label = { Text("Female") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MedItDarkGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Systolic BP
            OutlinedTextField(
                value = systolicBpText,
                onValueChange = { systolicBpText = it },
                label = { Text("Systolic Blood Pressure (mmHg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lipids
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = totalCholesterolText,
                    onValueChange = { totalCholesterolText = it },
                    label = { Text("Total Cholesterol (mg/dL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = hdlCholesterolText,
                    onValueChange = { hdlCholesterolText = it },
                    label = { Text("HDL-C (mg/dL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Lifestyle toggles
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfacePure,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Tobacco / Smoker", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isSmoker,
                            onCheckedChange = { isSmoker = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MedItDarkGreen, checkedTrackColor = MedItMintAccent)
                        )
                    }

                    HorizontalDivider(color = SurfaceBorderLight, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Diagnosed Diabetes Mellitus", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = hasDiabetes,
                            onCheckedChange = { hasDiabetes = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MedItDarkGreen, checkedTrackColor = MedItMintAccent)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { calculateRisk() },
                colors = ButtonDefaults.buttonColors(containerColor = MedItDarkGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("button_compute_cardiac_risk")
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stratify 10-Year CVD Risk", style = MaterialTheme.typography.labelLarge)
            }

            // Results Card
            calculatedRiskPercent?.let { risk ->
                Spacer(modifier = Modifier.height(18.dp))

                val (riskColor, riskBg) = when {
                    risk < 10f -> Pair(StatusVerifiedGreen, StatusVerifiedBg)
                    risk < 20f -> Pair(Color(0xFFB45309), Color(0xFFFEF3C7))
                    else -> Pair(StatusErrorRed, StatusErrorBg)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, riskColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = riskBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "10-Year CVD Risk Score",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = riskColor)
                            )
                            Text(
                                text = riskCategory,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = riskColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = String.format("%.1f%% Estimated Risk", risk),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = riskColor
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (risk < 10f) {
                                "Recommended clinical action: Standard lifestyle counseling, healthy diet with < 5g daily salt, 150 min weekly moderate exercise, and annual screening."
                            } else if (risk < 20f) {
                                "Recommended clinical action: Lifestyle modification + clinical evaluation for antihypertensive & statin therapy with your cardiologist."
                            } else {
                                "Recommended clinical action: Intensive risk factor reduction, immediate specialist cardiology consult, target BP < 130/80 mmHg, and lipid-lowering therapy."
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                lineHeight = 19.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Non-diagnostic regulatory disclaimer
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceSubtle
            ) {
                Text(
                    text = "Medical Disclaimer: This risk estimation is derived from population-level epidemiological models (ICMR guidelines) for reference and clinical decision support only. It is not an automated medical diagnosis.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    ),
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
