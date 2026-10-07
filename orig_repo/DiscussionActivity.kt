package com.example.skilldiscovery

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.skilldiscovery.ui.theme.SkillDiscoveryTheme

class DiscussionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkillDiscoveryTheme {
                var showAddPoll by remember { mutableStateOf(false) }
                var showAddPost by remember { mutableStateOf(false) }
                var showFilters by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }
                var selectedTags by remember { mutableStateOf(setOf<String>()) }
                var selectedVis by remember { mutableStateOf(setOf<String>()) }

                val discussionsList = remember {
                    mutableStateListOf(
                        DiscussionItem(1, "Arun", "@arun_dev", "Hackathon Team", "Looking for ML dev.", listOf("AI", "Projects"), "@all", "High", 45, "2h ago"),
                        DiscussionItem(2, "Priya", "@priya_f", "Flutter State", "Should we adopt Riverpod?", listOf("Projects", "Help"), "@all", "Normal", 12, "4h ago"),
                        DiscussionItem(4, "Vinay", "@vinayk", "Revision Poll", "Vote for session time.", listOf("Events", "Poll"), "@all", "Normal", 31, "30m ago",
                            poll = DiscussionPoll("Study window", listOf(DiscussionPollOption("6-7 PM", 14), DiscussionPollOption("7-8 PM", 9), DiscussionPollOption("8-9 PM", 5))))
                    )
                }

                DiscoveryScaffold(
                    selectedTab = MainTab.DISCUSS,
                    onAddPollClick = { showAddPoll = true },
                    onFilterDiscussionClick = { showFilters = true }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                                Button(onClick = { showAddPost = true }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.secondary), border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)) {
                                    Text(text = "+ Post Discussion", fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            DiscussionPanel(
                                discussions = discussionsList,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                selectedTags = selectedTags,
                                onTagToggle = { tag -> selectedTags = if (selectedTags.contains(tag)) selectedTags - tag else selectedTags + tag },
                                onUpvote = { i -> discussionsList[i] = discussionsList[i].copy(votes = discussionsList[i].votes + 1, upvoted = true, downvoted = false) },
                                onDownvote = { i -> discussionsList[i] = discussionsList[i].copy(votes = discussionsList[i].votes - 1, downvoted = true, upvoted = false) },
                                onPollVote = { discussionId, optionIndex ->
                                    val index = discussionsList.indexOfFirst { it.id == discussionId }
                                    if (index != -1) {
                                        val discussion = discussionsList[index]
                                        discussion.poll?.let { poll ->
                                            val oldIndex = poll.selectedOptionIndex
                                            if (oldIndex == optionIndex) return@let // Already selected

                                            val newOptions = poll.options.mapIndexed { i, opt ->
                                                when (i) {
                                                    optionIndex -> opt.copy(votes = opt.votes + 1)
                                                    oldIndex -> opt.copy(votes = opt.votes - 1)
                                                    else -> opt
                                                }
                                            }
                                            discussionsList[index] = discussion.copy(
                                                poll = poll.copy(options = newOptions, selectedOptionIndex = optionIndex)
                                            )
                                        }
                                    }
                                },
                                filterVisibility = selectedVis
                            )
                        }

                        if (showAddPoll) AddPollDialog(onDismiss = { showAddPoll = false }, onAdd = { discussionsList.add(0, it) })
                        if (showAddPost) AddDiscussionDialog(onDismiss = { showAddPost = false }, onAdd = { discussionsList.add(0, it) })
                        if (showFilters) DiscussionFilterDialog(selectedTags = selectedTags, selectedVisibility = selectedVis, onDismiss = { showFilters = false }, onApply = { t, v -> selectedTags = t; selectedVis = v; showFilters = false })
                    }
                }
            }
        }
    }
}

@Composable
fun DiscussionPanel(
    discussions: List<DiscussionItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit,
    onUpvote: (Int) -> Unit,
    onDownvote: (Int) -> Unit,
    onPollVote: (Int, Int) -> Unit,
    filterVisibility: Set<String>
) {
    val quickTags = listOf("AI", "Projects", "Help", "Events")
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(value = searchQuery, onValueChange = onSearchQueryChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search discussions...") }, shape = RoundedCornerShape(12.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickTags.forEach { tag -> CategoryChip(label = "[$tag]", selected = selectedTags.contains(tag), onClick = { onTagToggle(tag) }) }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(discussions) { item ->
                DiscussionCard(
                    item = item,
                    onUpvoteClick = { onUpvote(discussions.indexOf(item)) },
                    onDownvoteClick = { onDownvote(discussions.indexOf(item)) },
                    onPollVote = { optionIndex -> onPollVote(item.id, optionIndex) }
                )
            }
        }
    }
}

