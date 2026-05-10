package com.learnon.app.instructor.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class InstructorRoute(val route: String, val label: String, val icon: ImageVector) {
    data object Home : InstructorRoute("home", "Home", Icons.Outlined.Home)
    data object Requests : InstructorRoute("requests", "Pedidos", Icons.Outlined.TaskAlt)
    data object RequestDetail : InstructorRoute("requestDetail", "Detalhe", Icons.Outlined.TaskAlt)
    data object CreateCourse : InstructorRoute("createCourse", "Criar curso", Icons.Outlined.School)
    data object Upload : InstructorRoute("upload", "Upload", Icons.Outlined.CloudUpload)
    data object LiveSchedule : InstructorRoute("liveSchedule", "Ao vivo", Icons.Outlined.CalendarMonth)
    data object Courses : InstructorRoute("courses", "Cursos", Icons.Outlined.LibraryBooks)
    data object Qa : InstructorRoute("qa", "Q&A", Icons.Outlined.ChatBubbleOutline)
    data object Reviews : InstructorRoute("reviews", "Avaliacoes", Icons.Outlined.RateReview)
    data object Finance : InstructorRoute("finance", "Financeiro", Icons.Outlined.AttachMoney)
    data object Profile : InstructorRoute("profile", "Perfil", Icons.Outlined.Person)
    data object Notifications : InstructorRoute("notifications", "Alertas", Icons.Outlined.Notifications)
    data object Analytics : InstructorRoute("analytics", "Analytics", Icons.Outlined.Analytics)
}

val bottomRoutes = listOf(
    InstructorRoute.Home,
    InstructorRoute.Requests,
    InstructorRoute.Courses,
    InstructorRoute.LiveSchedule,
    InstructorRoute.Profile,
)

val drawerRoutes = listOf(
    InstructorRoute.Home,
    InstructorRoute.Requests,
    InstructorRoute.RequestDetail,
    InstructorRoute.CreateCourse,
    InstructorRoute.Upload,
    InstructorRoute.LiveSchedule,
    InstructorRoute.Courses,
    InstructorRoute.Qa,
    InstructorRoute.Reviews,
    InstructorRoute.Finance,
    InstructorRoute.Profile,
    InstructorRoute.Notifications,
    InstructorRoute.Analytics,
)
