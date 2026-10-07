package com.example.ui.more

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.CategoryChip
import com.example.ui.components.MoreInfoRow
import com.example.ui.components.MoreMenuButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MoreScreen(
    classCode: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    apiBaseUrl: String = "https://api.skilldiscovery.edu/v1",
    isSandboxBackend: Boolean = true,
    isOnline: Boolean = true,
    lastSyncTime: Long = System.currentTimeMillis(),
    isLoading: Boolean = false,
    currentUserId: String = "std_vinay",
    onUpdateServerSettings: (String, Boolean) -> Unit = { _, _ -> },
    onSyncNow: () -> Unit = {},
    onSwitchProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var inputUrl by remember(apiBaseUrl) { mutableStateOf(apiBaseUrl) }
    var sandboxMode by remember(isSandboxBackend) { mutableStateOf(isSandboxBackend) }

    val formattedSyncTime = remember(lastSyncTime) {
        val sdf = SimpleDateFormat("HH:mm:ss - dd MMM", Locale.getDefault())
        sdf.format(Date(lastSyncTime))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cohort Metadata Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "Cohort Metadata", fontWeight = FontWeight.Bold)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                MoreInfoRow(label = "Class Code ID", value = classCode)
                MoreInfoRow(label = "Network Status", value = if (isOnline) "Connected (Online)" else "Offline Mode")
                MoreInfoRow(label = "Backend Mode", value = if (sandboxMode) "Sandbox Engine" else "Live Remote Server")
                MoreInfoRow(label = "Last Synced", value = formattedSyncTime)
            }
        }

        // Backend Integration & Live URL Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "Backend Server Integration", fontWeight = FontWeight.Bold)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Use Sandbox Backend", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (sandboxMode) "Simulating dynamic backend endpoints locally" else "Connecting to live cloud REST API",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = sandboxMode,
                        onCheckedChange = {
                            sandboxMode = it
                            onUpdateServerSettings(inputUrl, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    )
                }

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text("API Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onUpdateServerSettings(inputUrl, sandboxMode)
                            Toast.makeText(context, "Server URL saved", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Config")
                    }

                    Button(
                        onClick = onSyncNow,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Sync Now")
                        }
                    }
                }
            }
        }

        // Switch Backend Data Payload (Demonstrating Content Changing Based On Backend Input)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Backend Payload Simulator", fontWeight = FontWeight.Bold)
                Text(
                    text = "Select a student to simulate distinct backend payloads. The profile, skill metrics, radar chart, and achievements will instantly adapt to the incoming backend data:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(
                        label = "Vinay (82%)",
                        selected = currentUserId == "std_vinay",
                        onClick = { onSwitchProfile("std_vinay") }
                    )
                    CategoryChip(
                        label = "Alex (92%)",
                        selected = currentUserId == "std_alex",
                        onClick = { onSwitchProfile("std_alex") }
                    )
                    CategoryChip(
                        label = "Elena (88%)",
                        selected = currentUserId == "std_elena",
                        onClick = { onSwitchProfile("std_elena") }
                    )
                }
            }
        }

        // Settings Menu Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                MoreMenuButton(
                    label = "Profile Settings",
                    onClick = { Toast.makeText(context, "Profile Settings - Preferences Synced", Toast.LENGTH_SHORT).show() }
                )
                MoreMenuButton(
                    label = "Notifications",
                    onClick = { Toast.makeText(context, "Notifications enabled for $classCode", Toast.LENGTH_SHORT).show() }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red.copy(alpha = 0.1f),
                contentColor = Color.Red
            ),
            border = BorderStroke(1.dp, Color.Red)
        ) {
            Text(text = "LOGOUT ACCOUNT", fontWeight = FontWeight.Bold)
        }
    }
}
