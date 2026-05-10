package com.learnon.app.instructor.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.learnon.app.instructor.ui.screens.AnalyticsScreen
import com.learnon.app.instructor.ui.screens.CoursesScreen
import com.learnon.app.instructor.ui.screens.CreateCourseScreen
import com.learnon.app.instructor.ui.screens.FinanceScreen
import com.learnon.app.instructor.ui.screens.HomeScreen
import com.learnon.app.instructor.ui.screens.LiveScheduleScreen
import com.learnon.app.instructor.ui.screens.NotificationsScreen
import com.learnon.app.instructor.ui.screens.ProfileScreen
import com.learnon.app.instructor.ui.screens.QaScreen
import com.learnon.app.instructor.ui.screens.RequestDetailScreen
import com.learnon.app.instructor.ui.screens.RequestsScreen
import com.learnon.app.instructor.ui.screens.ReviewsScreen
import com.learnon.app.instructor.ui.screens.UploadScreen
import com.learnon.app.instructor.registration.TeacherRegistrationRoute
import com.learnon.app.instructor.viewmodel.InstructorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorRoot(viewModel: InstructorViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it) }
    }

    Crossfade(targetState = state.isAuthenticated, label = "auth") { authenticated ->
        if (!authenticated) {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = InstructorRoute.Login.route) {
                composable(InstructorRoute.Login.route) {
                    InstructorLoginScreen(
                        isLoading = state.isLoading,
                        snackbarHostState = snackbarHostState,
                        onLogin = viewModel::login,
                        onApply = { navController.navigate(InstructorRoute.TeacherRegistration.route) },
                    )
                }
                composable(InstructorRoute.TeacherRegistration.route) {
                    TeacherRegistrationRoute(
                        onBack = { navController.popBackStack() },
                        onSuccessDone = {
                            navController.navigate(InstructorRoute.Login.route) {
                                popUpTo(InstructorRoute.Login.route) { inclusive = true }
                            }
                        },
                    )
                }
            }
        } else {
            val navController = rememberNavController()
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("LearnOn", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Painel do instrutor", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Divider()
                        drawerRoutes.forEach { route ->
                            NavigationDrawerItem(
                                label = { Text(route.label) },
                                selected = currentRoute == route.route,
                                icon = { Icon(route.icon, contentDescription = null) },
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(route.route) {
                                        launchSingleTop = true
                                        popUpTo(InstructorRoute.Home.route)
                                    }
                                },
                            )
                        }
                    }
                },
            ) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = { Text(drawerRoutes.firstOrNull { it.route == currentRoute }?.label ?: "Home") },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                                }
                            },
                            actions = {
                                IconButton(onClick = viewModel::refreshAll) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar")
                                }
                            },
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            bottomRoutes.forEach { route ->
                                NavigationBarItem(
                                    selected = currentRoute == route.route,
                                    onClick = {
                                        navController.navigate(route.route) {
                                            launchSingleTop = true
                                            popUpTo(InstructorRoute.Home.route)
                                        }
                                    },
                                    icon = { Icon(route.icon, contentDescription = route.label) },
                                    label = { Text(route.label) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = InstructorRoute.Home.route,
                        modifier = Modifier.padding(padding),
                    ) {
                        composable(InstructorRoute.Home.route) {
                            HomeScreen(state, viewModel::refreshAll) { navController.navigate(it.route) }
                        }
                        composable(InstructorRoute.Requests.route) {
                            RequestsScreen(state, viewModel::acceptRequest, viewModel::rejectRequest) {
                                navController.navigate(InstructorRoute.RequestDetail.route)
                            }
                        }
                        composable(InstructorRoute.RequestDetail.route) { RequestDetailScreen(state.pendingRequests.firstOrNull() ?: state.queueRequests.firstOrNull()) }
                        composable(InstructorRoute.CreateCourse.route) { CreateCourseScreen(state, viewModel::createCourse) }
                        composable(InstructorRoute.Upload.route) { UploadScreen() }
                        composable(InstructorRoute.LiveSchedule.route) { LiveScheduleScreen(state, viewModel::createSchedule) }
                        composable(InstructorRoute.Courses.route) { CoursesScreen(state) }
                        composable(InstructorRoute.Qa.route) { QaScreen(state, viewModel::answerQuestion) }
                        composable(InstructorRoute.Reviews.route) { ReviewsScreen(state) }
                        composable(InstructorRoute.Finance.route) { FinanceScreen(state) }
                        composable(InstructorRoute.Profile.route) { ProfileScreen(state) }
                        composable(InstructorRoute.Notifications.route) { NotificationsScreen(state) }
                        composable(InstructorRoute.Analytics.route) { AnalyticsScreen(state) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructorLoginScreen(
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    onLogin: (String, String) -> Unit,
    onApply: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text("LearnOn", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text("Microcursos sob demanda para duvidas reais.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail do instrutor") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
            item {
                Button(
                    onClick = { onLogin(email.trim(), password) },
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isLoading) "Entrando..." else "Entrar no painel do instrutor")
                }
            }
            item {
                OutlinedButton(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                    Text("Candidatar-se como instrutor")
                }
            }
        }
    }
}
