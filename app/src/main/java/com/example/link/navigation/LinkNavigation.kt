package com.example.link.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.link.ui.screens.caregiver.DashboardScreen
import com.example.link.ui.screens.caregiver.PauseAssistantScreen
import com.example.link.ui.screens.caregiver.SettingsScreen
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
    const val SeniorHome = "senior/home"
    const val Listening = "senior/listening"
    const val Confirmation = "senior/confirmation"
}

@Composable
fun LinkApp(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.AccountType) {
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
                onSeniorMode = { navController.navigateSingle(Routes.SeniorHome) }
            )
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
                onCaregiverMode = { navController.returnTo(Routes.Dashboard) }
            )
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
