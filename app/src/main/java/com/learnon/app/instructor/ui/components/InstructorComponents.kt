package com.learnon.app.instructor.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.learnon.app.instructor.domain.model.InstructorMetric
import com.learnon.app.instructor.domain.model.InstructorRequest

@Composable
fun MetricCard(metric: InstructorMetric, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(metric.label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(metric.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(metric.delta, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun RequestCard(
    request: InstructorRequest,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Text(request.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("%.0f".format(request.priorityScore), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text(request.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(request.category) })
                AssistChip(onClick = {}, label = { Text(request.urgency) })
                AssistChip(onClick = {}, label = { Text(request.formatPreference) })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(request.deadline ?: "Prazo dinamico", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text("${request.interestedStudents} interessados", style = MaterialTheme.typography.bodySmall)
            }
            actions?.invoke()
        }
    }
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                text = action,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Composable
fun LoadingSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
                                MaterialTheme.colorScheme.surface.copy(alpha = alpha),
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun CenterLoading() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

fun statusColor(status: String): Color = when (status) {
    "fast_track" -> Color(0xFFF59E0B)
    "em_andamento", "accepted" -> Color(0xFF2563EB)
    "concluido", "published" -> Color(0xFF16A34A)
    "cancelado", "declined" -> Color(0xFFDC2626)
    else -> Color(0xFF64748B)
}
