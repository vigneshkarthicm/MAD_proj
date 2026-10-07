package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.local.SkillDiscoveryDatabase
import com.example.data.remote.NetworkMonitor
import com.example.data.repository.SessionManager
import com.example.data.repository.SkillDiscoveryRepository
import com.example.ui.SkillDiscoveryApp
import com.example.ui.theme.SkillDiscoveryTheme
import com.example.ui.viewmodel.SkillDiscoveryViewModel
import com.example.ui.viewmodel.SkillDiscoveryViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: SkillDiscoveryViewModel by viewModels {
        val database = SkillDiscoveryDatabase.getDatabase(applicationContext)
        val sessionManager = SessionManager(applicationContext)
        val networkMonitor = NetworkMonitor(applicationContext)
        val repository = SkillDiscoveryRepository(database, sessionManager, networkMonitor)
        SkillDiscoveryViewModelFactory(repository, sessionManager, networkMonitor)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkillDiscoveryTheme {
                SkillDiscoveryApp(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
