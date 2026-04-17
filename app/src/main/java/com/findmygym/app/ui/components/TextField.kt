package com.findmygym.app.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

//Pomocna funkcija za input polja kako bi se lepse video tekst
@Composable
fun textFieldTextStyle(): TextStyle {
    return LocalTextStyle.current.copy(
        color = MaterialTheme.colorScheme.onBackground
    )
}
