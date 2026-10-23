package com.medit.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.medit.health.data.model.MedicalRecord
import com.medit.health.data.model.User
import com.medit.health.data.model.UserRole
import com.medit.health.ui.screens.*
import com.medit.health.ui.theme.BackgroundWarm
import com.medit.health.ui.theme.MedItTheme

sealed class Screen {
    object Splash : Screen()
    object WelcomeAuth : Screen()
    object PatientDashboard : Screen()
    object DoctorWorkspace : Screen()
    object ProviderPortal : Screen()
    object AddChoice : Screen()
    data class ManualEntry(val preFill: MedicalRecord? = null, val targetPatientId: String? = null) : Screen()
    object PrescriptionUpload : Screen()
    data class RecordDetail(val recordId: String) : Screen()
    object AuditTrail : Screen()
    object CardiacRisk : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MedItApp
        val repository = app.repository

        setContent {
            MedItTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = BackgroundWarm
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
                    val currentUser by repository.currentUser.collectAsState()

                    // BackHandler to handle custom screen stack
                    BackHandler(enabled = currentScreen !is Screen.Splash && currentScreen !is Screen.WelcomeAuth && currentScreen !is Screen.PatientDashboard && currentScreen !is Screen.DoctorWorkspace && currentScreen !is Screen.ProviderPortal) {
                        when (currentScreen) {
                            is Screen.AddChoice -> {
                                currentScreen = if (currentUser?.role == UserRole.DOCTOR) Screen.DoctorWorkspace else Screen.PatientDashboard
                            }
                            is Screen.ManualEntry -> {
                                currentScreen = Screen.AddChoice
                            }
                            is Screen.PrescriptionUpload -> {
                                currentScreen = Screen.AddChoice
                            }
                            is Screen.RecordDetail -> {
                                currentScreen = if (currentUser?.role == UserRole.DOCTOR) Screen.DoctorWorkspace else Screen.PatientDashboard
                            }
                            is Screen.AuditTrail -> {
                                currentScreen = if (currentUser?.role == UserRole.DOCTOR) Screen.DoctorWorkspace else Screen.PatientDashboard
                            }
                            is Screen.CardiacRisk -> {
                                currentScreen = Screen.PatientDashboard
                            }
                            else -> {}
                        }
                    }

                    when (val screen = currentScreen) {
                        is Screen.Splash -> {
                            SplashScreen(
                                onSplashFinished = {
                                    val user = currentUser
                                    currentScreen = if (user != null) {
                                        when (user.role) {
                                            UserRole.DOCTOR -> Screen.DoctorWorkspace
                                            UserRole.PROVIDER -> Screen.ProviderPortal
                                            else -> Screen.PatientDashboard
                                        }
                                    } else {
                                        Screen.WelcomeAuth
                                    }
                                }
                            )
                        }

                        is Screen.WelcomeAuth -> {
                            WelcomeAuthScreen(
                                repository = repository,
                                onAuthSuccess = { user ->
                                    currentScreen = when (user.role) {
                                        UserRole.DOCTOR -> Screen.DoctorWorkspace
                                        UserRole.PROVIDER -> Screen.ProviderPortal
                                        else -> Screen.PatientDashboard
                                    }
                                }
                            )
                        }

                        is Screen.PatientDashboard -> {
                            val user = currentUser
                            if (user == null) {
                                currentScreen = Screen.WelcomeAuth
                            } else {
                                PatientDashboardScreen(
                                    patientUser = user,
                                    repository = repository,
                                    onAddRecordClick = { currentScreen = Screen.AddChoice },
                                    onRecordClick = { rec -> currentScreen = Screen.RecordDetail(rec.id) },
                                    onAuditTrailClick = { currentScreen = Screen.AuditTrail },
                                    onCardiacRiskClick = { currentScreen = Screen.CardiacRisk },
                                    onSignOutClick = {
                                        repository.logout()
                                        currentScreen = Screen.WelcomeAuth
                                    }
                                )
                            }
                        }

                        is Screen.DoctorWorkspace -> {
                            val user = currentUser
                            if (user == null || user.role != UserRole.DOCTOR) {
                                currentScreen = Screen.WelcomeAuth
                            } else {
                                DoctorWorkspaceScreen(
                                    doctorUser = user,
                                    repository = repository,
                                    onRecordClick = { rec -> currentScreen = Screen.RecordDetail(rec.id) },
                                    onAddClinicalRecordClick = { patId ->
                                        currentScreen = Screen.ManualEntry(targetPatientId = patId)
                                    },
                                    onAuditTrailClick = { currentScreen = Screen.AuditTrail },
                                    onSignOutClick = {
                                        repository.logout()
                                        currentScreen = Screen.WelcomeAuth
                                    }
                                )
                            }
                        }

                        is Screen.ProviderPortal -> {
                            val user = currentUser
                            if (user == null || user.role != UserRole.PROVIDER) {
                                currentScreen = Screen.WelcomeAuth
                            } else {
                                ProviderPortalScreen(
                                    providerUser = user,
                                    repository = repository,
                                    onAddRecordClick = { patId ->
                                        currentScreen = Screen.ManualEntry(targetPatientId = patId)
                                    },
                                    onRecordClick = { rec -> currentScreen = Screen.RecordDetail(rec.id) },
                                    onAuditTrailClick = { currentScreen = Screen.AuditTrail },
                                    onSignOutClick = {
                                        repository.logout()
                                        currentScreen = Screen.WelcomeAuth
                                    }
                                )
                            }
                        }

                        is Screen.AddChoice -> {
                            AddRecordChoiceScreen(
                                onSelectManual = { currentScreen = Screen.ManualEntry() },
                                onSelectPrescriptionUpload = { currentScreen = Screen.PrescriptionUpload },
                                onBackClick = {
                                    currentScreen = if (currentUser?.role == UserRole.DOCTOR) Screen.DoctorWorkspace else Screen.PatientDashboard
                                }
                            )
                        }

                        is Screen.ManualEntry -> {
                            val user = currentUser
                            val targetId = screen.targetPatientId ?: user?.associatedId ?: "PAT-101"
                            if (user != null) {
                                ManualRecordEntryScreen(
                                    patientId = targetId,
                                    currentUser = user,
                                    repository = repository,
                                    initialPreFill = screen.preFill,
                                    onRecordSaved = {
                                        currentScreen = if (user.role == UserRole.DOCTOR) Screen.DoctorWorkspace else Screen.PatientDashboard
                                    },
                                    onBackClick = { currentScreen = Screen.AddChoice }
                                )
                            } else {
                                currentScreen = Screen.WelcomeAuth
                            }
                        }

                        is Screen.PrescriptionUpload -> {
                            val user = currentUser
                            if (user != null) {
                                PrescriptionUploadScreen(
                                    patientId = user.associatedId,
                                    currentUser = user,
                                    repository = repository,
                                    onRecordSaved = { currentScreen = Screen.PatientDashboard },
                                    onFallbackToManual = { preFill ->
                                        currentScreen = Screen.ManualEntry(preFill = preFill)
                                    },
                                    onBackClick = { currentScreen = Screen.AddChoice }
                                )
                            } else {
                                currentScreen = Screen.WelcomeAuth
                            }
                        }

                        is Screen.RecordDetail -> {
                            val user = currentUser
                            if (user != null) {
                                RecordDetailScreen(
                                    recordId = screen.recordId,
                                    currentUser = user,
                                    repository = repository,
                                    onEditClick = { rec ->
                                        currentScreen = Screen.ManualEntry(preFill = rec)
                                    },
                                    onDeleteSuccess = {
                                        currentScreen = if (user.role == UserRole.DOCTOR) Screen.DoctorWorkspace else Screen.PatientDashboard
                                    },
                                    onBackClick = {
                                        currentScreen = if (user.role == UserRole.DOCTOR) Screen.DoctorWorkspace else Screen.PatientDashboard
                                    }
                                )
                            } else {
                                currentScreen = Screen.WelcomeAuth
                            }
                        }

                        is Screen.AuditTrail -> {
                            val user = currentUser
                            if (user != null) {
                                AuditTrailScreen(
                                    currentUser = user,
                                    repository = repository,
                                    onBackClick = {
                                        currentScreen = if (user.role == UserRole.DOCTOR) Screen.DoctorWorkspace else Screen.PatientDashboard
                                    }
                                )
                            } else {
                                currentScreen = Screen.WelcomeAuth
                            }
                        }

                        is Screen.CardiacRisk -> {
                            val user = currentUser
                            if (user != null) {
                                CardiacRiskScreen(
                                    currentUser = user,
                                    onBackClick = { currentScreen = Screen.PatientDashboard }
                                )
                            } else {
                                currentScreen = Screen.WelcomeAuth
                            }
                        }
                    }
                }
            }
        }
    }
}
