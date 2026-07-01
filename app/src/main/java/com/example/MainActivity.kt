package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.MaintenanceLog
import com.example.ui.components.Bogie3DViewer
import com.example.ui.components.BogiePart
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiChatState
import com.example.ui.viewmodel.LhbViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

enum class AppTab(val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    SPECS("3D Specs", Icons.Outlined.Info, Icons.Filled.Info),
    SCHEDULES("Schedules", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    TROUBLESHOOT("Troubleshoot", Icons.Outlined.Build, Icons.Filled.Build),
    LOGS("Inspect Logs", Icons.Outlined.Assignment, Icons.Filled.Assignment)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val viewModel: LhbViewModel = viewModel()
    var currentTab by remember { mutableStateOf(AppTab.SPECS) }
    var showNewLogDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RailSteelBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsTransit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "RailTech LHB",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DarkMetal,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "Bogie Maintenance Suite".uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Gray,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    ),
                    actions = {
                        IconButton(
                            onClick = { viewModel.resetNewLogFields(); showNewLogDialog = true },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(36.dp)
                                .background(LightSteel, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Log New Inspection",
                                tint = DarkMetal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
                HorizontalDivider(color = BorderSlate, thickness = 1.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = BorderSlate, thickness = 1.dp)
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(72.dp)
                ) {
                    AppTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) RailSteelBlue else Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) RailSteelBlue else Color.Gray
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = RailSteelBlue.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LightSteel)
        ) {
            when (currentTab) {
                AppTab.SPECS -> SpecsTabScreen(viewModel)
                AppTab.SCHEDULES -> SchedulesTabScreen()
                AppTab.TROUBLESHOOT -> TroubleshootTabScreen(viewModel)
                AppTab.LOGS -> LogsTabScreen(viewModel) {
                    showNewLogDialog = true
                }
            }
        }
    }

    // Modal Sheet or Dialog for Logging New Inspection
    if (showNewLogDialog) {
        NewLogDialog(
            viewModel = viewModel,
            onDismiss = { showNewLogDialog = false },
            onSave = {
                viewModel.submitMaintenanceLog {
                    showNewLogDialog = false
                }
            }
        )
    }
}

