package com.medit.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medit.health.ui.theme.*

data class VitalPoint(
    val label: String, // e.g. "18 Mar", "24 Mar"
    val systolic: Float,
    val diastolic: Float,
    val pulse: Float
)

@Composable
fun VitalsTrendChart(
    dataPoints: List<VitalPoint>,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfacePure),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cardiovascular Vitals Trend",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Blood Pressure (mmHg) & Heart Rate (bpm)",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }

                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MedItDarkGreen, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BP", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextSecondary))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFE11D48), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pulse", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextSecondary))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Line Chart
            val minVal = 50f
            val maxVal = 160f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val w = size.width
                val h = size.height

                // Draw Reference Guidelines (120 mmHg standard systolic line)
                val y120 = h - ((120f - minVal) / (maxVal - minVal) * h)
                val y80 = h - ((80f - minVal) / (maxVal - minVal) * h)

                // Reference line for normal 120/80
                drawLine(
                    color = SurfaceBorder,
                    start = Offset(0f, y120),
                    end = Offset(w, y120),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
                drawLine(
                    color = SurfaceBorder,
                    start = Offset(0f, y80),
                    end = Offset(w, y80),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                if (dataPoints.size >= 2) {
                    val stepX = w / (dataPoints.size - 1)

                    // Systolic Path
                    val sysPath = Path()
                    val diaPath = Path()
                    val pulsePath = Path()

                    dataPoints.forEachIndexed { i, p ->
                        val x = i * stepX
                        val ySys = h - ((p.systolic.coerceIn(minVal, maxVal) - minVal) / (maxVal - minVal) * h)
                        val yDia = h - ((p.diastolic.coerceIn(minVal, maxVal) - minVal) / (maxVal - minVal) * h)
                        val yPulse = h - ((p.pulse.coerceIn(minVal, maxVal) - minVal) / (maxVal - minVal) * h)

                        if (i == 0) {
                            sysPath.moveTo(x, ySys)
                            diaPath.moveTo(x, yDia)
                            pulsePath.moveTo(x, yPulse)
                        } else {
                            sysPath.lineTo(x, ySys)
                            diaPath.lineTo(x, yDia)
                            pulsePath.lineTo(x, yPulse)
                        }

                        // Draw points
                        drawCircle(color = MedItDarkGreen, radius = 4.dp.toPx(), center = Offset(x, ySys))
                        drawCircle(color = MedItPrimary600, radius = 3.dp.toPx(), center = Offset(x, yDia))
                        drawCircle(color = Color(0xFFE11D48), radius = 3.dp.toPx(), center = Offset(x, yPulse))
                    }

                    drawPath(path = sysPath, color = MedItDarkGreen, style = Stroke(width = 2.5.dp.toPx()))
                    drawPath(path = diaPath, color = MedItPrimary600, style = Stroke(width = 2.dp.toPx()))
                    drawPath(path = pulsePath, color = Color(0xFFE11D48), style = Stroke(width = 1.5.dp.toPx()))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // X-Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dataPoints.forEach { p ->
                    Text(
                        text = p.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    )
                }
            }
        }
    }
}
