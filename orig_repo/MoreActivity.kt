package com.example.skilldiscovery

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.skilldiscovery.ui.theme.SkillDiscoveryTheme

class MoreActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkillDiscoveryTheme {
                DiscoveryScaffold(selectedTab = MainTab.MORE) { innerPadding ->
                    MoreActivityContent(innerPadding)
                }
            }
        }
    }
}

@Composable
fun MoreActivityContent(innerPadding: androidx.compose.foundation.layout.PaddingValues) {
    val context = LocalContext.current
    val classCode = "CSE2026"

    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "Cohort Metadata", fontWeight = FontWeight.Bold)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                MoreInfoRow(label = "Class Code ID", value = classCode)
                MoreInfoRow(label = "Connected Server", value = "AWS US-East")
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))) {
            Column(modifier = Modifier.padding(8.dp)) {
                 MoreMenuButton(label = "Profile Settings", onClick = { })
                 MoreMenuButton(label = "Notifications", onClick = { })
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                context.startActivity(Intent(context, AuthActivity::class.java))
                (context as? ComponentActivity)?.finishAffinity()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
            border = BorderStroke(1.dp, Color.Red)
        ) {
            Text(text = "LOGOUT ACCOUNT", fontWeight = FontWeight.Bold)
        }
    }
}
