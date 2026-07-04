package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ============================================================================
 * BypassSettingsPanel - Anti-Detection Settings এর UI
 * ============================================================================
 *
 * এই Composable টা MainActivity তে "Bypass Settings" নামে নতুন একটা tab/card
 * হিসেবে যোগ করতে হবে।
 *
 * যোগ করার উপায়:
 *  1. MainActivity.kt তে এই import যোগ করো:
 *     import com.example.BypassSettingsPanel
 *     import com.example.ChatListScannerPanel
 *
 *  2. SettingsAndLogsTab ফাংশনের ভেতরে LazyColumn এ নিচের item গুলো যোগ করো:
 *
 *     item { BypassSettingsPanel(viewModel = viewModel) }
 *     item { ChatListScannerPanel(viewModel = viewModel) }
 * ============================================================================
 */
@Composable
fun BypassSettingsPanel(viewModel: MainViewModel) {
    val bypassEnabled by viewModel.bypassEnabled.collectAsState()
    val minDelay by viewModel.bypassMinDelay.collectAsState()
    val maxDelay by viewModel.bypassMaxDelay.collectAsState()
    val tapRandomness by viewModel.bypassTapRandomness.collectAsState()
    val typingSim by viewModel.bypassTypingSim.collectAsState()
    val typingSpeed by viewModel.bypassTypingSpeed.collectAsState()
    val sessionLimit by viewModel.bypassSessionLimit.collectAsState()
    val nightMode by viewModel.bypassNightMode.collectAsState()
    val randomPauses by viewModel.bypassRandomPauses.collectAsState()
    val readBeforeReply by viewModel.bypassReadBeforeReply.collectAsState()
    val maxPerHour by viewModel.bypassMaxPerHour.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1F2E)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (bypassEnabled) Color(0xFF00A884).copy(alpha = 0.4f)
            else Color(0xFFEF5350).copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // === Header ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Bypass",
                        tint = if (bypassEnabled) Color(0xFF00A884) else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Bypass / Anti-Detection",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
                Switch(
                    checked = bypassEnabled,
                    onCheckedChange = { viewModel.updateBypassEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00A884),
                        checkedTrackColor = Color(0xFF00A884).copy(alpha = 0.3f)
                    )
                )
            }

            if (bypassEnabled) {
                // === Delay Range Slider ===
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Delay Range (ms)",
                            color = Color(0xFFE9EDEF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "$minDelay - $maxDelay ms",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "প্রতিটা action এর মধ্যে random delay (WhatsApp bot detection এড়ায়)",
                        color = Color(0xFF8696A0),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Min: ${minDelay}ms", color = Color.Gray, fontSize = 11.sp)
                    Slider(
                        value = minDelay.toFloat(),
                        onValueChange = { viewModel.updateBypassMinDelay(it.toInt()) },
                        valueRange = 1000f..5000f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text("Max: ${maxDelay}ms", color = Color.Gray, fontSize = 11.sp)
                    Slider(
                        value = maxDelay.toFloat(),
                        onValueChange = { viewModel.updateBypassMaxDelay(it.toInt()) },
                        valueRange = 5000f..15000f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // === Tap Randomness ===
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Tap Randomness (±dp)",
                            color = Color(0xFFE9EDEF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "±$tapRandomness dp",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Tap করার সময় random offset (exact center tap detect করা যায় না)",
                        color = Color(0xFF8696A0),
                        fontSize = 11.sp
                    )
                    Slider(
                        value = tapRandomness.toFloat(),
                        onValueChange = { viewModel.updateBypassTapRandomness(it.toInt()) },
                        valueRange = 0f..20f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // === Toggle: Typing Simulation ===
                BypassToggleRow(
                    icon = Icons.Filled.Speed,
                    title = "Typing Simulation",
                    description = "WhatsApp-এ \"typing...\" indicator দেখায় (char-by-char speed simulation)",
                    checked = typingSim,
                    onCheckedChange = { viewModel.updateBypassTypingSim(it) }
                )

                if (typingSim) {
                    Column {
                        Text(
                            "Typing Speed: $typingSpeed WPM",
                            color = Color(0xFFE9EDEF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = typingSpeed.toFloat(),
                            onValueChange = { viewModel.updateBypassTypingSpeed(it.toInt()) },
                            valueRange = 20f..80f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // === Toggle: Read Before Reply ===
                BypassToggleRow(
                    icon = Icons.Filled.Visibility,
                    title = "Read-Before-Reply Simulation",
                    description = "Reply করার আগে chat পড়ার simulation (instant reply detect করা যায় না)",
                    checked = readBeforeReply,
                    onCheckedChange = { viewModel.updateBypassReadBeforeReply(it) }
                )

                // === Toggle: Random Pauses ===
                BypassToggleRow(
                    icon = Icons.Filled.Refresh,
                    title = "Random Long Pauses",
                    description = "১৫% সম্ভাবনায় ৫-১৫s বড় pause (যেন মানুষ ভাবছে)",
                    checked = randomPauses,
                    onCheckedChange = { viewModel.updateBypassRandomPauses(it) }
                )

                // === Toggle: Night Mode ===
                BypassToggleRow(
                    icon = Icons.Filled.Check,
                    title = "Night Mode Slowdown",
                    description = "রাত ১১টা-সকাল ৬টা পর্যন্ত delay দ্বিগুণ (অস্বাভাবিক activity এড়ায়)",
                    checked = nightMode,
                    onCheckedChange = { viewModel.updateBypassNightMode(it) }
                )

                // === Session Limits ===
                Column {
                    Text(
                        "Session Limit: $sessionLimit messages",
                        color = Color(0xFFE9EDEF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "এক session এ সর্বোচ্চ কতটা message পাঠাবে (rate limit এড়ায়)",
                        color = Color(0xFF8696A0),
                        fontSize = 11.sp
                    )
                    Slider(
                        value = sessionLimit.toFloat(),
                        onValueChange = { viewModel.updateBypassSessionLimit(it.toInt()) },
                        valueRange = 5f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // === Hourly Limit ===
                Column {
                    Text(
                        "Hourly Limit: $maxPerHour messages",
                        color = Color(0xFFE9EDEF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "প্রতি ঘণ্টায় সর্বোচ্চ কতটা (WhatsApp rate limit এর নিচে)",
                        color = Color(0xFF8696A0),
                        fontSize = 11.sp
                    )
                    Slider(
                        value = maxPerHour.toFloat(),
                        onValueChange = { viewModel.updateBypassMaxPerHour(it.toInt()) },
                        valueRange = 10f..60f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // === Reset Button ===
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.resetBypassToDefaults() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reset",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset to Defaults", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                Text(
                    "⚠️ Bypass disabled — WhatsApp সহজেই bot detect করতে পারবে। সতর্কতার সাথে ব্যবহার করুন।",
                    color = Color(0xFFEF5350),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Reusable toggle row for bypass settings.
 */
@Composable
private fun BypassToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (checked) Color(0xFF00A884) else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    title,
                    color = Color(0xFFE9EDEF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    color = Color(0xFF8696A0),
                    fontSize = 11.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00A884),
                checkedTrackColor = Color(0xFF00A884).copy(alpha = 0.3f)
            )
        )
    }
}

// ============================================================================
//  Chat List Scanner Panel
// ============================================================================

/**
 * Chat List Scanner এর UI — পুরো WhatsApp chat list scan করে দেখায়।
 */
@Composable
fun ChatListScannerPanel(viewModel: MainViewModel) {
    val scanListOnly by viewModel.scanListOnly.collectAsState()
    val scannedChats by viewModel.scannedChats.collectAsState()
    val scanStatus by viewModel.scanStatus.collectAsState()
    val totalChats by viewModel.totalChatsFound.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1F2E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // === Header ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = "Scan",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Chat List Scanner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
                Switch(
                    checked = scanListOnly,
                    onCheckedChange = { viewModel.updateScanListOnly(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF53BDEB),
                        checkedTrackColor = Color(0xFF53BDEB).copy(alpha = 0.3f)
                    )
                )
            }

            Text(
                "WhatsApp এর পুরো chat list scan করে এই অ্যাপে দেখায়। " +
                        "Toggle অন করে WhatsApp খুললে স্বয়ংক্রিয়ভাবে scroll করে সব chat collect করবে।",
                color = Color(0xFF8696A0),
                fontSize = 11.sp
            )

            // === Status ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Status: ${scanStatus.name}",
                    color = when (scanStatus) {
                        ChatListScanner.ScanStatus.COMPLETED -> Color(0xFF00A884)
                        ChatListScanner.ScanStatus.SCANNING -> Color(0xFF53BDEB)
                        ChatListScanner.ScanStatus.ERROR -> Color(0xFFEF5350)
                        else -> Color.Gray
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Total: $totalChats",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // === Scanned Chat List ===
            if (scannedChats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(scannedChats) { chat ->
                        ScannedChatRow(chat)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedChatRow(chat: ChatListScanner.ScannedChat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF111B21))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF202C33)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                chat.contactName.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name + last message
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    chat.contactName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                if (chat.isGroup) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("👥", fontSize = 12.sp)
                }
                if (chat.isBusiness) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("🛍️", fontSize = 12.sp)
                }
            }
            Text(
                chat.lastMessage.ifEmpty { "(no preview)" },
                color = Color(0xFF8696A0),
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        // Right side: time + status
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                chat.timestamp,
                color = Color(0xFF8696A0),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Status indicators
            Row {
                if (chat.hasDoubleBlueTick) {
                    Text("✓✓", color = Color(0xFF53BDEB), fontSize = 12.sp)
                } else if (chat.hasDoubleGrayTick) {
                    Text("✓✓", color = Color.Gray, fontSize = 12.sp)
                } else if (chat.hasSingleTick) {
                    Text("✓", color = Color.Gray, fontSize = 12.sp)
                }
                if (chat.isUnread) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            chat.unreadCount.toString(),
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
