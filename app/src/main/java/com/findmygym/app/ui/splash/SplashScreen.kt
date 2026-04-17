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
    //Datastore za remember me opciju
    val rememberStore = remember { RememberMeStore(context) }
    //Koristi se da se proveri da li je korisnik ulogovan
    val repo = remember { AuthRepository() }

    LaunchedEffect(Unit) {
        //Da li je ukljucen remember me
        val rememberMe = rememberStore.rememberMeFlow.first()

        //Ako nije bio ukljucen remember me, prvo ide logout
        if (!rememberMe) repo.logout()

        //Ako jeste ukljucen i korisnik je ulogovan ide na mapu, u suprotnom na login
        if (rememberMe && repo.isLoggedIn()) onGoMap() else onGoLogin()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        //Loading indikator
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
