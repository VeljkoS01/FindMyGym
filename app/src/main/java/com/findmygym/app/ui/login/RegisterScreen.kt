package com.findmygym.app.ui.login

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.findmygym.app.util.ImageBase64

@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit,
    onGoMap: () -> Unit
) {
    val vm: AuthViewModel = viewModel()
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var pickedImagePreview by remember { mutableStateOf<Any?>(null) }
    var photoBase64 by remember { mutableStateOf<String?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pickedImagePreview = uri
            photoBase64 = ImageBase64.uriToBase64Jpeg(context, uri, quality = 70)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            pickedImagePreview = bmp
            photoBase64 = ImageBase64.bitmapToBase64Jpeg(bmp, quality = 70)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create account", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(password, { password = it }, label = { Text("Password (min 6)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(fullName, { fullName = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        pickedImagePreview?.let {
            AsyncImage(model = it, contentDescription = "Profile photo", modifier = Modifier.fillMaxWidth().height(180.dp))
            Spacer(Modifier.height(10.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { pickLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) { Text("Gallery") }

            OutlinedButton(
                onClick = { cameraLauncher.launch(null) },
                modifier = Modifier.weight(1f)
            ) { Text("Camera") }
        }

        vm.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                vm.register(username, password, fullName, phone, photoBase64) { onGoMap() }
            },
            enabled = !vm.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vm.loading) "Creating..." else "Create account")
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}