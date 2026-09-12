package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun NocStatusPill(
    status: WsConnectionStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, dotColor) = when (status) {
        WsConnectionStatus.LIVE -> Triple(NocGreenBg, NocGreen, NocGreen)
        WsConnectionStatus.RECONNECTING -> Triple(NocAmberBg, NocAmber, NocAmber)
        WsConnectionStatus.OFFLINE -> Triple(NocGrayBg, NocTextSecondary, NocGray)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by if (status == WsConnectionStatus.LIVE) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        rememberTransitionState(1f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor.copy(alpha = 0.7f))
            .border(1.dp, dotColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("ws_status_pill")
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = alpha))
        )
        Text(
            text = status.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun rememberTransitionState(value: Float): androidx.compose.runtime.State<Float> {
    return androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableFloatStateOf(value)
    }
}

@Composable
fun ConnectionBadge(
    status: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        ConnectionStatus.ONLINE -> Triple(NocGreenBg, NocGreen, "ONLINE")
        ConnectionStatus.DEGRADED -> Triple(NocAmberBg, NocAmber, "DEGRADED")
        ConnectionStatus.OFFLINE -> Triple(NocRedBg, NocRed, "OFFLINE")
    }

    Surface(
        color = bgColor.copy(alpha = 0.7f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun PowerStateBadge(
    state: VmPowerState,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (state) {
        VmPowerState.RUNNING -> Triple(NocGreenBg, NocGreen, "RUNNING")
        VmPowerState.STOPPED -> Triple(NocRedBg, NocRed, "STOPPED")
        VmPowerState.SUSPENDED -> Triple(NocAmberBg, NocAmber, "SUSPENDED")
        VmPowerState.UNKNOWN -> Triple(NocGrayBg, NocTextSecondary, "UNKNOWN")
    }

    Surface(
        color = bgColor.copy(alpha = 0.7f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SeverityBadge(
    severity: AlertSeverity,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (severity) {
        AlertSeverity.CRITICAL -> Pair(NocRedBg, NocRed)
        AlertSeverity.WARNING -> Pair(NocAmberBg, NocAmber)
        AlertSeverity.INFO -> Pair(NocSurfaceHover, NocBlue)
    }

    Surface(
        color = bgColor.copy(alpha = 0.8f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Text(
            text = severity.displayName.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MetricGaugeCard(
    title: String,
    metric: MetricValue?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NocSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NocSurfaceHighlight),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    color = NocTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                if (metric?.trend != null) {
                    val trendText = if (metric.trend >= 0) "+${String.format("%.1f", metric.trend)}%" else "${String.format("%.1f", metric.trend)}%"
                    Text(
                        text = trendText,
                        color = if (metric.trend > 0) NocAmber else NocGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Value representation: preserve 0 as 0 and null as unavailable (—)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (metric?.value != null) {
                    Text(
                        text = String.format("%.1f", metric.value),
                        color = NocTextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = metric.unit ?: "%",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                } else {
                    Text(
                        text = "—",
                        color = NocTextDisabled,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Unavailable",
                        color = NocTextDisabled,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Linear Progress Bar
            val progress = metric?.value?.let { (it / 100.0).coerceIn(0.0, 1.0).toFloat() } ?: 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    metric?.value == null -> NocSurfaceHover
                    metric.value >= 85 -> NocRed
                    metric.value >= 70 -> NocAmber
                    else -> accentColor
                },
                trackColor = NocSurfaceHover
            )
        }
    }
}

@Composable
fun HistoricalTelemetryCanvas(
    points: List<TelemetryHistoryPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(NocSurfaceVariant, RoundedCornerShape(12.dp))
                .border(1.dp, NocSurfaceHighlight, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No historical telemetry points available",
                color = NocTextSecondary,
                fontSize = 12.sp
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .background(NocSurfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, NocSurfaceHighlight, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORICAL TELEMETRY",
                    color = NocTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NocCyan))
                        Text("CPU", color = NocTextSecondary, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NocBlue))
                        Text("Memory", color = NocTextSecondary, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NocIndigo))
                        Text("Storage", color = NocTextSecondary, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                if (width <= 0 || height <= 0 || points.size < 2) return@Canvas

                val stepX = width / (points.size - 1)

                // Draw subtle grid lines
                for (i in 1..3) {
                    val y = height * (i / 4f)
                    drawLine(
                        color = Color(0xFF243048).copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                fun drawMetricLine(values: List<Double?>, lineColor: Color) {
                    val path = Path()
                    var started = false

                    for (i in values.indices) {
                        val v = values[i]
                        if (v != null) {
                            val x = i * stepX
                            val clamped = v.coerceIn(0.0, 100.0).toFloat()
                            val y = height - (clamped / 100f * height)
                            if (!started) {
                                path.moveTo(x, y)
                                started = true
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                    }

                    if (started) {
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 2.5f)
                        )
                    }
                }

                drawMetricLine(points.map { it.cpu }, NocCyan)
                drawMetricLine(points.map { it.memory }, NocBlue)
                drawMetricLine(points.map { it.storage }, NocIndigo)
            }
        }
    }
}
