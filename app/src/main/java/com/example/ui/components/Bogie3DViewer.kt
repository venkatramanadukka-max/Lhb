package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

// Simple 3D point structure
data class Point3D(val x: Float, val y: Float, val z: Float) {
    fun rotateY(angleRad: Float): Point3D {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Point3D(
            x = x * cosA - z * sinA,
            y = y,
            z = x * sinA + z * cosA
        )
    }

    fun rotateX(angleRad: Float): Point3D {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Point3D(
            x = x,
            y = y * cosA - z * sinA,
            z = y * sinA + z * cosA
        )
    }

    fun project(width: Float, height: Float, scale: Float): Offset {
        // Real perspective 3D projection
        val cameraZ = 350f
        val divisor = cameraZ + z
        val factor = cameraZ / if (divisor < 60f) 60f else divisor
        return Offset(
            x = width / 2f + x * scale * factor,
            y = height / 2f - y * scale * factor
        )
    }
}

// Struct to represent a 3D wireframe line or component
data class Segment3D(
    val p1: Point3D,
    val p2: Point3D,
    val color: Color,
    val strokeWidth: Float = 4f,
    val isDashed: Boolean = false
)

enum class BogiePart(val displayName: String, val technicalCode: String) {
    WHEELSET_AXLE("Wheelset & Axle", "WSH-01"),
    BRAKE_DISC("Axle Mounted Brake Disc", "BD-04"),
    PRIMARY_SPRING("Primary Suspension (Nested Coil)", "SUS-1P"),
    AIR_SPRING("Secondary Air Spring (Bellows)", "SUS-2S"),
    YAW_DAMPER("Yaw Damper (Lateral)", "DMP-YAW"),
    AXLE_BOX_CTRB("CTRB Axle Box", "BRG-CTRB"),
    BOGIE_FRAME("Y-Bogie Steel Frame", "FRM-FIAT"),
    ANTI_ROLL_BAR("Anti-Roll Bar Assembly", "ARB-09"),
    CONTROL_ARM("Axle Control Arm", "ARM-05"),
    ANCHOR_LINK("Longitudinal Anchor Link", "LNK-07")
}

