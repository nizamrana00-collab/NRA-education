package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.auth.AuthManager
import com.example.data.database.AppDatabase
import com.example.data.repository.TuitionRepository
import com.example.ui.MainNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TuitionViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val repository = TuitionRepository(db, applicationContext)

        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TuitionViewModel(repository) as T
            }
        }
        val viewModel = ViewModelProvider(this, viewModelFactory)[TuitionViewModel::class.java]

        // Silent background sign-in check on app start
        AuthManager.attemptAutoSignIn(applicationContext, lifecycleScope) { user ->
            viewModel.syncFromCloud()
        }

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainNavigation(viewModel = viewModel)
                }
            }
        }
    }
}
