package com.example.ui.discussion

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DiscussionItem
import com.example.model.DiscussionPoll
import com.example.model.DiscussionPollOption
import com.example.ui.components.CategoryChip

@Composable
fun DiscussionScreen(
    discussions: List<DiscussionItem>,
    showAddPoll: Boolean,
    showFilters: Boolean,
    onDismissAddPoll: () -> Unit,
    onDismissFilters: () -> Unit,
    onAddDiscussion: (DiscussionItem) -> Unit,
    onUpvote: (DiscussionItem) -> Unit,
    onDownvote: (DiscussionItem) -> Unit,
    onPollVote: (Int, Int) -> Unit,
    onClearPollVote: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddPost by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var selectedVis by remember { mutableStateOf(setOf<String>()) }

    val filteredDiscussions = remember(discussions, searchQuery, selectedTags) {
        discussions.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.content.contains(searchQuery, ignoreCase = true)
            val matchesTags = selectedTags.isEmpty() ||
                item.tags.any { it in selectedTags }
            matchesQuery && matchesTags
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { showAddPost = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Text(text = "+ Post Discussion", fontWeight = FontWeight.Bold)
                }
            }

            DiscussionPanel(
                discussions = filteredDiscussions,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedTags = selectedTags,
                onTagToggle = { tag ->
                    selectedTags = if (selectedTags.contains(tag)) selectedTags - tag else selectedTags + tag
                },
                onUpvote = onUpvote,
                onDownvote = onDownvote,
                onPollVote = onPollVote,
                onClearPollVote = onClearPollVote
            )
        }

        if (showAddPoll) {
            AddPollDialog(
                onDismiss = onDismissAddPoll,
                onAdd = {
                    onAddDiscussion(it)
                    onDismissAddPoll()
                }
            )
        }
        if (showAddPost) {
            AddDiscussionDialog(
                onDismiss = { showAddPost = false },
                onAdd = {
                    onAddDiscussion(it)
                    showAddPost = false
                }
            )
        }
        if (showFilters) {
            DiscussionFilterDialog(
                selectedTags = selectedTags,
                selectedVisibility = selectedVis,
                onDismiss = onDismissFilters,
                onApply = { t, v ->
                    selectedTags = t
                    selectedVis = v
                    onDismissFilters()
                }
            )
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
    onUpvote: (DiscussionItem) -> Unit,
    onDownvote: (DiscussionItem) -> Unit,
    onPollVote: (Int, Int) -> Unit,
    onClearPollVote: (Int) -> Unit
) {
    val quickTags = listOf("AI", "Projects", "Help", "Events")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search discussions...") },
            shape = RoundedCornerShape(12.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickTags.forEach { tag ->
                CategoryChip(
                    label = "[$tag]",
                    selected = selectedTags.contains(tag),
                    onClick = { onTagToggle(tag) }
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = discussions,
                key = { item ->
                    val pollVotes = item.poll?.options?.joinToString("-") { "${it.label}:${it.votes}" }.orEmpty()
                    "${item.id}_${item.votes}_${item.upvoted}_${item.downvoted}_${item.poll?.selectedOptionIndex}_$pollVotes"
                }
            ) { item ->
                DiscussionCard(
                    item = item,
                    onUpvoteClick = { onUpvote(item) },
                    onDownvoteClick = { onDownvote(item) },
                    onPollVote = { optionIndex -> onPollVote(item.id, optionIndex) },
                    onClearPollVote = { onClearPollVote(item.id) }
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
    onPollVote: (Int) -> Unit,
    onClearPollVote: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = item.visibility,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = item.timeAgo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            item.poll?.let { poll ->
                DiscussionPollSection(
                    poll = poll,
                    onVote = onPollVote,
                    onClearVote = onClearPollVote
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.tags.forEach {
                        Text(
                            text = "#$it",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.clickable { onUpvoteClick() },
                        shape = RoundedCornerShape(12.dp),
                        color = if (item.upvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (item.upvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "▲",
                                fontSize = 10.sp,
                                color = if (item.upvoted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${item.votes}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (item.upvoted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.clickable { onDownvoteClick() },
                        shape = RoundedCornerShape(12.dp),
                        color = if (item.downvoted) Color.Red.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (item.downvoted) Color.Red else MaterialTheme.colorScheme.outline)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "▼",
                                fontSize = 10.sp,
                                color = if (item.downvoted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscussionPollSection(poll: DiscussionPoll, onVote: (Int) -> Unit, onClearVote: () -> Unit) {
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
                val ratio = (option.votes.toFloat() / totalVotes.toFloat()).coerceIn(0f, 1f)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        )
                    }
                }
            }
            if (poll.selectedOptionIndex != null) {
                Text(
                    text = "Clear my vote",
                    modifier = Modifier.clickable { onClearVote() },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun AddPollDialog(onDismiss: () -> Unit, onAdd: (DiscussionItem) -> Unit) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                                onAdd(
                                    DiscussionItem(
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
                                    )
                                )
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Post Discussion", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Title") },
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("Content") },
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Tags (comma separated)") },
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAdd(
                                DiscussionItem(
                                    id = System.currentTimeMillis().toInt(),
                                    author = "You",
                                    handle = "@you",
                                    title = title,
                                    content = content,
                                    tags = if (tags.isBlank()) listOf("General") else tags.split(",").map { it.trim() },
                                    visibility = "@all",
                                    priority = "Normal",
                                    votes = 0,
                                    timeAgo = "Now"
                                )
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("POST", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DiscussionFilterDialog(
    selectedTags: Set<String>,
    selectedVisibility: Set<String>,
    onDismiss: () -> Unit,
    onApply: (Set<String>, Set<String>) -> Unit
) {
    val allTags = listOf("AI", "Projects", "Help", "Events", "Poll")
    var tempTags by remember { mutableStateOf(selectedTags) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Filter Discussions", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                Text("Filter by Tag:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTags.forEach { tag ->
                        CategoryChip(
                            label = tag,
                            selected = tempTags.contains(tag),
                            onClick = {
                                tempTags = if (tempTags.contains(tag)) tempTags - tag else tempTags + tag
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onApply(tempTags, selectedVisibility) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("APPLY FILTERS", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            tempTags = emptySet()
                            onApply(emptySet(), selectedVisibility)
                        },
                        modifier = Modifier
                            .weight(0.7f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Reset", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