@Composable
fun Bogie3DViewer(
    selectedPart: BogiePart,
    onPartSelected: (BogiePart) -> Unit,
    modifier: Modifier = Modifier
) {
    var yawAngle by remember { mutableStateOf(0.6f) } // Initial rotation
    var pitchAngle by remember { mutableStateOf(-0.3f) }
    var zoomScale by remember { mutableStateOf(1.2f) }

    // Floating animation for a more dynamic look
    val infiniteTransition = rememberInfiniteTransition(label = "bogie_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_anim"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Interactive 3D Schematic",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkMetal
                    )
                    Text(
                        text = "Swipe to rotate • Drag to inspect components",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { zoomScale = (zoomScale - 0.1f).coerceAtLeast(0.6f) },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = LightSteel, contentColor = DarkMetal),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { zoomScale = (zoomScale + 0.1f).coerceAtBound(2.0f) }, // coerceAtMost
                        colors = IconButtonDefaults.iconButtonColors(containerColor = LightSteel, contentColor = DarkMetal),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = {
                            yawAngle = 0.6f
                            pitchAngle = -0.3f
                            zoomScale = 1.2f
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = LightSteel, contentColor = DarkMetal),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset View", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFF8FAFC), Color(0xFFE8F0FE))
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val baseScale = (minOf(w, h) / 350f) * zoomScale
                            
                            val halfWheelbase = 120f
                            val halfGauge = 80f
                            val frameY = 0f
                            val beamZ1 = 92f
                            val beamZ2 = -92f
                            
                            val partPoints = mapOf(
                                BogiePart.WHEELSET_AXLE to listOf(
                                    Point3D(halfWheelbase, frameY, 0f),
                                    Point3D(-halfWheelbase, frameY, 0f)
                                ),
                                BogiePart.BRAKE_DISC to listOf(
                                    Point3D(halfWheelbase, frameY, 30f),
                                    Point3D(halfWheelbase, frameY, -30f),
                                    Point3D(-halfWheelbase, frameY, 30f),
                                    Point3D(-halfWheelbase, frameY, -30f)
                                ),
                                BogiePart.PRIMARY_SPRING to listOf(
                                    Point3D(halfWheelbase, frameY + 24f, beamZ1),
                                    Point3D(halfWheelbase, frameY + 24f, beamZ2),
                                    Point3D(-halfWheelbase, frameY + 24f, beamZ1),
                                    Point3D(-halfWheelbase, frameY + 24f, beamZ2)
                                ),
                                BogiePart.AIR_SPRING to listOf(
                                    Point3D(0f, frameY + 20f, 96f),
                                    Point3D(0f, frameY + 20f, -96f)
                                ),
                                BogiePart.YAW_DAMPER to listOf(
                                    Point3D(35f, frameY + 23.5f, beamZ1 + 12.5f),
                                    Point3D(-35f, frameY + 23.5f, beamZ2 - 12.5f)
                                ),
                                BogiePart.AXLE_BOX_CTRB to listOf(
                                    Point3D(halfWheelbase, frameY, halfGauge + 15f),
                                    Point3D(halfWheelbase, frameY, -halfGauge - 15f),
                                    Point3D(-halfWheelbase, frameY, halfGauge + 15f),
                                    Point3D(-halfWheelbase, frameY, -halfGauge - 15f)
                                ),
                                BogiePart.BOGIE_FRAME to listOf(
                                    Point3D(0f, frameY + 10f, 0f),
                                    Point3D(50f, frameY + 8f, beamZ1),
                                    Point3D(-50f, frameY + 8f, beamZ1),
                                    Point3D(50f, frameY + 8f, beamZ2),
                                    Point3D(-50f, frameY + 8f, beamZ2)
                                ),
                                BogiePart.ANTI_ROLL_BAR to listOf(
                                    Point3D(-35f, frameY + 15f, 0f),
                                    Point3D(-35f, frameY + 15f, 45f),
                                    Point3D(-35f, frameY + 15f, -45f)
                                ),
                                BogiePart.CONTROL_ARM to listOf(
                                    Point3D(100f, frameY + 5f, beamZ1),
                                    Point3D(-100f, frameY + 5f, beamZ2),
                                    Point3D(100f, frameY + 5f, beamZ2),
                                    Point3D(-100f, frameY + 5f, beamZ1)
                                ),
                                BogiePart.ANCHOR_LINK to listOf(
                                    Point3D(0f, frameY + 18f, 50f),
                                    Point3D(0f, frameY + 18f, -50f)
                                )
                            )
                            
                            var closestPart: BogiePart? = null
                            var minDistance = Float.MAX_VALUE
                            
                            partPoints.forEach { (part, points) ->
                                points.forEach { pt ->
                                    val animatedP = Point3D(pt.x, pt.y + floatOffset * 0.3f, pt.z)
                                    val rotated = animatedP.rotateY(yawAngle).rotateX(pitchAngle)
                                    val projected = rotated.project(w, h, baseScale)
                                    val dx = projected.x - tapOffset.x
                                    val dy = projected.y - tapOffset.y
                                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                    if (dist < minDistance) {
                                        minDistance = dist
                                        closestPart = part
                                    }
                                }
                            }
                            
                            if (minDistance < 65f && closestPart != null) {
                                onPartSelected(closestPart!!)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            yawAngle += dragAmount.x * 0.007f
                            pitchAngle += dragAmount.y * 0.007f
                            // Constrain pitch to avoid flipping upside down
                            pitchAngle = pitchAngle.coerceIn(-1.2f, 1.2f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }
                val baseScale = (minOf(wPx, hPx) / 350f) * zoomScale

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 1. GENERATE BOGIE 3D GEOMETRY
                    // Dimensions based on FIAT LHB Bogie scaled
                    val halfWheelbase = 120f
                    val halfGauge = 80f
                    val wheelRadius = 40f
                    val frameY = 0f

                    // Rotate and project helper
                    fun proj(p: Point3D): Offset {
                        // Apply float animation on Y coordinate
                        val animatedP = Point3D(p.x, p.y + floatOffset * 0.3f, p.z)
                        val rotated = animatedP.rotateY(yawAngle).rotateX(pitchAngle)
                        return rotated.project(w, h, baseScale)
                    }

                    // Highlight color based on selected part
                    fun getPartColor(part: BogiePart, defaultColor: Color): Color {
                        return if (selectedPart == part) RailCrimson else defaultColor
                    }

                    // --- DRAW WHEELS & AXLES ---
                    val isWheelSelected = selectedPart == BogiePart.WHEELSET_AXLE
                    val wheelColor = getPartColor(BogiePart.WHEELSET_AXLE, Color.Gray)
                    val axleColor = getPartColor(BogiePart.WHEELSET_AXLE, LightSteel)

                    // Front Axle
                    val pAxleF1 = Point3D(halfWheelbase, frameY, halfGauge)
                    val pAxleF2 = Point3D(halfWheelbase, frameY, -halfGauge)
                    drawLine(axleColor, proj(pAxleF1), proj(pAxleF2), strokeWidth = if (isWheelSelected) 10f else 6f)

                    // Rear Axle
                    val pAxleR1 = Point3D(-halfWheelbase, frameY, halfGauge)
                    val pAxleR2 = Point3D(-halfWheelbase, frameY, -halfGauge)
                    drawLine(axleColor, proj(pAxleR1), proj(pAxleR2), strokeWidth = if (isWheelSelected) 10f else 6f)

                    // Draw 4 Wheels as 3D circles (polygons perpendicular to Z)
                    val wheelOffsets = listOf(
                        halfWheelbase to halfGauge,
                        halfWheelbase to -halfGauge,
                        -halfWheelbase to halfGauge,
                        -halfWheelbase to -halfGauge
                    )

                    wheelOffsets.forEach { (wx, wz) ->
                        // Generate circle coordinates in 3D (X-Y plane, constant Z)
                        val segments = 16
                        val points = mutableListOf<Point3D>()
                        for (i in 0..segments) {
                            val rad = (i * 2 * Math.PI / segments).toFloat()
                            points.add(
                                Point3D(
                                    x = wx + wheelRadius * sin(rad),
                                    y = frameY + wheelRadius * cos(rad),
                                    z = wz
                                )
                            )
                        }
                        // Draw outline path
                        val path = Path()
                        path.moveTo(proj(points[0]).x, proj(points[0]).y)
                        for (i in 1 until points.size) {
                            val offset = proj(points[i])
                            path.lineTo(offset.x, offset.y)
                        }
                        drawPath(
                            path = path,
                            color = wheelColor,
                            style = Stroke(width = if (isWheelSelected) 6f else 3f)
                        )
                    }

                    // --- DRAW AXLE-MOUNTED BRAKE DISCS (2 per axle, inner side) ---
                    val isDiscSelected = selectedPart == BogiePart.BRAKE_DISC
                    val discColor = getPartColor(BogiePart.BRAKE_DISC, GoldenAmber)
                    val discRadius = 22f
                    val discPositions = listOf(
                        halfWheelbase to 30f,
                        halfWheelbase to -30f,
                        -halfWheelbase to 30f,
                        -halfWheelbase to -30f
                    )

                    discPositions.forEach { (dx, dz) ->
                        val segments = 12
                        val points = mutableListOf<Point3D>()
                        for (i in 0..segments) {
                            val rad = (i * 2 * Math.PI / segments).toFloat()
                            points.add(
                                Point3D(
                                    x = dx + discRadius * sin(rad),
                                    y = frameY + discRadius * cos(rad),
                                    z = dz
                                )
                            )
                        }
                        val path = Path()
                        path.moveTo(proj(points[0]).x, proj(points[0]).y)
                        for (i in 1 until points.size) {
                            path.lineTo(proj(points[i]).x, proj(points[i]).y)
                        }
                        drawPath(
                            path = path,
                            color = discColor,
                            style = Stroke(width = if (isDiscSelected) 5f else 2.5f)
                        )
                    }

                    // --- DRAW BOGIE FRAME (H / Y-Steel structure) ---
                    val isFrameSelected = selectedPart == BogiePart.BOGIE_FRAME
                    val frameColor = getPartColor(BogiePart.BOGIE_FRAME, RailSteelBlue.copy(alpha = 0.8f))
                    val frameThickness = if (isFrameSelected) 12f else 7f

                    // Side Beams at Z = +/- 90 (Y-shaped frame contours)
                    val beamZ1 = 92f
                    val beamZ2 = -92f

                    // Left Side Frame
                    val pL1 = Point3D(-halfWheelbase - 10f, frameY + 5f, beamZ1)
                    val pL2 = Point3D(-30f, frameY + 12f, beamZ1)
                    val pL3 = Point3D(30f, frameY + 12f, beamZ1)
                    val pL4 = Point3D(halfWheelbase + 10f, frameY + 5f, beamZ1)
                    drawLine(frameColor, proj(pL1), proj(pL2), strokeWidth = frameThickness)
                    drawLine(frameColor, proj(pL2), proj(pL3), strokeWidth = frameThickness + 3f)
                    drawLine(frameColor, proj(pL3), proj(pL4), strokeWidth = frameThickness)

                    // Right Side Frame
                    val pR1 = Point3D(-halfWheelbase - 10f, frameY + 5f, beamZ2)
                    val pR2 = Point3D(-30f, frameY + 12f, beamZ2)
                    val pR3 = Point3D(30f, frameY + 12f, beamZ2)
                    val pR4 = Point3D(halfWheelbase + 10f, frameY + 5f, beamZ2)
                    drawLine(frameColor, proj(pR1), proj(pR2), strokeWidth = frameThickness)
                    drawLine(frameColor, proj(pR2), proj(pR3), strokeWidth = frameThickness + 3f)
                    drawLine(frameColor, proj(pR3), proj(pR4), strokeWidth = frameThickness)

                    // Center Cross-Beams / Transoms connecting side frames
                    val pTrans1L = Point3D(-25f, frameY + 10f, beamZ1)
                    val pTrans1R = Point3D(-25f, frameY + 10f, beamZ2)
                    val pTrans2L = Point3D(25f, frameY + 10f, beamZ1)
                    val pTrans2R = Point3D(25f, frameY + 10f, beamZ2)
                    drawLine(frameColor, proj(pTrans1L), proj(pTrans1R), strokeWidth = frameThickness + 1f)
                    drawLine(frameColor, proj(pTrans2L), proj(pTrans2R), strokeWidth = frameThickness + 1f)

                    // --- DRAW PRIMARY SPRINGS (Coil springs above Axle Boxes) ---
                    val isPrimarySelected = selectedPart == BogiePart.PRIMARY_SPRING
                    val primaryColor = getPartColor(BogiePart.PRIMARY_SPRING, AccentTeal)
                    val springW = if (isPrimarySelected) 8f else 4f

                    // Four axle box positions (X = +/- 120, Z = +/- 92)
                    val springPositions = listOf(
                        Point3D(halfWheelbase, frameY, beamZ1),
                        Point3D(halfWheelbase, frameY, beamZ2),
                        Point3D(-halfWheelbase, frameY, beamZ1),
                        Point3D(-halfWheelbase, frameY, beamZ2)
                    )

                    springPositions.forEach { sp ->
                        // Draw a helical spiral or a cylinder representing the nested spring
                        val numTurns = 5
                        val radius = 10f
                        val height = 24f
                        val steps = 30
                        var lastPt = Point3D(sp.x, sp.y, sp.z)
                        for (i in 1..steps) {
                            val fraction = i.toFloat() / steps
                            val angle = fraction * numTurns * 2 * Math.PI
                            val curPt = Point3D(
                                x = sp.x + (radius * sin(angle)).toFloat(),
                                y = sp.y + fraction * height,
                                z = sp.z + (radius * cos(angle)).toFloat()
                            )
                            drawLine(primaryColor, proj(lastPt), proj(curPt), strokeWidth = springW)
                            lastPt = curPt
                        }
                    }

                    // --- DRAW SECONDARY AIR SPRINGS (Air Bellows in Center) ---
                    val isAirSelected = selectedPart == BogiePart.AIR_SPRING
                    val airColor = getPartColor(BogiePart.AIR_SPRING, Color(0xFF64B5F6))
                    val airW = if (isAirSelected) 12f else 6f

                    // Two air springs, located at X = 0, Z = +/- 102 (on side frames)
                    val airSprings = listOf(
                        Point3D(0f, frameY + 12f, 96f),
                        Point3D(0f, frameY + 12f, -96f)
                    )

                    airSprings.forEach { asp ->
                        // Draw air bellow as multiple parallel concentric circles
                        val bellowRadius = 24f
                        val numRings = 4
                        val ringHeight = 10f
                        for (r in 0 until numRings) {
                            val ry = asp.y + r * ringHeight
                            val segments = 12
                            val points = mutableListOf<Point3D>()
                            for (i in 0..segments) {
                                val rad = (i * 2 * Math.PI / segments).toFloat()
                                points.add(
                                    Point3D(
                                        x = asp.x + bellowRadius * sin(rad),
                                        y = ry,
                                        z = asp.z + bellowRadius * cos(rad)
                                    )
                                )
                            }
                            // Draw ring
                            val path = Path()
                            path.moveTo(proj(points[0]).x, proj(points[0]).y)
                            for (i in 1 until points.size) {
                                path.lineTo(proj(points[i]).x, proj(points[i]).y)
                            }
                            drawPath(
                                path = path,
                                color = airColor,
                                style = Stroke(width = airW)
                            )
                        }
                        // Vertical outer borders
                        drawLine(airColor, proj(Point3D(asp.x, asp.y, asp.z + bellowRadius)), proj(Point3D(asp.x, asp.y + 30f, asp.z + bellowRadius)), strokeWidth = airW / 2)
                        drawLine(airColor, proj(Point3D(asp.x, asp.y, asp.z - bellowRadius)), proj(Point3D(asp.x, asp.y + 30f, asp.z - bellowRadius)), strokeWidth = airW / 2)
                    }

                    // --- DRAW CTRB AXLE BOXES (At axles ends) ---
                    val isAxleBoxSelected = selectedPart == BogiePart.AXLE_BOX_CTRB
                    val axleBoxColor = getPartColor(BogiePart.AXLE_BOX_CTRB, Color(0xFFFF7043))
                    val boxSize = 12f

                    val axleBoxPositions = listOf(
                        Point3D(halfWheelbase, frameY, halfGauge + 15f),
                        Point3D(halfWheelbase, frameY, -halfGauge - 15f),
                        -halfWheelbase to (halfGauge + 15f),
                        -halfWheelbase to (-halfGauge - 15f)
                    )

                    axleBoxPositions.forEach { pos ->
                        val px = if (pos is Point3D) pos.x else (pos as Pair<*, *>).first as Float
                        val pz = if (pos is Point3D) pos.z else (pos as Pair<*, *>).second as Float

                        // Draw simple 3D cube or circle for axle box
                        val pC1 = Point3D(px - boxSize, frameY - boxSize, pz)
                        val pC2 = Point3D(px + boxSize, frameY - boxSize, pz)
                        val pC3 = Point3D(px + boxSize, frameY + boxSize, pz)
                        val pC4 = Point3D(px - boxSize, frameY + boxSize, pz)

                        val path = Path()
                        path.moveTo(proj(pC1).x, proj(pC1).y)
                        path.lineTo(proj(pC2).x, proj(pC2).y)
                        path.lineTo(proj(pC3).x, proj(pC3).y)
                        path.lineTo(proj(pC4).x, proj(pC4).y)
                        path.close()

                        drawPath(
                            path = path,
                            color = axleBoxColor,
                            style = Stroke(width = if (isAxleBoxSelected) 6f else 3f)
                        )
                    }

                    // --- DRAW YAW DAMPERS (Diagonal lines connecting frame to chassis) ---
                    val isYawSelected = selectedPart == BogiePart.YAW_DAMPER
                    val yawColor = getPartColor(BogiePart.YAW_DAMPER, RailCrimson)
                    val yawThickness = if (isYawSelected) 8f else 4f

                    // Lateral yaw dampers connecting central beam to outer chassis (simulated points)
                    val pYaw1Start = Point3D(0f, frameY + 12f, beamZ1 + 10f)
                    val pYaw1End = Point3D(70f, frameY + 35f, beamZ1 + 15f)
                    val pYaw2Start = Point3D(0f, frameY + 12f, beamZ2 - 10f)
                    val pYaw2End = Point3D(-70f, frameY + 35f, beamZ2 - 15f)

                    drawLine(yawColor, proj(pYaw1Start), proj(pYaw1End), strokeWidth = yawThickness)
                    drawLine(yawColor, proj(pYaw2Start), proj(pYaw2End), strokeWidth = yawThickness)

                    // Draw nice little spherical pivot joints
                    drawCircle(yawColor, radius = if (isYawSelected) 12f else 6f, center = proj(pYaw1Start))
                    drawCircle(yawColor, radius = if (isYawSelected) 12f else 6f, center = proj(pYaw1End))
                    drawCircle(yawColor, radius = if (isYawSelected) 12f else 6f, center = proj(pYaw2Start))
                    drawCircle(yawColor, radius = if (isYawSelected) 12f else 6f, center = proj(pYaw2End))

                    // --- DRAW ANTI-ROLL BAR ASSEMBLY ---
                    val isArbSelected = selectedPart == BogiePart.ANTI_ROLL_BAR
                    val arbColor = getPartColor(BogiePart.ANTI_ROLL_BAR, Color(0xFF9575CD)) // Soft purple
                    val arbThickness = if (isArbSelected) 8f else 4.5f

                    // Transverse bar
                    val pArbLeft = Point3D(-35f, frameY + 15f, beamZ1 - 10f)
                    val pArbRight = Point3D(-35f, frameY + 15f, beamZ2 + 10f)
                    drawLine(arbColor, proj(pArbLeft), proj(pArbRight), strokeWidth = arbThickness)

                    // Torsion links (vertical rods)
                    val pArbLinkL1 = Point3D(-35f, frameY + 15f, beamZ1 - 8f)
                    val pArbLinkL2 = Point3D(-35f, frameY + 2f, beamZ1 - 8f)
                    val pArbLinkR1 = Point3D(-35f, frameY + 15f, beamZ2 + 8f)
                    val pArbLinkR2 = Point3D(-35f, frameY + 2f, beamZ2 + 8f)
                    drawLine(arbColor, proj(pArbLinkL1), proj(pArbLinkL2), strokeWidth = arbThickness - 1f)
                    drawLine(arbColor, proj(pArbLinkR1), proj(pArbLinkR2), strokeWidth = arbThickness - 1f)
                    drawCircle(arbColor, radius = if (isArbSelected) 8f else 4f, center = proj(pArbLinkL2))
                    drawCircle(arbColor, radius = if (isArbSelected) 8f else 4f, center = proj(pArbLinkR2))

                    // --- DRAW CONTROL ARMS ---
                    val isArmSelected = selectedPart == BogiePart.CONTROL_ARM
                    val armColor = getPartColor(BogiePart.CONTROL_ARM, Color(0xFF4DB6AC)) // Soft teal
                    val armThickness = if (isArmSelected) 9f else 5f

                    // Arms connecting axle boxes (X = +/-120, Y = 0) to frame pivots (X = +/-80, Y = 10)
                    val armPoints = listOf(
                        // Front axle boxes
                        Pair(Point3D(halfWheelbase, frameY, beamZ1), Point3D(halfWheelbase - 40f, frameY + 10f, beamZ1)),
                        Pair(Point3D(halfWheelbase, frameY, beamZ2), Point3D(halfWheelbase - 40f, frameY + 10f, beamZ2)),
                        // Rear axle boxes
                        Pair(Point3D(-halfWheelbase, frameY, beamZ1), Point3D(-halfWheelbase + 40f, frameY + 10f, beamZ1)),
                        Pair(Point3D(-halfWheelbase, frameY, beamZ2), Point3D(-halfWheelbase + 40f, frameY + 10f, beamZ2))
                    )

                    armPoints.forEach { (pAxle, pFrame) ->
                        // Draw main longitudinal arm body
                        drawLine(armColor, proj(pAxle), proj(pFrame), strokeWidth = armThickness)
                        
                        // Draw the secondary reinforcing lower web of control arm (making it a robust cast steel guide arm)
                        val pWeb = Point3D(pAxle.x, pAxle.y + 4f, pAxle.z)
                        drawLine(armColor, proj(pWeb), proj(pFrame), strokeWidth = armThickness - 1f)

                        // Draw horizontal circular primary suspension seat/cup integral to the control arm holding the coil spring
                        drawCircle(
                            color = armColor,
                            radius = if (isArmSelected) 14f else 9f,
                            center = proj(pAxle),
                            style = Stroke(width = if (isArmSelected) 3.5f else 2f)
                        )
                        drawCircle(
                            color = armColor.copy(alpha = 0.25f),
                            radius = if (isArmSelected) 11f else 7f,
                            center = proj(pAxle)
                        )

                        // Pivot joint at bogie frame
                        drawCircle(armColor, radius = if (isArmSelected) 9f else 4.5f, center = proj(pFrame))
                    }

                    // --- DRAW LONGITUDINAL ANCHOR LINKS ---
                    val isLinkSelected = selectedPart == BogiePart.ANCHOR_LINK
                    val linkColor = getPartColor(BogiePart.ANCHOR_LINK, Color(0xFFEC407A)) // Bright pinkish red
                    val linkThickness = if (isLinkSelected) 8f else 4.5f

                    // Longitudinal links (bolster to frame)
                    val pLinkL1 = Point3D(-40f, frameY + 18f, 50f)
                    val pLinkL2 = Point3D(40f, frameY + 18f, 50f)
                    val pLinkR1 = Point3D(-40f, frameY + 18f, -50f)
                    val pLinkR2 = Point3D(40f, frameY + 18f, -50f)

                    drawLine(linkColor, proj(pLinkL1), proj(pLinkL2), strokeWidth = linkThickness)
                    drawLine(linkColor, proj(pLinkR1), proj(pLinkR2), strokeWidth = linkThickness)
                    drawCircle(linkColor, radius = if (isLinkSelected) 9f else 4.5f, center = proj(pLinkL1))
                    drawCircle(linkColor, radius = if (isLinkSelected) 9f else 4.5f, center = proj(pLinkL2))
                    drawCircle(linkColor, radius = if (isLinkSelected) 9f else 4.5f, center = proj(pLinkR1))
                    drawCircle(linkColor, radius = if (isLinkSelected) 9f else 4.5f, center = proj(pLinkR2))

                    // --- DRAW INTERACTIVE HOTSPOTS ---
                    val hotspots = listOf(
                        BogiePart.WHEELSET_AXLE to Point3D(halfWheelbase, frameY, 0f),
                        BogiePart.WHEELSET_AXLE to Point3D(-halfWheelbase, frameY, 0f),
                        BogiePart.BRAKE_DISC to Point3D(halfWheelbase, frameY, 30f),
                        BogiePart.BRAKE_DISC to Point3D(-halfWheelbase, frameY, -30f),
                        BogiePart.PRIMARY_SPRING to Point3D(halfWheelbase, frameY + 24f, beamZ1),
                        BogiePart.PRIMARY_SPRING to Point3D(-halfWheelbase, frameY + 24f, beamZ2),
                        BogiePart.AIR_SPRING to Point3D(0f, frameY + 20f, 96f),
                        BogiePart.AIR_SPRING to Point3D(0f, frameY + 20f, -96f),
                        BogiePart.YAW_DAMPER to Point3D(35f, frameY + 23.5f, beamZ1 + 12.5f),
                        BogiePart.YAW_DAMPER to Point3D(-35f, frameY + 23.5f, beamZ2 - 12.5f),
                        BogiePart.AXLE_BOX_CTRB to Point3D(halfWheelbase, frameY, halfGauge + 15f),
                        BogiePart.AXLE_BOX_CTRB to Point3D(-halfWheelbase, frameY, -halfGauge - 15f),
                        BogiePart.BOGIE_FRAME to Point3D(0f, frameY + 10f, 0f),
                        BogiePart.ANTI_ROLL_BAR to Point3D(-35f, frameY + 15f, 0f),
                        BogiePart.CONTROL_ARM to Point3D(100f, frameY + 5f, beamZ1),
                        BogiePart.CONTROL_ARM to Point3D(-100f, frameY + 5f, beamZ2),
                        BogiePart.ANCHOR_LINK to Point3D(0f, frameY + 18f, 50f)
                    )

                    hotspots.forEach { (part, pt) ->
                        val projPt = proj(pt)
                        val isPartSelected = part == selectedPart
                        if (isPartSelected) {
                            val pulseRadius = 12f + (floatOffset + 5f) * 0.5f
                            drawCircle(
                                color = RailSteelBlue.copy(alpha = 0.25f),
                                radius = pulseRadius,
                                center = projPt
                            )
                            drawCircle(
                                color = RailSteelBlue,
                                radius = 6f,
                                center = projPt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.5f,
                                center = projPt
                            )
                        } else {
                            drawCircle(
                                color = Color.White,
                                radius = 5f,
                                center = projPt
                            )
                            drawCircle(
                                color = DarkMetal.copy(alpha = 0.5f),
                                radius = 5.5f,
                                center = projPt,
                                style = Stroke(width = 1.5f)
                            )
                            drawCircle(
                                color = RailSteelBlue.copy(alpha = 0.8f),
                                radius = 3f,
                                center = projPt
                            )
                        }
                    }
                }

                // --- 3D FLOATING TOOLTIP OVERLAY ---
                val selectedPartCenter = remember(selectedPart) {
                    when (selectedPart) {
                        BogiePart.WHEELSET_AXLE -> Point3D(120f, 0f, 0f)
                        BogiePart.BRAKE_DISC -> Point3D(120f, 0f, 30f)
                        BogiePart.PRIMARY_SPRING -> Point3D(120f, 24f, 92f)
                        BogiePart.AIR_SPRING -> Point3D(0f, 20f, 96f)
                        BogiePart.YAW_DAMPER -> Point3D(35f, 23.5f, 92f + 12.5f)
                        BogiePart.AXLE_BOX_CTRB -> Point3D(120f, 0f, 95f)
                        BogiePart.BOGIE_FRAME -> Point3D(0f, 10f, 0f)
                        BogiePart.ANTI_ROLL_BAR -> Point3D(-35f, 15f, 0f)
                        BogiePart.CONTROL_ARM -> Point3D(100f, 5f, 92f)
                        BogiePart.ANCHOR_LINK -> Point3D(0f, 18f, 50f)
                    }
                }

                val selectedAnimatedP = Point3D(selectedPartCenter.x, selectedPartCenter.y + floatOffset * 0.3f, selectedPartCenter.z)
                val selectedRotated = selectedAnimatedP.rotateY(yawAngle).rotateX(pitchAngle)
                val selectedProjected = selectedRotated.project(wPx, hPx, baseScale)

                val tooltipX = with(density) { selectedProjected.x.toDp() }
                val tooltipY = with(density) { selectedProjected.y.toDp() }

                val tooltipWidth = 190.dp
                val tooltipHeight = 110.dp
                val clampedTooltipX = (tooltipX - (tooltipWidth / 2)).coerceIn(8.dp, maxWidth - tooltipWidth - 8.dp)
                val clampedTooltipY = (tooltipY - tooltipHeight - 16.dp).coerceIn(8.dp, maxHeight - tooltipHeight - 8.dp)

                Box(
                    modifier = Modifier
                        .offset(x = clampedTooltipX, y = clampedTooltipY)
                        .width(tooltipWidth)
                        .height(tooltipHeight)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.5.dp, RailSteelBlue, RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(RailSteelBlue.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = RailSteelBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = selectedPart.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DarkMetal,
                                    maxLines = 1
                                )
                                Text(
                                    text = selectedPart.technicalCode,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                        }

                        HorizontalDivider(color = BorderSlate, thickness = 0.8.dp)

                        val details = when (selectedPart) {
                            BogiePart.WHEELSET_AXLE -> Pair("915mm Nominal Dia", "Wear: 1.2mm (Safe)")
                            BogiePart.BRAKE_DISC -> Pair("110mm Thick Disc", "Clearance: 1.5mm")
                            BogiePart.PRIMARY_SPRING -> Pair("Height: 285mm", "Double Nested Coil")
                            BogiePart.AIR_SPRING -> Pair("Pressure: 4.2 Bar", "Leveling Valve: OK")
                            BogiePart.YAW_DAMPER -> Pair("+/-190mm Stroke", "Dual-acting Hydraulic")
                            BogiePart.AXLE_BOX_CTRB -> Pair("Bearing Temp: 42°C", "CTRB Double Row")
                            BogiePart.BOGIE_FRAME -> Pair("Load Test: Passed", "FIAT Steel Casting")
                            BogiePart.ANTI_ROLL_BAR -> Pair("Dia: 50mm Solid Steel", "Rubber Bushing: No wear")
                            BogiePart.CONTROL_ARM -> Pair("Primary Spring Seat", "Silent Block Play: 0.2mm")
                            BogiePart.ANCHOR_LINK -> Pair("Longitudinal Tie Rod", "Force Transfer: Nominal")
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = details.first, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkMetal)
                                Box(
                                    modifier = Modifier
                                        .background(CleanGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = CleanGreen,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = details.second,
                                fontSize = 8.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Small legend overlay for rotating
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Swipe,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "3D Interactive Model",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Carousel / Chips of parts for quick selection
            Text(
                text = "Select Component to Inspect:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = DarkMetal.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scrollable chips row (using a grid or wrapping row)
                // Let's implement scrollable-like behavior or wrap with custom flowing layout
                // Since Row can overflow, let's list 4 main ones or let them click.
                // We will create a small grid of components so they are extremely easy to tap
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BogieChip(BogiePart.WHEELSET_AXLE, selectedPart == BogiePart.WHEELSET_AXLE, Modifier.weight(1f)) { onPartSelected(it) }
                        BogieChip(BogiePart.BRAKE_DISC, selectedPart == BogiePart.BRAKE_DISC, Modifier.weight(1f)) { onPartSelected(it) }
                        BogieChip(BogiePart.AIR_SPRING, selectedPart == BogiePart.AIR_SPRING, Modifier.weight(1f)) { onPartSelected(it) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BogieChip(BogiePart.PRIMARY_SPRING, selectedPart == BogiePart.PRIMARY_SPRING, Modifier.weight(1f)) { onPartSelected(it) }
                        BogieChip(BogiePart.YAW_DAMPER, selectedPart == BogiePart.YAW_DAMPER, Modifier.weight(1f)) { onPartSelected(it) }
                        BogieChip(BogiePart.AXLE_BOX_CTRB, selectedPart == BogiePart.AXLE_BOX_CTRB, Modifier.weight(1f)) { onPartSelected(it) }
                    }
                }
            }
        }
    }
}

// Kotlin extension helper to avoid missing method in older standard libraries
private fun Float.coerceAtBound(maxVal: Float): Float {
    return if (this > maxVal) maxVal else this
}

@Composable
fun BogieChip(
    part: BogiePart,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (BogiePart) -> Unit
) {
    val containerColor = if (isSelected) RailSteelBlue else Color.White
    val contentColor = if (isSelected) Color.White else DarkMetal
    val borderStroke = if (isSelected) null else BorderStroke(1.dp, BorderSlate)

    Button(
        onClick = { onClick(part) },
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = borderStroke,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(34.dp)
    ) {
        Text(
            text = part.displayName,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