@Composable
fun DiscussionCard(
    item: DiscussionItem,
    onUpvoteClick: () -> Unit,
    onDownvoteClick: () -> Unit,
    onPollVote: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) { Text(text = item.visibility, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary) }
                Text(text = item.timeAgo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = item.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = item.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            item.poll?.let { poll ->
                DiscussionPollSection(
                    poll = poll,
                    onVote = onPollVote
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { item.tags.forEach { Text(text = "#$it", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall) } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(modifier = Modifier.clickable { onUpvoteClick() }, shape = RoundedCornerShape(12.dp), color = if (item.upvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, if (item.upvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "▲", fontSize = 10.sp, color = if (item.upvoted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${item.votes}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (item.upvoted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Surface(modifier = Modifier.clickable { onDownvoteClick() }, shape = RoundedCornerShape(12.dp), color = if (item.downvoted) Color.Red.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, if (item.downvoted) Color.Red else MaterialTheme.colorScheme.outline)) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Text(text = "▼", fontSize = 10.sp, color = if (item.downvoted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscussionPollSection(poll: DiscussionPoll, onVote: (Int) -> Unit) {
    val totalVotes = poll.options.sumOf { it.votes }.coerceAtLeast(1)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = poll.question, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            poll.options.forEachIndexed { index, option ->
                val ratio = option.votes.toFloat() / totalVotes.toFloat()
                val isSelected = poll.selectedOptionIndex == index
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onVote(index) }
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Text(text = "✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(text = "${(ratio * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddPollDialog(onDismiss: () -> Unit, onAdd: (DiscussionItem) -> Unit) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") } // Dynamic list starting with 2 empty options

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(14.dp), shape = RoundedCornerShape(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(22.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Create Poll", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                
                OutlinedTextField(
                    value = question, 
                    onValueChange = { question = it }, 
                    modifier = Modifier.fillMaxWidth(), 
                    placeholder = { Text("Question") }, 
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Options", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = option,
                            onValueChange = { options[index] = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Option ${index + 1}") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        
                        if (options.size > 2) {
                            Surface(
                                modifier = Modifier.clickable { options.removeAt(index) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "✕",
                                    modifier = Modifier.padding(8.dp),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (options.size < 6) {
                    Button(
                        onClick = { options.add("") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("+ Add Option", fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            val validOptions = options.filter { it.isNotBlank() }
                            if (question.isNotBlank() && validOptions.size >= 2) { 
                                onAdd(DiscussionItem(
                                    id = System.currentTimeMillis().toInt(), 
                                    author = "You", 
                                    handle = "@you", 
                                    title = question, 
                                    content = "Poll", 
                                    tags = listOf("Poll"), 
                                    visibility = "@all", 
                                    priority = "Normal", 
                                    votes = 0, 
                                    timeAgo = "Now", 
                                    poll = DiscussionPoll(question, validOptions.map { DiscussionPollOption(it, 0) })
                                ))
                                onDismiss() 
                            } 
                        }, 
                        shape = RoundedCornerShape(14.dp)
                    ) { 
                        Text("ADD POLL", fontWeight = FontWeight.Bold) 
                    }

                    Button(
                        modifier = Modifier.weight(0.7f),
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun AddDiscussionDialog(onDismiss: () -> Unit, onAdd: (DiscussionItem) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(14.dp), shape = RoundedCornerShape(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(22.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Post Discussion", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Title") }, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = content, onValueChange = { content = it }, modifier = Modifier.fillMaxWidth().height(120.dp), placeholder = { Text("Content") }, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = tags, onValueChange = { tags = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Tags (comma separated)") }, shape = RoundedCornerShape(12.dp))
                Button(onClick = { if (title.isNotBlank()) { onAdd(DiscussionItem(0, "You", "@you", title, content, tags.split(",").map { it.trim() }, "@all", "Normal", 0, "Now")); onDismiss() } }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) { Text("POST") }
            }
        }
    }
}

@Composable
fun DiscussionFilterDialog(selectedTags: Set<String>, selectedVisibility: Set<String>, onDismiss: () -> Unit, onApply: (Set<String>, Set<String>) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(14.dp), shape = RoundedCornerShape(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Filter Discussions", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Button(onClick = { onApply(selectedTags, selectedVisibility) }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) { Text("APPLY FILTERS") }
            }
        }
    }
}
