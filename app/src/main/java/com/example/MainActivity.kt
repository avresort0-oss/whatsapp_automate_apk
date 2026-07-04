package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.BypassSettingsPanel
import com.example.ChatListScannerPanel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                
                // Periodically check if accessibility service is enabled when app resumes
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    while(true) {
                        viewModel.checkServiceStatus()
                        kotlinx.coroutines.delay(2000)
                    }
                }

                val automationActive by viewModel.automationActive.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        if (automationActive) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    viewModel.toggleAutomation(false)
                                },
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.testTag("stop_automation_fab"),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Stop Automation"
                                    )
                                },
                                text = {
                                    Text("Stop Automation", fontWeight = FontWeight.Bold)
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    MainDashboardScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val targetContacts by viewModel.targetContacts.collectAsState()
    val quickReplyMessage by viewModel.quickReplyMessage.collectAsState()
    val automationActive by viewModel.automationActive.collectAsState()
    val isServiceEnabled by viewModel.isServiceEnabled.collectAsState()
    val humanDelay by viewModel.humanDelay.collectAsState()
    val onlyBlueTicks by viewModel.onlyBlueTicks.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val logs by AutomationLogManager.logs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Top Header
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val isUnderTest = remember {
                            try {
                                Class.forName("org.junit.Test") != null
                            } catch (e: Exception) {
                                false
                            }
                        }

                        if (!isUnderTest) {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_icon),
                                contentDescription = "App Icon",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "App Icon",
                                tint = Color.White,
                                modifier = Modifier.padding(4.dp).fillMaxSize()
                            )
                        }
                    }
                    Text(
                        text = "WhatsApp Automator",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = Color.White
            )
        )

        // Custom Tabs Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.testTag("tab_settings")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        "Settings & Logs",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Gray,
                        fontSize = 15.sp
                    )
                }
            }
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.testTag("tab_simulator")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        "Visual Simulator",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Gray,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Main Contents
        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedTab == 0) {
                SettingsAndLogsTab(
                    context = context,
                    viewModel = viewModel,
                    targetContacts = targetContacts,
                    quickReplyMessage = quickReplyMessage,
                    automationActive = automationActive,
                    isServiceEnabled = isServiceEnabled,
                    humanDelay = humanDelay,
                    onlyBlueTicks = onlyBlueTicks,
                    selectedImageUri = selectedImageUri,
                    logs = logs
                )
            } else {
                SimulatorTab(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SettingsAndLogsTab(
    context: Context,
    viewModel: MainViewModel,
    targetContacts: String,
    quickReplyMessage: String,
    automationActive: Boolean,
    isServiceEnabled: Boolean,
    humanDelay: Int,
    onlyBlueTicks: Boolean,
    selectedImageUri: Uri?,
    logs: List<AutomationLogManager.LogEntry>
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.selectImage(uri)
                AutomationLogManager.log("Quick reply image template updated successfully!", AutomationLogManager.LogType.SUCCESS)
            }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Image Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero),
                    contentDescription = "Automation Hero Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        "Chat Automation Assistant",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Automate searches, human scrolling, and replies easily",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Accessibility Service Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceEnabled) Color(0xFF0F241E) else Color(0xFF281C1B)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isServiceEnabled) Color(0xFF00A884).copy(alpha = 0.4f) else Color(0xFFEF5350).copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isServiceEnabled) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                contentDescription = "Status Icon",
                                tint = if (isServiceEnabled) Color(0xFF25D366) else Color(0xFFEF5350),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isServiceEnabled) "Accessibility Service: ENABLED" else "Accessibility Service: INACTIVE",
                                fontWeight = FontWeight.Bold,
                                color = if (isServiceEnabled) Color(0xFFE9EDEF) else Color(0xFFEF5350),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isServiceEnabled) 
                                "Automation is ready to process. Turn on toggle to activate." 
                                else "You must enable the Accessibility Service in your device Settings to run.",
                            color = Color(0xFF8696A0),
                            fontSize = 12.sp
                        )
                    }
                    if (!isServiceEnabled) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("enable_service_button")
                        ) {
                            Text("Enable", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Automation Config Panel Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Automation Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                        
                        // Active / Inactive Switch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (automationActive) "ACTIVE" else "OFF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (automationActive) Color(0xFF25D366) else Color.Gray,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Switch(
                                checked = automationActive,
                                onCheckedChange = { viewModel.toggleAutomation(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF25D366),
                                    checkedTrackColor = Color(0xFF00A884).copy(alpha = 0.3f)
                                ),
                                enabled = isServiceEnabled,
                                modifier = Modifier.testTag("automation_toggle")
                            )
                        }
                    }

                    // Field 1: Target Contacts
                    Column {
                        Text(
                            "Target Contacts (কমা দিয়ে আলাদা করুন)",
                            color = Color(0xFFE9EDEF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = targetContacts,
                            onValueChange = { viewModel.updateTargetContacts(it) },
                            placeholder = { Text("e.g., Abir, Jahid, Robin", color = Color.Gray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("contacts_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF202C33),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color.Gray
                            )
                        )
                        Text(
                            "এই নামগুলোর চ্যাট আসলে স্বয়ংক্রিয়ভাবে ওপেন করে মেসেজ পাঠাবে।",
                            color = Color(0xFF8696A0),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Field 1.5: Smart Double Blue Tick Filter
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF202C33).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Overlapping Double check icon
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Read Tick",
                                        tint = Color(0xFF53BDEB),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Read Tick",
                                        tint = Color(0xFF53BDEB),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .offset(x = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Double Blue Tick Filter",
                                    color = Color(0xFFE9EDEF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Switch(
                                checked = onlyBlueTicks,
                                onCheckedChange = { viewModel.updateOnlyBlueTicks(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF53BDEB),
                                    checkedTrackColor = Color(0xFF53BDEB).copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF202C33)
                                ),
                                modifier = Modifier.testTag("only_blue_ticks_toggle")
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "শুধুমাত্র সেই চ্যাটগুলোতে উত্তর পাঠাবে যেগুলোতে ডাবল ব্লু টিক (পঠিত/Read) রয়েছে।\nOnly reply to contacts who have read your message (shown by a double blue tick).",
                            color = Color(0xFF8696A0),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    // Field 2: Quick Reply Message Text
                    Column {
                        Text(
                            "Quick Reply Message",
                            color = Color(0xFFE9EDEF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = quickReplyMessage,
                            onValueChange = { viewModel.updateQuickReplyMessage(it) },
                            placeholder = { Text("Enter message text to send", color = Color.Gray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("message_input"),
                            shape = RoundedCornerShape(8.dp),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF202C33)
                            )
                        )
                    }

                    // Field 3: Anti-Detection Delay Time Settings
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Human-Like Delay / Anti-Detection",
                                color = Color(0xFFE9EDEF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${(humanDelay / 1000.0)}s",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Configure the delay time (in milliseconds) between sending automated messages to avoid potential detection of bot-like behavior.",
                            color = Color(0xFF8696A0),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Slider
                            Slider(
                                value = humanDelay.toFloat(),
                                onValueChange = { viewModel.updateHumanDelay(it.toInt()) },
                                valueRange = 1000f..10000f,
                                steps = 17, // 1000 to 10000 in steps of 500
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("delay_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color(0xFF202C33)
                                )
                            )
                            
                            // Editable Delay Input Field
                            OutlinedTextField(
                                value = humanDelay.toString(),
                                onValueChange = { newVal ->
                                    val filtered = newVal.filter { it.isDigit() }
                                    if (filtered.isNotEmpty()) {
                                        val parsed = filtered.toIntOrNull() ?: 2500
                                        viewModel.updateHumanDelay(parsed.coerceIn(500, 30000))
                                    } else {
                                        viewModel.updateHumanDelay(0)
                                    }
                                },
                                modifier = Modifier
                                    .width(90.dp)
                                    .testTag("delay_input_field"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF202C33),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }

                    // Field 4: Image Quick Reply Attachment
                    Column {
                        Text(
                            "Quick Reply Image Attachment (ঐচ্ছিক / Optional)",
                            color = Color(0xFFE9EDEF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Select an image from your gallery to automatically attach to quick replies.",
                            color = Color(0xFF8696A0),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedImageUri == null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("pick_image_button"),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF111B21)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF202C33)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF202C33)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AddCircle,
                                            contentDescription = "Add Image Icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "No Image Selected",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "Tap to choose from gallery",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF111B21)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF00A884).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // High Quality Image Preview
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF202C33))
                                            .border(1.dp, Color(0xFF00A884).copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val bitmap = rememberUriImageBitmap(selectedImageUri, context)
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap,
                                                contentDescription = "Selected quick reply image",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Loaded successfully",
                                                tint = Color(0xFF25D366)
                                            )
                                        }
                                    }

                                    // Details & Actions Column
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "Active Indicator",
                                                tint = Color(0xFF25D366),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Active Payload Image",
                                                color = Color(0xFF25D366),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val displayName = selectedImageUri?.lastPathSegment ?: "gallery_image.jpg"
                                        Text(
                                            text = displayName,
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Actions: Change & Remove
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Change Image button
                                            IconButton(
                                                onClick = {
                                                    imagePickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(Color(0xFF202C33), CircleShape)
                                                    .testTag("change_image_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Refresh,
                                                    contentDescription = "Change Image",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            // Remove Image Button
                                            IconButton(
                                                onClick = {
                                                    viewModel.selectImage(null)
                                                    AutomationLogManager.log("Quick reply image cleared.", AutomationLogManager.LogType.WARNING)
                                                },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(Color(0xFF2C1614), CircleShape)
                                                    .testTag("clear_image_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Remove Image",
                                                    tint = Color(0xFFEF5350),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Direct Launch WhatsApp Button
                    Button(
                        onClick = {
                            try {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                    AutomationLogManager.log("Launching WhatsApp! Starting background automation scanning.", AutomationLogManager.LogType.SUCCESS)
                                } else {
                                    AutomationLogManager.log("WhatsApp is not installed on this device.", AutomationLogManager.LogType.ERROR)
                                }
                            } catch (e: Exception) {
                                AutomationLogManager.log("Could not open WhatsApp: ${e.localizedMessage}", AutomationLogManager.LogType.ERROR)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("launch_whatsapp_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Open WhatsApp to Automate", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // === NEW: Bypass / Anti-Detection Settings Panel ===
        item {
            BypassSettingsPanel(viewModel = viewModel)
        }

        // === NEW: Chat List Scanner Panel ===
        item {
            ChatListScannerPanel(viewModel = viewModel)
        }

        // Live Log Terminal Panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF090F13)),
                border = BorderStroke(1.dp, Color(0xFF202C33)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (automationActive) Color(0xFF25D366) else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Live Automation Logs",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(
                            onClick = { AutomationLogManager.clear() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Clear logs",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Color(0xFF202C33))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No activity logs yet. Live system logs will scroll here.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(logs) { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "[${log.timestamp}] ",
                                        color = Color(0xFF00E676).copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    val textColor = when (log.type) {
                                        AutomationLogManager.LogType.SUCCESS -> Color(0xFF25D366)
                                        AutomationLogManager.LogType.WARNING -> Color(0xFFFFD54F)
                                        AutomationLogManager.LogType.ERROR -> Color(0xFFEF5350)
                                        AutomationLogManager.LogType.INFO -> Color(0xFFE9EDEF)
                                    }
                                    Text(
                                        text = log.message,
                                        color = textColor,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatFiltersRow(
    selectedFilter: MainViewModel.ContactFilter,
    onFilterSelected: (MainViewModel.ContactFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val filters = listOf(
            Triple(MainViewModel.ContactFilter.ALL, "All", null),
            Triple(MainViewModel.ContactFilter.UNREAD, "Unread", null),
            Triple(MainViewModel.ContactFilter.READ, "Read", Icons.Filled.Check),
            Triple(MainViewModel.ContactFilter.BLUE_TICK, "Blue-Tick", null)
        )

        filters.forEach { (filter, label, icon) ->
            val isSelected = selectedFilter == filter
            val bg = if (isSelected) Color(0xFF00A884).copy(alpha = 0.25f) else Color(0xFF202C33)
            val border = if (isSelected) BorderStroke(1.dp, Color(0xFF00A884)) else BorderStroke(1.dp, Color.Transparent)
            val contentColor = if (isSelected) Color(0xFF00A884) else Color(0xFF8696A0)

            Surface(
                onClick = { onFilterSelected(filter) },
                shape = RoundedCornerShape(20.dp),
                color = bg,
                border = border,
                modifier = Modifier.testTag("filter_chip_${label.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (filter == MainViewModel.ContactFilter.BLUE_TICK) {
                        // Draw double blue check
                        Box(
                            modifier = Modifier.size(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF53BDEB) else Color(0xFF8696A0),
                                modifier = Modifier.size(11.dp)
                            )
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF53BDEB) else Color(0xFF8696A0),
                                modifier = Modifier
                                    .size(11.dp)
                                    .offset(x = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (filter == MainViewModel.ContactFilter.UNREAD) {
                        // Unread green dot indicator
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFFE9EDEF) else Color(0xFF8696A0),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun SimulatorTab(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val isSimulatorRunning by viewModel.isSimulatorRunning.collectAsState()
    val simulatorStatus by viewModel.simulatorStatus.collectAsState()
    val simulatorScreen by viewModel.simulatorScreen.collectAsState()
    val activeChatName by viewModel.activeChatName.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    val chatListState = rememberLazyListState()

    val filteredChats = remember(viewModel.mockChats, selectedFilter) {
        viewModel.mockChats.filter { chat ->
            when (selectedFilter) {
                MainViewModel.ContactFilter.ALL -> true
                MainViewModel.ContactFilter.READ -> !chat.isUnread
                MainViewModel.ContactFilter.UNREAD -> chat.isUnread
                MainViewModel.ContactFilter.BLUE_TICK -> chat.hasDoubleBlueTick
            }
        }
    }

    // Trigger auto-scrolling of simulator chat list if running
    LaunchedEffect(isSimulatorRunning) {
        if (isSimulatorRunning && simulatorScreen == MainViewModel.SimulatorScreen.CHAT_LIST) {
            chatListState.animateScrollToItem(0) // Start from top
            kotlinx.coroutines.delay(2000)
            chatListState.animateScrollToItem(chatListState.layoutInfo.totalItemsCount - 1) // Scroll down like a human!
            kotlinx.coroutines.delay(2000)
            chatListState.animateScrollToItem(0) // Back to matched item
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Control Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.3f)) {
                    Text(
                        "Automation Simulator",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Status: $simulatorStatus",
                        color = if (isSimulatorRunning) Color(0xFF25D366) else Color.Gray,
                        fontWeight = if (isSimulatorRunning) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = { viewModel.runAutomationSimulation() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimulatorRunning) Color.Gray else Color(0xFF25D366)
                    ),
                    enabled = !isSimulatorRunning,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("run_simulator_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // WhatsApp Simulated Container Device Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF202C33), RoundedCornerShape(16.dp))
                .background(Color(0xFF0B141A))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // WhatsApp Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF1F2C34))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (simulatorScreen == MainViewModel.SimulatorScreen.CHAT_DETAIL) {
                        IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeChatName.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = activeChatName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isTyping) "typing..." else "online",
                                color = if (isTyping) Color(0xFF25D366) else Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        // Main WhatsApp list header
                        Text(
                            text = "WhatsApp Mock",
                            color = Color(0xFF8696A0),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF8696A0),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // WhatsApp Screen Body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (simulatorScreen == MainViewModel.SimulatorScreen.CHAT_LIST) {
                        // List Screen with Custom modern Filters Row
                        Column(modifier = Modifier.fillMaxSize()) {
                            ChatFiltersRow(
                                selectedFilter = selectedFilter,
                                onFilterSelected = { viewModel.updateSelectedFilter(it) }
                            )

                            LazyColumn(
                                state = chatListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                            ) {
                                items(filteredChats) { chat ->
                                    // Custom animated highlight if matched
                                    val itemBgColor by animateColorAsState(
                                        targetValue = if (chat.isMatched) Color(0xFF00A884).copy(alpha = 0.25f) else Color.Transparent,
                                        label = "matched_bg"
                                    )
                                    val borderModifier = if (chat.isMatched) {
                                        Modifier.border(1.5.dp, Color(0xFF25D366), RoundedCornerShape(8.dp))
                                    } else Modifier

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(itemBgColor)
                                            .then(borderModifier)
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Profile Avatar
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(Color(chat.colorHex)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = chat.name.take(1),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Chat Texts
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = chat.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = chat.time,
                                                    color = if (chat.isUnread && !chat.isCompleted) Color(0xFF25D366) else Color(0xFF8696A0),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (chat.isUnread && !chat.isCompleted) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (chat.lastMessageSentByMe) {
                                                    Box(
                                                        modifier = Modifier.width(20.dp),
                                                        contentAlignment = Alignment.CenterStart
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = "Tick 1",
                                                            tint = if (chat.hasDoubleBlueTick) Color(0xFF53BDEB) else Color.Gray,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = "Tick 2",
                                                            tint = if (chat.hasDoubleBlueTick) Color(0xFF53BDEB) else Color.Gray,
                                                            modifier = Modifier
                                                                .size(13.dp)
                                                                .offset(x = 5.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = chat.lastMessage,
                                                    color = if (chat.isUnread && !chat.isCompleted) Color(0xFFE9EDEF) else Color(0xFF8696A0),
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontWeight = if (chat.isUnread && !chat.isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                
                                                // Unread circular badge
                                                if (chat.isUnread && !chat.isCompleted) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(start = 6.dp)
                                                            .size(18.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF25D366)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = chat.unreadCount.toString(),
                                                            color = Color(0xFF0B141A),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Checkmark if processed
                                        if (chat.isCompleted) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF005C4B)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = "Success",
                                                    tint = Color(0xFF25D366),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                    Divider(color = Color(0xFF202C33), thickness = 0.5.dp)
                                }
                            }
                        }
                    } else {
                        // Chat details screen with custom WhatsApp chat wallpaper!
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0B141A))
                        ) {
                            // Chat messages list
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(viewModel.activeChatMessages) { message ->
                                    val bubbleBg = if (message.isSender) Color(0xFF005C4B) else Color(0xFF202C33)
                                    val align = if (message.isSender) Alignment.CenterEnd else Alignment.CenterStart

                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = align
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (message.isSender) 12.dp else 0.dp,
                                                bottomEnd = if (message.isSender) 0.dp else 12.dp
                                            ),
                                            colors = CardDefaults.cardColors(containerColor = bubbleBg),
                                            modifier = Modifier.widthIn(max = 240.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                // Load Image attachment if sent
                                                if (message.imageUri != null) {
                                                    val bitmap = rememberUriImageBitmap(message.imageUri, context)
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(130.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.Black.copy(alpha = 0.2f))
                                                            .padding(bottom = 6.dp)
                                                    ) {
                                                        if (bitmap != null) {
                                                            Image(
                                                                bitmap = bitmap,
                                                                contentDescription = "Attached visual quick reply",
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            // Fallback nice card decoration
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .background(Color(0xFF075E54)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                    Icon(
                                                                        imageVector = Icons.Filled.CheckCircle,
                                                                        contentDescription = "Selected",
                                                                        tint = Color(0xFF25D366),
                                                                        modifier = Modifier.size(28.dp)
                                                                    )
                                                                    Spacer(modifier = Modifier.height(4.dp))
                                                                    Text(
                                                                        "Image Template Sent",
                                                                        color = Color.White,
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = message.text,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    lineHeight = 17.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    modifier = Modifier.align(Alignment.End),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = message.time,
                                                        color = Color(0xFF8696A0),
                                                        fontSize = 9.sp
                                                    )
                                                    if (message.isSender) {
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = "Read",
                                                            tint = Color(0xFF53BDEB),
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // WhatsApp Mock Bottom Send Bar
                if (simulatorScreen == MainViewModel.SimulatorScreen.CHAT_DETAIL) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1F2C34))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Simulated Input Field
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(Color(0xFF2A3942))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (isTyping) "typing..." else "Message typing completed...",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00A884)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberUriImageBitmap(uri: Uri?, context: Context): ImageBitmap? {
    return remember(uri) {
        if (uri == null) return@remember null
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
