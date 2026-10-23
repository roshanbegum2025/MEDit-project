package com.medit.health.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medit.health.data.model.MedicalRecord
import com.medit.health.data.model.MedicationItem
import com.medit.health.data.model.User
import com.medit.health.data.repository.MedItRepository
import com.medit.health.ui.components.MedItTopBar
import com.medit.health.ui.theme.*
import com.medit.health.util.AttachmentStorage
import com.medit.health.util.DateUtils

val MEDICAL_CATEGORIES = listOf(
    "General Medicine",
    "Cardiology",
    "Endocrinology & Diabetology",
    "Hematology & Biochemistry",
    "Pulmonology",
    "Orthopedics",
    "Gastroenterology",
    "Dermatology",
    "Neurology",
    "Preventative Health"
)

val COMMON_INDIAN_HOSPITALS = listOf(
    "Apollo Hospitals, New Delhi",
    "Fortis Escorts Heart Institute",
    "Max Super Speciality Hospital",
    "All India Institute of Medical Sciences (AIIMS)",
    "Dr. Lal PathLabs & Diagnostics",
    "Thyrocare Technologies",
    "St. Jude Medical Center",
    "Metro Health Clinic"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualRecordEntryScreen(
    patientId: String,
    currentUser: User,
    repository: MedItRepository,
    onRecordSaved: () -> Unit,
    onBackClick: () -> Unit,
    initialPreFill: MedicalRecord? = null
) {
    val context = LocalContext.current

    var recordType by remember { mutableStateOf(initialPreFill?.recordType ?: if (currentUser.role.name == "PATIENT") "prescription" else "visit") }
    var title by remember { mutableStateOf(initialPreFill?.title ?: "") }
    var category by remember { mutableStateOf(initialPreFill?.category ?: "General Medicine") }
    var providerName by remember { mutableStateOf(initialPreFill?.providerName ?: COMMON_INDIAN_HOSPITALS[0]) }
    var doctorName by remember { mutableStateOf(initialPreFill?.doctorName ?: "") }
    var doctorSpecialty by remember { mutableStateOf(initialPreFill?.doctorSpecialty ?: "") }
    var date by remember { mutableStateOf(initialPreFill?.date ?: DateUtils.todayIso()) }
    var description by remember { mutableStateOf(initialPreFill?.description ?: "") }
    var followUpDate by remember { mutableStateOf(initialPreFill?.followUpDate ?: "Review in 2 weeks") }
    var costInrText by remember { mutableStateOf(if ((initialPreFill?.costInr ?: 0.0) > 0) initialPreFill!!.costInr.toInt().toString() else "500") }

    // Dynamic medications list
    val medications = remember {
        mutableStateListOf<MedicationItem>().apply {
            if (initialPreFill?.medications != null && initialPreFill.medications.isNotEmpty()) {
                addAll(initialPreFill.medications)
            } else {
                add(MedicationItem("Telmisartan", "40mg", "Once daily morning", "30 Days", "Take with water"))
            }
        }
    }

    // Dynamic metrics (Vitals / Lab values)
    var bpText by remember { mutableStateOf(initialPreFill?.metrics?.get("Blood Pressure") ?: "120/80 mmHg") }
    var pulseText by remember { mutableStateOf(initialPreFill?.metrics?.get("Pulse") ?: "72 bpm") }
    var sugarText by remember { mutableStateOf(initialPreFill?.metrics?.get("Fasting Blood Sugar") ?: "") }

    // Attachment
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentPath by remember { mutableStateOf(initialPreFill?.attachmentPath ?: "") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachmentUri = uri
            val savedPath = AttachmentStorage.copyUriToInternalStorage(context, uri)
            attachmentPath = savedPath
        }
    }

    var categoryExpanded by remember { mutableStateOf(false) }
    var hospitalExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            MedItTopBar(
                title = if (initialPreFill != null) "Review & Save Record" else "New Medical Record",
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
            Text(
                text = "Clinical Consultation Details",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                text = "Fields marked with * are required. Record will be anchored chronologically.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Record Type Selector
            Text(
                text = "Record Type *",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "prescription" to "Prescription",
                    "visit" to "OPD Visit",
                    "lab_report" to "Lab Report",
                    "diagnosis" to "Diagnosis"
                ).forEach { (typeVal, label) ->
                    val isSelected = recordType == typeVal
                    FilterChip(
                        selected = isSelected,
                        onClick = { recordType = typeVal },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MedItDarkGreen,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_$typeVal")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                label = { Text("Record Title *") },
                placeholder = { Text("e.g. Cardiology OPD Follow-up & Prescription") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_record_title"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Medical Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Medical Category *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("dropdown_record_category"),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    MEDICAL_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hospital / Clinic Name Dropdown & Custom Edit
            ExposedDropdownMenuBox(
                expanded = hospitalExpanded,
                onExpandedChange = { hospitalExpanded = !hospitalExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = providerName,
                    onValueChange = {
                        providerName = it
                        errorMessage = null
                    },
                    label = { Text("Hospital or Clinic Name *") },
                    placeholder = { Text("e.g. Apollo Hospitals, New Delhi") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hospitalExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("input_hospital_name"),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = hospitalExpanded,
                    onDismissRequest = { hospitalExpanded = false }
                ) {
                    COMMON_INDIAN_HOSPITALS.forEach { hosp ->
                        DropdownMenuItem(
                            text = { Text(hosp) },
                            onClick = {
                                providerName = hosp
                                hospitalExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Doctor details row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Doctor Name") },
                    placeholder = { Text("Dr. Ananya Sharma") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("input_doctor_name"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = doctorSpecialty,
                    onValueChange = { doctorSpecialty = it },
                    label = { Text("Specialty") },
                    placeholder = { Text("Cardiology") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("input_doctor_specialty"),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Consultation Date & Cost (₹)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("input_consultation_date"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = costInrText,
                    onValueChange = { costInrText = it },
                    label = { Text("Fee / Cost (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("input_cost_inr"),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Diagnosis or reason for visit
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    errorMessage = null
                },
                label = { Text("Diagnosis or Reason for Visit *") },
                placeholder = { Text("Detailed clinical findings, test observations, and patient instructions...") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_diagnosis_description"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Prescribed Medicines Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prescribed Medicines",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = {
                        medications.add(MedicationItem("", "10mg", "Once daily", "14 Days", "Take with water"))
                    },
                    modifier = Modifier.testTag("button_add_medicine")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Medicine")
                }
            }

            medications.forEachIndexed { index, med ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfacePure)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = med.name,
                                onValueChange = { medications[index] = med.copy(name = it) },
                                label = { Text("Medicine Name") },
                                placeholder = { Text("e.g. Paracetamol") },
                                singleLine = true,
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedTextField(
                                value = med.dosage,
                                onValueChange = { medications[index] = med.copy(dosage = it) },
                                label = { Text("Dosage") },
                                placeholder = { Text("650mg") },
                                singleLine = true,
                                modifier = Modifier.weight(0.7f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            IconButton(
                                onClick = { medications.removeAt(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = StatusErrorRed, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = med.frequency,
                                onValueChange = { medications[index] = med.copy(frequency = it) },
                                label = { Text("Frequency") },
                                placeholder = { Text("Twice daily") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = med.duration,
                                onValueChange = { medications[index] = med.copy(duration = it) },
                                label = { Text("Duration") },
                                placeholder = { Text("5 Days") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clinical Metrics / Vitals
            Text(
                text = "Clinical Vitals & Metrics (Optional)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = bpText,
                    onValueChange = { bpText = it },
                    label = { Text("Blood Pressure") },
                    placeholder = { Text("120/80 mmHg") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = pulseText,
                    onValueChange = { pulseText = it },
                    label = { Text("Pulse Rate") },
                    placeholder = { Text("72 bpm") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Follow-up Date
            OutlinedTextField(
                value = followUpDate,
                onValueChange = { followUpDate = it },
                label = { Text("Follow-up / Review Advice") },
                placeholder = { Text("e.g. Review after 2 weeks with repeat lipid profile") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Attachment Section
            Text(
                text = "Prescription Attachment (Optional)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfacePure,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (attachmentPath.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = if (attachmentPath.isNotBlank()) StatusVerifiedGreen else TextMuted
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (attachmentPath.isNotBlank()) "Document Attached" else "No file selected",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (attachmentPath.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                color = if (attachmentPath.isNotBlank()) StatusVerifiedGreen else TextSecondary
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = { filePicker.launch("*/*") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("button_pick_attachment")
                    ) {
                        Text(if (attachmentPath.isNotBlank()) "Change" else "Choose File")
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
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

            Spacer(modifier = Modifier.height(24.dp))

            // Save Action Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Please enter a record title."
                        return@Button
                    }
                    if (providerName.isBlank()) {
                        errorMessage = "Please enter a hospital or clinic name."
                        return@Button
                    }
                    if (description.trim().length < 5) {
                        errorMessage = "Diagnosis or reason for visit must be at least 5 characters."
                        return@Button
                    }

                    isSaving = true
                    val metricsMap = mutableMapOf<String, String>()
                    if (bpText.isNotBlank()) metricsMap["Blood Pressure"] = bpText.trim()
                    if (pulseText.isNotBlank()) metricsMap["Pulse"] = pulseText.trim()
                    if (sugarText.isNotBlank()) metricsMap["Fasting Blood Sugar"] = sugarText.trim()

                    val cleanMeds = medications.filter { it.name.isNotBlank() }
                    val costVal = costInrText.toDoubleOrNull() ?: 0.0

                    val newRecord = MedicalRecord(
                        id = initialPreFill?.id ?: "",
                        patientId = patientId,
                        recordType = recordType,
                        title = title.trim(),
                        providerName = providerName.trim(),
                        doctorName = doctorName.trim(),
                        doctorSpecialty = doctorSpecialty.trim(),
                        date = date.trim(),
                        description = description.trim(),
                        category = category,
                        metrics = metricsMap,
                        medications = cleanMeds,
                        verificationStatus = if (currentUser.role.name == "PATIENT") "self_reported" else "verified",
                        createdByRole = currentUser.role.name.lowercase(),
                        createdById = currentUser.associatedId,
                        followUpDate = followUpDate.trim(),
                        attachmentPath = attachmentPath,
                        attachmentType = "application/octet-stream",
                        ocrProcessed = initialPreFill?.ocrProcessed ?: false,
                        costInr = costVal
                    )

                    val result = if (initialPreFill?.id?.isNotBlank() == true) {
                        repository.updateRecord(newRecord, currentUser)
                    } else {
                        repository.createRecord(newRecord, currentUser)
                    }

                    isSaving = false
                    result.onSuccess {
                        onRecordSaved()
                    }.onFailure { ex ->
                        errorMessage = ex.message ?: "Failed to save medical record."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MedItDarkGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("button_save_medical_record"),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Commit Record to Patient Timeline",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
