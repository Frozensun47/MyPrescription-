package com.example.myprescription.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PinInput(
    pin: String,
    onPinChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    pinLength: Int = 4
) {
    val focusRequester = remember { FocusRequester() }

    BasicTextField(
        value = pin,
        onValueChange = {
            if (it.length <= pinLength && it.all { char -> char.isDigit() }) {
                onPinChange(it)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier.focusRequester(focusRequester),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(pinLength) { index ->
                    val char = when {
                        index < pin.length -> "●"
                        else -> ""
                    }
                    val hasFocus = pin.length == index
                    val boxColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                    val borderColor = when {
                        isError -> MaterialTheme.colorScheme.error
                        hasFocus -> MaterialTheme.colorScheme.primary
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(boxColor, shape = MaterialTheme.shapes.medium)
                            .border(
                                width = 1.dp,
                                color = borderColor,
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )

    // Request focus when the component enters the composition.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}