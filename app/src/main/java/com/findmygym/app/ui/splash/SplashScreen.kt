package com.findmygym.app.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.findmygym.app.data.auth.AuthRepository
import com.findmygym.app.data.auth.RememberMeStore
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(
    onGoLogin: () -> Unit,
    onGoMap: () -> Unit
) {
    val context = LocalContext.current
    val rememberStore = remember { RememberMeStore(context) }
    val repo = remember { AuthRepository() }

    LaunchedEffect(Unit) {
        val rememberMe = rememberStore.rememberMeFlow.first()

        if (!rememberMe) repo.logout()

        if (rememberMe && repo.isLoggedIn()) onGoMap() else onGoLogin()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
