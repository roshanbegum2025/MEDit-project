package com.medit.health.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medit.health.data.model.MedicalRecord
import com.medit.health.data.model.MedicationItem
import com.medit.health.data.model.OcrExtractionResult
import com.medit.health.data.model.User
import com.medit.health.data.repository.MedItRepository
import com.medit.health.ui.components.MedItTopBar
import com.medit.health.ui.theme.*
import com.medit.health.util.AttachmentStorage
import com.medit.health.util.DateUtils
import com.medit.health.util.PrescriptionOcrEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PrescriptionUploadScreen(
    patientId: String,
    currentUser: User,
    repository: MedItRepository,
    onRecordSaved: () -> Unit,
    onFallbackToManual: (MedicalRecord?) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var storedAttachmentPath by remember { mutableStateOf("") }

    var isProcessingOcr by remember { mutableStateOf(false) }
    var ocrResult by remember { mutableStateOf<OcrExtractionResult?>(null) }
    var isConfirmedByUser by remember { mutableStateOf(false) }

    // Editable fields for review
    var editTitle by remember { mutableStateOf("") }
    var editHospital by remember { mutableStateOf("") }
    var editDoctor by remember { mutableStateOf("") }
    var editDate by remember { mutableStateOf("") }
    var editDiagnosis by remember { mutableStateOf("") }
    var editFollowUp by remember { mutableStateOf("") }
    val editMedications = remember { mutableStateListOf<MedicationItem>() }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Document Picker launcher
    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            try {
                val input = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(input)
                input?.close()
                selectedBitmap = bmp
                val path = AttachmentStorage.copyUriToInternalStorage(context, uri)
                storedAttachmentPath = path
            } catch (e: Exception) {
                // If PDF or unparseable image, still store the path
                val path = AttachmentStorage.copyUriToInternalStorage(context, uri)
                storedAttachmentPath = path
            }
        }
    }

    // Camera Capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            val path = AttachmentStorage.saveBitmap(context, bitmap)
            storedAttachmentPath = path
        }
    }

    Scaffold(
        topBar = {
            MedItTopBar(
                title = "Prescription OCR Scanner",
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
            // STEP 1: Selection & Capture
            Text(
                text = "1. Select or Capture Prescription",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                text = "Supports paper prescriptions, clinical OPD slips, discharge summaries (JPG, PNG, PDF).",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { cameraLauncher.launch(null) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MedItDarkGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("button_camera_capture")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Photo")
                }

                Button(
                    onClick = { documentPicker.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = MedItDarkGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("button_pick_document")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose File")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Image Preview Card
            if (selectedBitmap != null || storedAttachmentPath.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfacePure)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Prescription Attachment Preview",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StatusVerifiedBg
                            ) {
                                Text(
                                    text = "Ready for OCR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = StatusVerifiedGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (selectedBitmap != null) {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "Prescription Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(SurfaceSubtle, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MedItPrimary600, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Clinical Document Stored", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // OCR Trigger Button
                        if (ocrResult == null) {
                            Button(
                                onClick = {
                                    isProcessingOcr = true
                                    coroutineScope.launch {
                                        delay(800) // Simulated optical processing
                                        val result = if (selectedBitmap != null) {
                                            PrescriptionOcrEngine.processPrescription(context, selectedBitmap)
                                        } else if (selectedUri != null) {
                                            PrescriptionOcrEngine.processPrescriptionUri(context, selectedUri!!)
                                        } else {
                                            PrescriptionOcrEngine.createManualFallbackResult()
                                        }

                                        ocrResult = result
                                        editTitle = result.title
                                        editHospital = result.providerName
                                        editDoctor = result.doctorName
                                        editDate = result.date
                                        editDiagnosis = result.diagnosis
                                        editFollowUp = result.followUpDate
                                        editMedications.clear()
                                        editMedications.addAll(result.medications)
                                        isProcessingOcr = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MedItPrimary700),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("button_run_ocr"),
                                enabled = !isProcessingOcr
                            ) {
                                if (isProcessingOcr) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Extracting Clinical Data...")
                                } else {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Process Prescription with OCR")
                                }
                            }
                        }
                    }
                }
            } else {
                // Empty state card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfacePure)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MedItPrimary600,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Document Selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Take a photo or choose an image/PDF from your phone to start OCR analysis.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // STEP 2: OCR Review & Editing
            if (ocrResult != null) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "2. Review & Confirm Extracted Information",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Inspect and edit all fields. OCR is never automatically treated as medically verified.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Confidence Banner
                val res = ocrResult!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (res.hasUncertainties) Color(0xFFFDE68A) else MedItMintAccent, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = if (res.hasUncertainties) Color(0xFFFFFBEB) else MedItMintSoft)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (res.hasUncertainties) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (res.hasUncertainties) Color(0xFFB45309) else MedItDarkGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Optical Extraction Confidence: ${(res.confidenceScore * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.hasUncertainties) Color(0xFFB45309) else MedItDarkGreen
                                )
                            )
                        }

                        if (res.uncertainFields.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            res.uncertainFields.forEach { unc ->
                                Text(
                                    text = "• $unc",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF92400E),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Editable Fields
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Prescription Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ocr_edit_title"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editHospital,
                    onValueChange = { editHospital = it },
                    label = { Text("Detected Hospital / Clinic *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ocr_edit_hospital"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editDoctor,
                        onValueChange = { editDoctor = it },
                        label = { Text("Doctor Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f).testTag("ocr_edit_doctor"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = editDate,
                        onValueChange = { editDate = it },
                        label = { Text("Date") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f).testTag("ocr_edit_date"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editDiagnosis,
                    onValueChange = { editDiagnosis = it },
                    label = { Text("Clinical Diagnosis *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("ocr_edit_diagnosis"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Extracted Medicines (Editable)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Extracted Medicines (${editMedications.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(
                        onClick = {
                            editMedications.add(MedicationItem("", "10mg", "Once daily", "14 Days", "Take with water"))
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }

                editMedications.forEachIndexed { i, med ->
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
                                    onValueChange = { editMedications[i] = med.copy(name = it) },
                                    label = { Text("Medicine ${i + 1}") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedTextField(
                                    value = med.dosage,
                                    onValueChange = { editMedications[i] = med.copy(dosage = it) },
                                    label = { Text("Dosage") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                IconButton(
                                    onClick = { editMedications.removeAt(i) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = StatusErrorRed, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = med.frequency,
                                    onValueChange = { editMedications[i] = med.copy(frequency = it) },
                                    label = { Text("Schedule") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = med.instructions,
                                    onValueChange = { editMedications[i] = med.copy(instructions = it) },
                                    label = { Text("Instructions") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Patient confirmation checkbox (MANDATORY REQUIREMENT: Require user to review and confirm)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceSubtle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { isConfirmedByUser = !isConfirmedByUser }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isConfirmedByUser,
                            onCheckedChange = { isConfirmedByUser = it },
                            colors = CheckboxDefaults.colors(checkedColor = MedItDarkGreen),
                            modifier = Modifier.testTag("checkbox_confirm_ocr")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "I have reviewed the extracted medicines, dosages, and instructions against the physical prescription and confirm accuracy.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        )
                    }
                }

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

                // Save confirmed record
                Button(
                    onClick = {
                        if (!isConfirmedByUser) {
                            errorMessage = "Please review all fields and check the confirmation box before saving."
                            return@Button
                        }
                        if (editTitle.isBlank() || editHospital.isBlank() || editDiagnosis.isBlank()) {
                            errorMessage = "Title, Hospital, and Diagnosis fields cannot be blank."
                            return@Button
                        }

                        isSaving = true
                        val newRecord = MedicalRecord(
                            id = "",
                            patientId = patientId,
                            recordType = "prescription",
                            title = editTitle.trim(),
                            providerName = editHospital.trim(),
                            doctorName = editDoctor.trim(),
                            date = editDate.trim(),
                            description = editDiagnosis.trim(),
                            category = res.category,
                            medications = editMedications.toList(),
                            verificationStatus = "self_reported", // OCR assisted patient self-upload
                            createdByRole = currentUser.role.name.lowercase(),
                            createdById = currentUser.associatedId,
                            followUpDate = editFollowUp.trim(),
                            attachmentPath = storedAttachmentPath,
                            attachmentType = "image/jpeg",
                            ocrProcessed = true,
                            costInr = 350.0
                        )

                        val result = repository.createRecord(newRecord, currentUser)
                        isSaving = false
                        result.onSuccess {
                            onRecordSaved()
                        }.onFailure { ex ->
                            errorMessage = ex.message ?: "Failed to save prescription record."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedItDarkGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("button_confirm_save_ocr"),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Confirm & Save to Health Timeline",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fallback manual entry button (MANDATORY REQUIREMENT)
            OutlinedButton(
                onClick = {
                    val preFill = ocrResult?.let { res ->
                        MedicalRecord(
                            id = "",
                            patientId = patientId,
                            recordType = "prescription",
                            title = editTitle.ifBlank { res.title },
                            providerName = editHospital.ifBlank { res.providerName },
                            doctorName = editDoctor.ifBlank { res.doctorName },
                            date = editDate.ifBlank { res.date },
                            description = editDiagnosis.ifBlank { res.diagnosis },
                            category = res.category,
                            medications = if (editMedications.isNotEmpty()) editMedications.toList() else res.medications,
                            attachmentPath = storedAttachmentPath
                        )
                    }
                    onFallbackToManual(preFill)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_manual_fallback")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Switch to Manual Entry Fallback")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
