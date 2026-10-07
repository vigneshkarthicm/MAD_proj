package com.example.skilldiscovery

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.skilldiscovery.ui.theme.SkillDiscoveryTheme

class AchievementsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkillDiscoveryTheme {
                var showAddAchievement by remember { mutableStateOf(false) }
                val achievementsList = remember {
                    mutableStateListOf(
                        AchievementItem("Machine Learning Certificate", "12 July 2026", "Certification", "Python, Deep Learning", "Completed CNN training and optimization on Edge TPU.", "android.resource://com.example.skillshare/drawable/ml_cert_placeholder"),
                        AchievementItem("College Event Android App", "05 June 2026", "Project", "Flutter, Dart", "Published the college companion app. Managed global states using Clean Architecture.", "android.resource://com.example.skillshare/drawable/android_app_placeholder"),
                        AchievementItem("Smart IoT Irrigation System", "20 May 2026", "Project", "ESP32, MicroPython, IoT", "Automated farm watering using soil moisture feedback loops.", "android.resource://com.example.skillshare/drawable/iot_irrigation_placeholder")
                    )
                }

                DiscoveryScaffold(
                    selectedTab = MainTab.ACHIEVE,
                    onAddAchievementClick = { showAddAchievement = true }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AchievementsPanel(achievements = achievementsList)
                        
                        if (showAddAchievement) {
                            AddAchievementDialog(
                                onDismiss = { showAddAchievement = false },
                                onAdd = { newAch ->
                                    achievementsList.add(0, newAch)
                                    showAddAchievement = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementsPanel(achievements: List<AchievementItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }
        items(achievements) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(text = item.category, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(text = "Posted: ${item.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.skills.split(",").forEach { skill ->
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(text = skill.trim(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Text(text = item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!item.imageUri.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = item.imageUri,
                            contentDescription = "Achievement Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddAchievementDialog(onDismiss: () -> Unit, onAdd: (AchievementItem) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Project") }
    var skills by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("23 July 2026") }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    val categories = listOf("Certification", "Project", "Competition", "Research", "Learning")
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(22.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Post Achievement", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Title", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                categories.take(3).forEach { cat ->
                                    CategoryChip(label = cat, selected = selectedCategory == cat, onClick = { selectedCategory = cat })
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                categories.drop(3).forEach { cat ->
                                    CategoryChip(label = cat, selected = selectedCategory == cat, onClick = { selectedCategory = cat })
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Skills (comma separated)", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(value = skills, onValueChange = { skills = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Description", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(12.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Attachment Image", style = MaterialTheme.typography.labelMedium)
                    if (selectedImageUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Image Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Text(
                                    text = "✕",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Photo", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onAdd(
                                    AchievementItem(
                                        title = title,
                                        date = date,
                                        category = selectedCategory,
                                        skills = if (skills.isBlank()) "General" else skills,
                                        description = desc,
                                        imageUri = selectedImageUri?.toString()
                                    )
                                )
                                Toast.makeText(context, "Achievement Posted!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.2f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("POST", fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onDismiss, modifier = Modifier.weight(0.8f).height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
