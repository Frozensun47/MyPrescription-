package com.example.myprescription.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A stable, reusable, and crash-free PIN input component that handles input via a key event listener.
 *
 * @param pin The current value of the pin.
 * @param onPinChange Lambda to be invoked when the pin value changes.
 * @param isError Whether the component should be displayed in an error state.
 * @param modifier The modifier to be applied to the component.
 * @param pinLength The total length of the PIN.
 */
@Composable
fun PinInput(
    pin: String,
    onPinChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    pinLength: Int = 4
) {
    val focusRequester = remember { FocusRequester() }

    // This is the core of the crash-free implementation.
    // It makes the component itself focusable and listens for raw key events,
    // completely bypassing the problematic TextField focus system.
    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) {
                    val lastPin = pin
                    val newPin = when (event.key) {
                        Key.Backspace, Key.Delete -> pin.dropLast(1)
                        Key.Zero, Key.NumPad0 -> pin + "0"
                        Key.One, Key.NumPad1 -> pin + "1"
                        Key.Two, Key.NumPad2 -> pin + "2"
                        Key.Three, Key.NumPad3 -> pin + "3"
                        Key.Four, Key.NumPad4 -> pin + "4"
                        Key.Five, Key.NumPad5 -> pin + "5"
                        Key.Six, Key.NumPad6 -> pin + "6"
                        Key.Seven, Key.NumPad7 -> pin + "7"
                        Key.Eight, Key.NumPad8 -> pin + "8"
                        Key.Nine, Key.NumPad9 -> pin + "9"
                        else -> pin
                    }
                    if (newPin.length <= pinLength) {
                        onPinChange(newPin)
                    }
                    return@onKeyEvent newPin != lastPin
                }
                return@onKeyEvent false
            }
    ) {
        // The visual display for the PIN
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(pinLength) { index ->
                val char = when {
                    index < pin.length -> "●"
                    else -> ""
                }
                val boxColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(boxColor, shape = MaterialTheme.shapes.medium)
                        .border(
                            width = 1.dp,
                            color = if (isError) MaterialTheme.colorScheme.error else Color.Transparent,
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

    // Request focus when the component enters the composition.
    // This is safe because the focus target is the Box, not a TextField.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}