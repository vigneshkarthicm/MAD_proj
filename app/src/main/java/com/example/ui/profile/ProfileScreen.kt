package com.example.ui.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.ClassmateItem
import com.example.ui.components.ConsistencyChart
import com.example.ui.components.MonthlyImprovementChart
import com.example.ui.components.SkillProgressBar
import com.example.ui.components.SkillRadarChart
import com.example.ui.viewmodel.SkillGraphLink

@Composable
fun ProfileScreen(
    classmates: List<ClassmateItem>,
    modifier: Modifier = Modifier,
    studentName: String = "Vinay Kumar",
    studentRole: String = "CSE - III Year",
    contactNumber: String = "9876543210",
    overallLevel: Int = 82,
    projectCount: Int = 12,
    achievementCount: Int = 15,
    technicalSkills: List<Pair<String, Int>> = listOf(
        "Python Backend" to 80,
        "Flutter Apps" to 70,
        "Machine Learning" to 60
    ),
    radarMetrics: Map<String, Float> = mapOf(
        "AI" to 0.6f,
        "Coding" to 0.85f,
        "Comm" to 0.75f,
        "Lead" to 0.7f,
        "Proj" to 0.9f
    ),
    weeklyActivity: List<Pair<String, Int>> = listOf(
        "Mon" to 4,
        "Tue" to 5,
        "Wed" to 3,
        "Thu" to 5,
        "Fri" to 4
    ),
    skillGraph: List<SkillGraphLink> = emptyList(),
    weeklyConsistency: Int = 0
) {
    var isProfileExpanded by remember { mutableStateOf(false) }
    var selectedPeer by remember { mutableStateOf<ClassmateItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        ProfilePanel(
            isExpanded = isProfileExpanded,
            onToggleExpand = { isProfileExpanded = !isProfileExpanded },
            classmates = classmates,
            onPeerClick = { selectedPeer = it },
            studentName = studentName,
            studentRole = studentRole,
            contactNumber = contactNumber,
            overallLevel = overallLevel,
            projectCount = projectCount,
            achievementCount = achievementCount,
            technicalSkills = technicalSkills,
            radarMetrics = radarMetrics,
            weeklyActivity = weeklyActivity,
            skillGraph = skillGraph,
            weeklyConsistency = weeklyConsistency
        )
        selectedPeer?.let { peer ->
            PeerDetailsDialog(peer = peer, onDismiss = { selectedPeer = null })
        }
    }
}

@Composable
fun ProfilePanel(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    classmates: List<ClassmateItem>,
    onPeerClick: (ClassmateItem) -> Unit,
    studentName: String = "Vinay Kumar",
    studentRole: String = "CSE - III Year",
    contactNumber: String = "9876543210",
    overallLevel: Int = 82,
    projectCount: Int = 12,
    achievementCount: Int = 15,
    technicalSkills: List<Pair<String, Int>> = listOf(
        "Python Backend" to 80,
        "Flutter Apps" to 70,
        "Machine Learning" to 60
    ),
    radarMetrics: Map<String, Float> = mapOf(
        "AI" to 0.6f,
        "Coding" to 0.85f,
        "Comm" to 0.75f,
        "Lead" to 0.7f,
        "Proj" to 0.9f
    ),
    weeklyActivity: List<Pair<String, Int>> = listOf(
        "Mon" to 4,
        "Tue" to 5,
        "Wed" to 3,
        "Thu" to 5,
        "Fri" to 4
    ),
    skillGraph: List<SkillGraphLink> = emptyList(),
    weeklyConsistency: Int = 0
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val initials = studentName.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifEmpty { "ST" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() },
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = studentName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = studentRole,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contact: $contactNumber",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(contactNumber))
                            Toast.makeText(context, "Contact $contactNumber copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Contact", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Overall Skill Level", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "$overallLevel%",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Projects", style = MaterialTheme.typography.labelSmall)
                            Text(text = "$projectCount", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Achievements", style = MaterialTheme.typography.labelSmall)
                            Text(text = "$achievementCount", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Text(text = "Technical Skill Metrics", fontWeight = FontWeight.Bold)
                        technicalSkills.forEach { skill ->
                            SkillProgressBar(label = skill.first, level = skill.second)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Text(text = "Skill Distribution Radar", fontWeight = FontWeight.Bold)
                        SkillRadarChart(skills = radarMetrics)

                        if (skillGraph.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Text(text = "Backend Skill Graph", fontWeight = FontWeight.Bold)
                            skillGraph.forEach { link ->
                                Text(
                                    text = "${link.source} -> ${link.target} (${link.weight})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Text(text = "Skill Growth (Jan - Apr)", fontWeight = FontWeight.Bold)
                        MonthlyImprovementChart(
                            data = listOf(
                                "Jan" to 40,
                                "Feb" to 55,
                                "Mar" to 70,
                                "Apr" to 85
                            )
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Text(text = "Weekly Activity Consistency", fontWeight = FontWeight.Bold)
                        Text(
                            text = "$weeklyConsistency active day(s) this week",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ConsistencyChart(
                            data = weeklyActivity.ifEmpty { listOf("No activity" to 0) }
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Class Members",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            classmates.forEach { peer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPeerClick(peer) },
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = peer.imageChar,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = peer.name, fontWeight = FontWeight.Bold)
                            Text(
                                text = peer.role,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${peer.skillLevel}%",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PeerDetailsDialog(peer: ClassmateItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showStats by remember { mutableStateOf(true) }

    val progressLabels = peer.skills.take(3).ifEmpty { listOf("Core", "Problem Solving", "Delivery") }
    val progressValues = progressLabels.mapIndexed { index, _ ->
        (peer.skillLevel - index * 10).coerceIn(35, 98)
    }
    val radarSkills = mapOf(
        progressLabels.getOrElse(0) { "Core" } to (peer.skillLevel / 100f).coerceIn(0.35f, 0.98f),
        progressLabels.getOrElse(1) { "Growth" } to ((peer.skillLevel - 6) / 100f).coerceIn(0.30f, 0.96f),
        progressLabels.getOrElse(2) { "Consistency" } to ((peer.skillLevel - 12) / 100f).coerceIn(0.25f, 0.94f),
        "Collab" to ((peer.skillLevel + 4) / 100f).coerceIn(0.30f, 0.98f),
        "Focus" to ((peer.skillLevel - 2) / 100f).coerceIn(0.30f, 0.98f)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = peer.imageChar,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Text(
                    text = peer.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                SkillProgressBar(label = "Verified Proficiency", level = peer.skillLevel)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Primary Competencies", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        peer.skills.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { showStats = !showStats },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (showStats) "Hide Member Stats" else "Show Member Stats",
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedVisibility(visible = showStats) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Text(text = "Skill Snapshot", fontWeight = FontWeight.Bold)
                        progressLabels.zip(progressValues).forEach { (label, level) ->
                            SkillProgressBar(label = label, level = level)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        SkillRadarChart(skills = radarSkills)

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Text(text = "Activity Consistency", fontWeight = FontWeight.Bold)
                        ConsistencyChart(data = listOf("Mon" to 3, "Tue" to 4, "Wed" to 5, "Thu" to 2, "Fri" to 5))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val peerContact = remember(peer.name) {
                        (kotlin.math.abs(peer.name.hashCode()) % 899999999 + 1000000000L).toString()
                    }
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(peerContact))
                            Toast.makeText(context, "Contact $peerContact copied to clipboard!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Contact ($peerContact)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Close", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
