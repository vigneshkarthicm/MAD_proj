package com.example.skilldiscovery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.skilldiscovery.ui.theme.SkillDiscoveryTheme

class ExploreActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkillDiscoveryTheme {
                var searchQuery by remember { mutableStateOf("") }
                var selectedYears by remember { mutableStateOf(setOf<String>()) }
                var selectedPeer by remember { mutableStateOf<ClassmateItem?>(null) }
                
                val classmatesList = listOf(
                    ClassmateItem("Arun", "ML Engineer", 75, listOf("Python", "Deep Learning", "TensorFlow"), "3rd", "A"),
                    ClassmateItem("Priya", "Flutter Developer", 80, listOf("Flutter", "Dart", "Firebase"), "3rd", "P"),
                    ClassmateItem("Rahul", "IoT Developer", 70, listOf("ESP32", "Python", "Embedded C"), "3rd", "R"),
                    ClassmateItem("Kiran", "UI/UX Designer", 85, listOf("Figma", "Design Systems", "Prototyping"), "2nd", "K"),
                    ClassmateItem("Meera", "Software Engineer", 68, listOf("Solidity", "Web3", "Ethereum"), "4th", "M")
                )

                DiscoveryScaffold(selectedTab = MainTab.EXPLORE) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        ExplorePanel(
                            classmates = classmatesList,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedYears = selectedYears,
                            onYearToggle = { yr -> selectedYears = if (selectedYears.contains(yr)) selectedYears - yr else selectedYears + yr },
                            onStudentClick = { selectedPeer = it }
                        )
                        selectedPeer?.let { peer ->
                            PeerDetailsDialog(peer = peer, onDismiss = { selectedPeer = null })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExplorePanel(classmates: List<ClassmateItem>, searchQuery: String, onSearchQueryChange: (String) -> Unit, selectedYears: Set<String>, onYearToggle: (String) -> Unit, onStudentClick: (ClassmateItem) -> Unit) {
    val yearList = listOf("2nd", "3rd", "4th")
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = searchQuery, onValueChange = onSearchQueryChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search skills...") }, shape = RoundedCornerShape(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Academic Year:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                yearList.forEach { yr -> CategoryChip(label = yr, selected = selectedYears.contains(yr), onClick = { onYearToggle(yr) }) }
            }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(classmates) { peer ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onStudentClick(peer) }, colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Text(text = peer.imageChar, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = peer.name, fontWeight = FontWeight.Bold)
                            Text(text = "${peer.role} • ${peer.year} Year", style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) { peer.skills.take(3).forEach { Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Text(text = it, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall) } } }
                        }
                        Text(text = "${peer.skillLevel}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
