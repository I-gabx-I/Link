package com.example.link.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.link.GuardianApplication
import com.example.link.ui.screens.assistant.AssistantScreen
import com.example.link.ui.screens.assistant.AssistantViewModel
import com.example.link.ui.screens.caregiver.DashboardScreen
import com.example.link.ui.screens.caregiver.PauseAssistantScreen
import com.example.link.ui.screens.caregiver.SettingsScreen
import com.example.link.ui.screens.connection.ConnectionScreen
import com.example.link.ui.screens.connection.ConnectionViewModel
import com.example.link.ui.screens.login.LoginScreen
import com.example.link.ui.screens.login.LoginViewModel
import com.example.link.ui.screens.senior.ConfirmationScreen
import com.example.link.ui.screens.senior.ListeningScreen
import com.example.link.ui.screens.senior.SeniorHomeScreen
import com.example.link.ui.screens.setup.AccountTypeScreen
import com.example.link.ui.screens.setup.AppsScreen
import com.example.link.ui.screens.setup.AppsSettingsScreen
import com.example.link.ui.screens.setup.CompleteScreen
import com.example.link.ui.screens.setup.ContactsScreen
import com.example.link.ui.screens.setup.ContactsSettingsScreen
import com.example.link.ui.screens.setup.PermissionsScreen
import com.example.link.ui.screens.setup.PermissionsSettingsScreen
import com.example.link.ui.screens.setup.SeniorDataScreen

private object Routes {
    const val Login = "auth/login"
    const val AccountType = "setup/account-type"
    const val SeniorData = "setup/senior-data"
    const val Contacts = "setup/contacts"
    const val Apps = "setup/apps"
    const val Permissions = "setup/permissions"
    const val Complete = "setup/complete"
    const val Dashboard = "caregiver/dashboard"
    const val Settings = "caregiver/settings"
    const val Pause = "caregiver/pause"
    const val SettingsContacts = "caregiver/settings/contacts"
    const val SettingsApps = "caregiver/settings/apps"
    const val SettingsPermissions = "caregiver/settings/permissions"
    const val Connection = "caregiver/settings/connection"
    const val Assistant = "senior/assistant"
    const val SeniorHome = "senior/home"
    const val Listening = "senior/listening"
    const val Confirmation = "senior/confirmation"
}

@Composable
fun LinkApp(navController: NavHostController = rememberNavController()) {
    val app = LocalContext.current.applicationContext as GuardianApplication
    val startDestination = remember {
        if (app.authRepository.isSignedIn) Routes.AccountType else Routes.Login
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.Login) {
            val loginViewModel: LoginViewModel = viewModel { LoginViewModel(app.authRepository) }
            LoginScreen(
                viewModel = loginViewModel,
                onLoggedIn = {
                    navController.navigate(Routes.AccountType) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.AccountType) {
            AccountTypeScreen(onContinue = { navController.navigate(Routes.SeniorData) })
        }
        composable(Routes.SeniorData) {
            SeniorDataScreen(
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.Contacts) }
            )
        }
        composable(Routes.Contacts) {
            ContactsScreen(
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.Apps) }
            )
        }
        composable(Routes.Apps) {
            AppsScreen(
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.Permissions) }
            )
        }
        composable(Routes.Permissions) {
            PermissionsScreen(
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.Complete) }
            )
        }
        composable(Routes.Complete) {
            CompleteScreen(
                onBack = { navController.popBackStack() },
                onFinish = {
                    navController.navigate(Routes.Dashboard) {
                        popUpTo(Routes.AccountType) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Dashboard) {
            DashboardScreen(
                onSettings = { navController.navigateSingle(Routes.Settings) },
                onPause = { navController.navigateSingle(Routes.Pause) },
                onSeniorMode = { navController.navigateSingle(Routes.SeniorHome) }
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                onDashboard = { navController.returnTo(Routes.Dashboard) },
                onPause = { navController.navigateSingle(Routes.Pause) },
                onContacts = { navController.navigate(Routes.SettingsContacts) },
                onApps = { navController.navigate(Routes.SettingsApps) },
                onPermissions = { navController.navigate(Routes.SettingsPermissions) },
                onSeniorMode = { navController.navigateSingle(Routes.SeniorHome) },
                onConnection = { navController.navigate(Routes.Connection) },
                onLogout = {
                    app.authRepository.signOut()
                    navController.navigate(Routes.Login) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Connection) {
            val connectionViewModel: ConnectionViewModel = viewModel {
                ConnectionViewModel(app.connectionStore, app.repository)
            }
            ConnectionScreen(viewModel = connectionViewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.Pause) {
            PauseAssistantScreen(
                onDashboard = { navController.returnTo(Routes.Dashboard) },
                onSettings = { navController.navigateSingle(Routes.Settings) }
            )
        }
        composable(Routes.SettingsContacts) {
            ContactsSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SettingsApps) {
            AppsSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SettingsPermissions) {
            PermissionsSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SeniorHome) {
            SeniorHomeScreen(
                onListen = { navController.navigate(Routes.Listening) },
                onWrite = { navController.navigate(Routes.Assistant) },
                onCaregiverMode = { navController.returnTo(Routes.Dashboard) }
            )
        }
        composable(Routes.Assistant) {
            val assistantViewModel: AssistantViewModel = viewModel {
                AssistantViewModel(app.repository)
            }
            AssistantScreen(viewModel = assistantViewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.Listening) {
            ListeningScreen(
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.Confirmation) }
            )
        }
        composable(Routes.Confirmation) {
            ConfirmationScreen(
                onBack = { navController.popBackStack() },
                onCancel = { navController.returnTo(Routes.SeniorHome) },
                onDone = { navController.returnTo(Routes.SeniorHome) }
            )
        }
    }
}

private fun NavHostController.navigateSingle(route: String) {
    navigate(route) { launchSingleTop = true }
}

private fun NavHostController.returnTo(route: String) {
    if (!popBackStack(route, inclusive = false)) {
        navigate(route) { launchSingleTop = true }
    }
}