// ==========================================
// TAB 1: INTERACTIVE SPECS & 3D BOGIE
// ==========================================
@Composable
fun SpecsTabScreen(viewModel: LhbViewModel) {
    var selectedPart by remember { mutableStateOf(BogiePart.WHEELSET_AXLE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Interactive 3D Canvas Card
        Bogie3DViewer(
            selectedPart = selectedPart,
            onPartSelected = { selectedPart = it }
        )

        // Detailed Specs Card for Selected Part
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            border = BorderStroke(1.dp, BorderSlate)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(RailSteelBlue.copy(alpha = 0.1f), CircleShape)
                            .padding(10.dp)
                    ) {
                        val icon = when (selectedPart) {
                            BogiePart.WHEELSET_AXLE -> Icons.Default.Circle
                            BogiePart.BRAKE_DISC -> Icons.Default.RadioButtonChecked
                            BogiePart.PRIMARY_SPRING -> Icons.Default.FilterVintage
                            BogiePart.AIR_SPRING -> Icons.Default.LeakAdd
                            BogiePart.YAW_DAMPER -> Icons.Default.SwapVert
                            BogiePart.AXLE_BOX_CTRB -> Icons.Default.Layers
                            BogiePart.BOGIE_FRAME -> Icons.Default.GridGoldenratio
                            BogiePart.ANTI_ROLL_BAR -> Icons.Default.SyncAlt
                            BogiePart.CONTROL_ARM -> Icons.Default.Build
                            BogiePart.ANCHOR_LINK -> Icons.Default.Link
                        }
                        Icon(icon, contentDescription = null, tint = RailSteelBlue, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            text = selectedPart.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkMetal
                        )
                        Text(
                            text = "Component Code: ${selectedPart.technicalCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                HorizontalDivider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))

                // Render specs based on selection
                when (selectedPart) {
                    BogiePart.WHEELSET_AXLE -> {
                        SpecRow("Standard Gauge", "1676 mm (Indian Broad Gauge)")
                        SpecRow("Wheel Base", "2560 mm")
                        SpecRow("New Wheel Diameter", "915 mm")
                        SpecRow("Condemning Wheel Dia", "845 mm", isCritical = true)
                        SpecRow("Max Flat on Tread", "50 mm (Critical Safety Limit)", isCritical = true)
                        SpecRow("Axle Type", "Solid forged steel, Type Wheelset FIAT")
                    }
                    BogiePart.BRAKE_DISC -> {
                        SpecRow("Disc Diameter", "640 mm outer diameter")
                        SpecRow("New Disc Thickness", "110 mm")
                        SpecRow("Condemning Disc Thickness", "96 mm", isCritical = true)
                        SpecRow("Friction Pad New", "35 mm")
                        SpecRow("Friction Pad Condemning", "7 mm (Replace immediately)", isCritical = true)
                        SpecRow("Disc Quantity", "2 discs per axle (4 per bogie)")
                    }
                    BogiePart.PRIMARY_SPRING -> {
                        SpecRow("Spring Type", "Nested Helical Coil Springs (Outer & Inner)")
                        SpecRow("Quantity", "2 nests per axle box (8 nests per bogie)")
                        SpecRow("Nominal Height (Unloaded)", "approx. 291 mm")
                        SpecRow("Coil Material", "Silicon Manganese spring steel")
                        SpecRow("Primary Damper", "Vertical hydraulic shock absorber (1 per box)")
                    }
                    BogiePart.AIR_SPRING -> {
                        SpecRow("Air Spring Type", "Pneumatic rubber bellow with emergency rubber spring")
                        SpecRow("Nominal Design Height", "294 mm (+0 / -4 mm) under tare")
                        SpecRow("Operating Pressure", "1.5 to 6.0 kg/cm² depending on passenger load")
                        SpecRow("Levelling Valve", "1 per air spring (maintains constant height)")
                        SpecRow("Safety System", "FIBA (Failure Indication-cum-Brake Application) equipped")
                        SpecRow("Emergency Speed Limit", "60 km/h in case of sudden deflation", isCritical = true)
                    }
                    BogiePart.YAW_DAMPER -> {
                        SpecRow("Yaw Damper Purpose", "Resists hunting oscillations at high speed (> 120 km/h)")
                        SpecRow("Mounting Style", "Diagonally between bogie frame and coach underframe")
                        SpecRow("Mounting Torque", "170 Nm (Strict engineering mandate)", isCritical = true)
                        SpecRow("Bushing Style", "Spherical elastomeric joint")
                        SpecRow("Quantity", "2 per bogie, diagonally opposite")
                    }
                    BogiePart.AXLE_BOX_CTRB -> {
                        SpecRow("Bearing Design", "Cartridge Tapered Roller Bearing (CTRB)")
                        SpecRow("Manufacturer", "TIMKEN / FAG / NEI")
                        SpecRow("Lubrication Grease", "Lithshield / Grease Grade RR3 (350g approx.)")
                        SpecRow("Max Running Temp", "80°C (Hot Axle declaration above 80°C)", isCritical = true)
                        SpecRow("Security", "Three axle box end cover bolts secured by locking plate")
                    }
                    BogiePart.BOGIE_FRAME -> {
                        SpecRow("Frame Construction", "Heavy welded solid steel 'Y' side-frames & cross transoms")
                        SpecRow("Steel Grade", "Corten Steel / St 52 (High tensile weldable)")
                        SpecRow("Traction Transmission", "Traction rod and pivot pin system")
                        SpecRow("Secondary Lateral Stop", "Clearance: 25 mm ± 2 mm")
                        SpecRow("NDT Critical Points", "Welded joints around transom and bolster anchors")
                    }
                    BogiePart.ANTI_ROLL_BAR -> {
                        SpecRow("Bar Diameter", "50 mm solid steel torsion shaft")
                        SpecRow("Primary Function", "Resists carbody roll & stabilizes curve running")
                        SpecRow("Torsion Links", "Dual vertical rods with spherical rubber bushings")
                        SpecRow("Inspection Limit", "Bushing rubber crack depth must not exceed 1.5 mm", isCritical = true)
                        SpecRow("Fasteners Torque", "Torsion arm bolts: 210 Nm")
                    }
                    BogiePart.CONTROL_ARM -> {
                        SpecRow("Construction", "Cast steel guide arm clamped to axle box")
                        SpecRow("Pivot Design", "Single large elastomer silent block joint")
                        SpecRow("Function", "Transmits tractive & braking forces to bogie frame")
                        SpecRow("Bumper Clearance", "Vertical rubber bumper clearance: 32 mm")
                        SpecRow("Pivot Bolt Torque", "Bracket bolt torque: 280 Nm", isCritical = true)
                    }
                    BogiePart.ANCHOR_LINK -> {
                        SpecRow("Link Type", "Longitudinal traction anchor rods")
                        SpecRow("Quantity", "2 links per bogie (transmits tractive forces to coach bolster)")
                        SpecRow("Joint Type", "Spherical silent block (elastomeric pivot)")
                        SpecRow("Anchor Bolt Torque", "Bracket hinge pin: 310 Nm", isCritical = true)
                        SpecRow("Critical Flaw", "Inspect for rubber debonding from metal sleeves", isCritical = true)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Technician Tip Box
                Surface(
                    color = SoftBlueHighlight.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.BuildCircle, contentDescription = null, tint = RailSteelBlue)
                        Column {
                            Text(
                                text = "Technician Maintenance Checklist:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = DarkMetal
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val tipText = when (selectedPart) {
                                BogiePart.WHEELSET_AXLE -> "Always use a wheel profile gauge during D2 monthly checks. Look for thin flanges, deep cavities, or sharp flanges that may result in derailment risk."
                                BogiePart.BRAKE_DISC -> "Measure pad-to-disc clearance. It should be 1.0 to 1.5 mm. Check guide pin lubrication and ensure caliper slides freely."
                                BogiePart.PRIMARY_SPRING -> "Inspect primary vertical dampers for any traces of oil seepage. Damper oil leakage drastically reduces ride comfort and stability."
                                BogiePart.AIR_SPRING -> "Verify installation height (294 mm) with a specialized height gauge. If out of tolerance, adjust levelling valve linkage nuts."
                                BogiePart.YAW_DAMPER -> "Check bracket bolts torque (170 Nm). If bushings have cracked rubber or yaw dampers have excessive oil leaks, replace them immediately."
                                BogiePart.AXLE_BOX_CTRB -> "Always measure CTRB temperature at the washing lines immediately upon train arrival using an infrared non-contact thermometer."
                                BogiePart.BOGIE_FRAME -> "Check traction rod play. Any excessive lateral/longitudinal slop indicates worn silent blocks, requiring workshop overhaul."
                                BogiePart.ANTI_ROLL_BAR -> "Check the anti-roll bar bracket mounts and torsion links. Lubricate joints and look for any signs of rubber splitting or play in the vertical links during D3 checks."
                                BogiePart.CONTROL_ARM -> "Verify the condition of the silent block at the control arm's pivot point. Cracked rubber or eccentric sagging requires urgent wheelset removal and bush replacement."
                                BogiePart.ANCHOR_LINK -> "Inspect anchor link pins, nuts, and split pins. Verify that the rubber silent blocks show no radial/axial play or debonding from the metal casing to ensure safe tractive force transfer."
                            }
                            Text(
                                text = tipText,
                                fontSize = 11.sp,
                                color = DarkMetal,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecRow(label: String, value: String, isCritical: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isCritical) RailCrimson else Color.Gray,
            fontWeight = if (isCritical) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = if (isCritical) RailCrimson else DarkMetal,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

// ==========================================
// TAB 2: MAINTENANCE SCHEDULES TIMELINE
// ==========================================
data class ScheduleInfo(
    val code: String,
    val name: String,
    val interval: String,
    val description: String,
    val tasks: List<String>
)

@Composable
fun SchedulesTabScreen() {
    val schedules = remember {
        listOf(
            ScheduleInfo(
                "D1", "Trip Schedule", "Every Trip / Daily",
                "Primary visual inspection conducted at the washing lines before dispatch.",
                listOf(
                    "Visual inspection of wheels, tyres and check for flat tires (limit 50mm).",
                    "Check brake cylinder pressure indicators (Green/Red indicators).",
                    "Verify air bellows are inflated and FIBA failure indicators are green.",
                    "Quick scan of suspension springs and control arms for visual damage."
                )
            ),
            ScheduleInfo(
                "D2", "Monthly Schedule", "30 Days ± 3 days",
                "Detailed visual and clearance inspection in dry pits.",
                listOf(
                    "Measure brake pad thickness (Condemning 7 mm).",
                    "Measure wheel profile parameters with steel wheel gauge.",
                    "Inspect all hydraulic dampers (primary, secondary, yaw) for oil leakages.",
                    "Verify primary control arm pivot bolts for tightness.",
                    "Inspect air bellow skin for minor cracking and linkage adjustments."
                )
            ),
            ScheduleInfo(
                "D3", "Half-Yearly Schedule", "180 Days ± 15 days",
                "Advanced physical and non-destructive testing checks.",
                listOf(
                    "Ultrasonic Testing (UST) of axles to detect internal fatigue cracks.",
                    "Operational testing of WSP (Wheel Slide Protection) dumping valves.",
                    "Intensive cleaning of the underframe and bogie frame using steam jet.",
                    "Testing of emergency brake application and distributor valves (DV)."
                )
            ),
            ScheduleInfo(
                "SS-1", "Shop Schedule I", "1.5 Years or 6 Lakh km",
                "First light shop workshop schedule involving component profiling.",
                listOf(
                    "Full bogie run-out from under the coach.",
                    "Profiling of wheels on pit lathe to restore nominal tread profile.",
                    "Overhauling of brake cylinders and brake caliper assemblies.",
                    "Dismantling and complete overhauling of all hydraulic dampers."
                )
            ),
            ScheduleInfo(
                "SS-2", "Shop Schedule II", "3 Years or 12 Lakh km",
                "Mid-term workshop schedule with overhaul and replacements.",
                listOf(
                    "Complete strip-down of the FIAT bogie frame.",
                    "Replace all silent blocks and rubber components (gaskets, rings, seals).",
                    "Overhauling and height testing of pneumatic air bellows.",
                    "Structural Dye-Penetrant (NDT) testing to verify welded joints integrity."
                )
            ),
            ScheduleInfo(
                "SS-3", "Shop Schedule III", "6 Years or 24 Lakh km",
                "Heavy overhauling workshop schedule. Complete restoration.",
                listOf(
                    "Full bogie strip-down to bare frame.",
                    "Complete replacement of Axle bearings (CTRB) with new grease packs.",
                    "Wheel disc replacement if worn near condemning diameter.",
                    "Complete restoration and painting of the bogie frame and sub-assemblies."
                )
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Official IR LHB Maintenance Schedules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DarkMetal
            )
            Text(
                text = "Technicians must execute and sign off logs per these schedules.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(schedules) { schedule ->
            val badgeBg = if (schedule.code.startsWith("SS")) RailCrimson else RailSteelBlue
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(badgeBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = schedule.code,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = schedule.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkMetal
                            )
                        }
                        Text(
                            text = schedule.interval,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RailSteelBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = schedule.description,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Key Workshop Tasks:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkMetal
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    schedule.tasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "•",
                                fontSize = 14.sp,
                                color = RailCrimson,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = task,
                                fontSize = 11.sp,
                                color = DarkMetal,
                                lineHeight = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: DIAGNOSTIC GUIDE & AI COPILOT
// ==========================================
@Composable
fun TroubleshootTabScreen(viewModel: LhbViewModel) {
    var selectedGuideIndex by remember { mutableStateOf<Int?>(null) }
    val guides = remember {
        listOf(
            "Brake Binding Troubleshooting" to listOf(
                "Check the brake indicators on the coach side. Red means applied, Green means released.",
                "If indicator is Red but brakes should be released, pull the Manual Release Cord underframe on the Distributor Valve (DV).",
                "If it releases, investigate why the pilot air pressure was retained. Check air pipes.",
                "If brakes still do not release, isolate the affected coach by turning the Distributor Valve isolating handle.",
                "Physically inspect the brake caliper levers. Look for jammed pins, stuck cylinders, or handbrake cable over-tension.",
                "Tap caliper levers gently if a mechanical jam is detected. Spray lubricant on pins."
            ),
            "Air Spring Deflation (FIBA Active)" to listOf(
                "FIBA indicator showing RED means an air spring is punctured or has lost pressure below 1.5 kg/cm².",
                "The FIBA system will automatically apply emergency brakes to limit train speed.",
                "Check the affected side of the bogie. Inspect air bellows visually for tear, rupture, or displacement.",
                "Measure bellow installation height. (Normal is 294 mm; flat deflated is ~250 mm).",
                "Isolate the defective air spring using the isolating cock on the auxiliary reservoir pipe.",
                "Pull the FIBA indicator resetting valve handle to reset indicator back to GREEN and release emergency brakes.",
                "Declare caution order: Speed must be limited to 60 km/h to reach nearest maintenance hub."
            ),
            "Hot Axle CTRB Detection" to listOf(
                "Use a non-contact infrared thermometer to measure CTRB temperature instantly upon arrival of coach.",
                "If CTRB box temperature is > 80°C (or > 30°C above ambient), declare as 'Hot Axle'.",
                "Immediately isolate/detach the coach. Do NOT allow high-speed movement.",
                "Inspect axle box visually for signs of grease oozing, heat discolouration, or grease leaks at rear cover seal.",
                "Check if end cover bolts are loose or missing. Any missing bolt indicates immediate bearing failure risk."
            ),
            "WSP (Wheel Slide Protection) Code Faults" to listOf(
                "Locate the WSP controller panel in the electrical cabinet inside the coach.",
                "Read the LED/LCD error code. Code 95 is standard 'System Healthy'.",
                "Code 72 indicates speed sensor gap error on one of the axles.",
                "Locate the faulty sensor. Check gap to phonic wheel using feeler gauge (should be 0.9 mm to 1.4 mm).",
                "Adjust sensor holder screws to achieve proper gap.",
                "Clean phonic wheel teeth of dust, mud, and steel particles using wire brush.",
                "Code 99 indicates multiple/general power fault. Inspect WSP battery supply."
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Dynamic Flowchart Guides
        Text(
            text = "Step-by-Step Diagnostic Checklists",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DarkMetal
        )

        guides.forEachIndexed { idx, guide ->
            val isExpanded = selectedGuideIndex == idx
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedGuideIndex = if (isExpanded) null else idx },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = if (idx == 0) Icons.Default.CancelPresentation else if (idx == 1) Icons.Default.TrendingDown else if (idx == 2) Icons.Default.DeviceThermostat else Icons.Default.DeveloperBoard,
                                contentDescription = null,
                                tint = RailCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = guide.first,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkMetal
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = BorderSlate, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        guide.second.forEachIndexed { stepIdx, step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(RailSteelBlue.copy(alpha = 0.1f), CircleShape)
                                        .size(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${stepIdx + 1}",
                                        color = RailSteelBlue,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = step,
                                    fontSize = 11.sp,
                                    color = DarkMetal,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: AI Tech Assistant Chat Panel
        Text(
            text = "AI Technical Troubleshooting Assistant",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DarkMetal,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            border = BorderStroke(1.dp, BorderSlate)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of AI chat
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFC))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(CleanGreen, CircleShape)
                                .size(8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LHB Copie-Bot (AI Active)",
                            color = DarkMetal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                HorizontalDivider(color = BorderSlate, thickness = 1.dp)

                // Messages list
                val listState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                        .verticalScroll(listState)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        viewModel.chatMessages.forEach { msg ->
                            val isAi = msg.sender == "ai"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                            ) {
                                Surface(
                                    color = if (isAi) LightSteel else RailSteelBlue,
                                    shape = RoundedCornerShape(
                                        topStart = 8.dp,
                                        topEnd = 8.dp,
                                        bottomStart = if (isAi) 0.dp else 8.dp,
                                        bottomEnd = if (isAi) 8.dp else 0.dp
                                    ),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        fontSize = 11.sp,
                                        color = if (isAi) DarkMetal else Color.White,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }

                        // Auto scroll to bottom
                        LaunchedEffect(viewModel.chatMessages.size) {
                            listState.animateScrollTo(listState.maxValue)
                        }
                    }
                }

                if (viewModel.aiChatState is AiChatState.Loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = RailCrimson,
                        trackColor = LightSteel
                    )
                }

                // Predefined questions quick bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(Color(0xFFF0F4F7))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PredefinedChip("Brake Binding released DV?") { viewModel.currentChatInput = it }
                    PredefinedChip("FIBA deflation speed?") { viewModel.currentChatInput = it }
                    PredefinedChip("CTRB hot axle temp?") { viewModel.currentChatInput = it }
                    PredefinedChip("WSP error code 72?") { viewModel.currentChatInput = it }
                    PredefinedChip("Yaw damper torque Nm?") { viewModel.currentChatInput = it }
                }

                // Input box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val keyboardController = LocalSoftwareKeyboardController.current

                    OutlinedTextField(
                        value = viewModel.currentChatInput,
                        onValueChange = { viewModel.currentChatInput = it },
                        placeholder = { Text("Ask Copie-Bot about LHB maintenance...", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RailSteelBlue,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                viewModel.sendAiQuestion()
                                keyboardController?.hide()
                            }
                        )
                    )

                    IconButton(
                        onClick = {
                            viewModel.sendAiQuestion()
                            keyboardController?.hide()
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = RailSteelBlue, contentColor = Color.White),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PredefinedChip(text: String, onClick: (String) -> Unit) {
    Button(
        onClick = { onClick(text) },
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DarkMetal),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        border = BorderStroke(1.dp, Color.LightGray),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(28.dp)
    ) {
        Text(text = text, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ==========================================
// TAB 4: INSPECTION LOGS (ROOM PERSISTENCE)
// ==========================================
@Composable
fun LogsTabScreen(
    viewModel: LhbViewModel,
    onNewLogRequested: () -> Unit
) {
    val logs by viewModel.logsState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Analytics Dashboard Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RailSteelBlue),
            elevation = CardDefaults.cardElevation(2.dp),
            border = BorderStroke(1.dp, BorderSlate)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Technician Workshop Dashboard",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox("Logs Saved", "${logs.size}", Icons.Default.Inventory)
                    
                    val warningLogs = logs.filter {
                        !it.isBrakePadsOk || !it.isBrakeDiscsOk || !it.isCylinderDampersOk ||
                        !it.isCtrbTemperatureOk || !it.isAirSpringPressureOk || 
                        !it.isPhonicWheelSensorOk || !it.isTractionRodOk
                    }.size
                    StatBox("Attention Req.", "$warningLogs", Icons.Default.Warning, if (warningLogs > 0) RailCrimson else CleanGreen)
                    
                    val lastLog = if (logs.isNotEmpty()) {
                        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                        sdf.format(Date(logs[0].timestamp))
                    } else "N/A"
                    StatBox("Last Inspection", lastLog, Icons.Default.EventNote)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Inspection History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DarkMetal
            )

            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearAllLogs() },
                    colors = ButtonDefaults.textButtonColors(contentColor = RailCrimson)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 11.sp)
                }
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, BorderSlate), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentLate,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No Inspection Logs Found",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Press the (+) button above or button below to create your first LHB maintenance log.",
                        textAlign = TextAlign.Center,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Button(
                        onClick = onNewLogRequested,
                        colors = ButtonDefaults.buttonColors(containerColor = RailSteelBlue)
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Checklist")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { log ->
                    LogItemCard(log = log, onDelete = { viewModel.deleteLog(log.id) })
                }
            }
        }
    }
}

@Composable
fun StatBox(title: String, valStr: String, icon: ImageVector, tint: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = tint.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = valStr, color = tint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = title, color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp)
    }
}

@Composable
fun LogItemCard(log: MaintenanceLog, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    val formattedDate = sdf.format(Date(log.timestamp))

    val failedChecks = mutableListOf<String>()
    if (!log.isBrakePadsOk) failedChecks.add("Brake Pads")
    if (!log.isBrakeDiscsOk) failedChecks.add("Brake Discs")
    if (!log.isCylinderDampersOk) failedChecks.add("Dampers/Cylinders")
    if (!log.isCtrbTemperatureOk) failedChecks.add("CTRB Bearings")
    if (!log.isAirSpringPressureOk) failedChecks.add("Air Pressure")
    if (!log.isPhonicWheelSensorOk) failedChecks.add("WSP Sensors")
    if (!log.isTractionRodOk) failedChecks.add("Traction Link")

    val isHealthy = failedChecks.isEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = log.coachNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkMetal
                        )
                        Box(
                            modifier = Modifier
                                .background(RailCrimson.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = log.scheduleType,
                                color = RailCrimson,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Tech: ${log.technicianName} • $formattedDate",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete log", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderSlate, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Overall Status Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isHealthy) CleanGreen else RailCrimson,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isHealthy) "Bogie fully serviceable (All checks passed)" else "Alert: Attention required! Faulty parts found.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHealthy) CleanGreen else RailCrimson
                )
            }

            if (!isHealthy) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Defective components: ${failedChecks.joinToString(", ")}",
                    fontSize = 10.sp,
                    color = RailCrimson,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (log.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = SoftBlueHighlight.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notes: ${log.notes}",
                        fontSize = 10.sp,
                        color = DarkMetal,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// NEW INSPECTION LOG WALKTHROUGH DIALOG
// ==========================================
@Composable
fun NewLogDialog(
    viewModel: LhbViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.PostAdd, contentDescription = null, tint = RailSteelBlue)
                Text(text = "LHB Bogie Inspection Checklist", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.newCoachNumber,
                    onValueChange = { viewModel.newCoachNumber = it },
                    label = { Text("Coach Number (e.g., 193245)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.newTechnicianName,
                    onValueChange = { viewModel.newTechnicianName = it },
                    label = { Text("Technician Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Schedule Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Schedule:", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    listOf("D1", "D2", "D3", "SS1").forEach { sched ->
                        FilterChip(
                            selected = viewModel.newScheduleType == sched,
                            onClick = { viewModel.newScheduleType = sched },
                            label = { Text(sched, fontSize = 10.sp) }
                        )
                    }
                }

                Divider(color = Color.LightGray, thickness = 0.5.dp)

                Text("Checklist Walkthrough:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                CheckboxRow("Wheelset & Axle Profile (No flats, cracks)", viewModel.checkBrakePads) { viewModel.checkBrakePads = it }
                CheckboxRow("Brake Disc Thickness & Security (> 96mm)", viewModel.checkBrakeDiscs) { viewModel.checkBrakeDiscs = it }
                CheckboxRow("Suspension Damper Seepage & Cylinder Leak", viewModel.checkCylinderDampers) { viewModel.checkCylinderDampers = it }
                CheckboxRow("CTRB Bearings (No hot spot, running smooth)", viewModel.checkCtrbTemp) { viewModel.checkCtrbTemp = it }
                CheckboxRow("Secondary Bellow Pressure & Levelling Linkage", viewModel.checkAirSpringPressure) { viewModel.checkAirSpringPressure = it }
                CheckboxRow("WSP speed sensors & Phonic wheel (Gap OK)", viewModel.checkPhonicWheelSensor) { viewModel.checkPhonicWheelSensor = it }
                CheckboxRow("Traction Center pivot & Anchors torque", viewModel.checkTractionRod) { viewModel.checkTractionRod = it }

                OutlinedTextField(
                    value = viewModel.newNotes,
                    onValueChange = { viewModel.newNotes = it },
                    label = { Text("Inspection Notes / Remedial Actions") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = RailCrimson),
                enabled = viewModel.newCoachNumber.isNotBlank() && viewModel.newTechnicianName.isNotBlank()
            ) {
                Text("Save Log", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun CheckboxRow(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = CleanGreen)
        )
        Text(text = label, fontSize = 11.sp, color = DarkMetal)
    }
}
