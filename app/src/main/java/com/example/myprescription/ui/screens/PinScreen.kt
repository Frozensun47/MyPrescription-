package com.example.myprescription.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PinScreen(
    mode: PinScreenMode,
    onPinSet: (String) -> Unit,
    onPinEntered: (String) -> Unit,
    error: String?
) {
    var pin by remember { mutableStateOf("") }
    var shake by remember { mutableStateOf(false) }

    // When an error occurs, trigger the shake animation
    LaunchedEffect(error) {
        if (error != null) {
            shake = true
            delay(500) // Duration of shake
            shake = false
        }
    }

    val scale: Float by animateFloatAsState(
        targetValue = if (shake) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "shake_animation"
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .systemBarsPadding(), // Ensures content is not behind status/navigation bars
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (mode == PinScreenMode.SET) "Create a Secure PIN" else "Enter Your PIN",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your information is protected.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))

            PinInputField(
                pin = pin,
                onPinChange = {
                    if (it.length <= 4) pin = it.filter { c -> c.isDigit() }
                },
                isError = error != null,
                modifier = Modifier.scale(scale)
            )

            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (pin.length == 4) {
                        if (mode == PinScreenMode.SET) {
                            onPinSet(pin)
                        } else {
                            onPinEntered(pin)
                        }
                    }
                },
                enabled = pin.length == 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(50.dp)
            ) {
                Text(if (mode == PinScreenMode.SET) "Save and Continue" else "Unlock")
            }
        }
    }
}

@Composable
fun PinInputField(
    pin: String,
    onPinChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    pinLength: Int = 4
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100) // Delay to allow UI to settle before requesting focus
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Hidden text field to handle input
        BasicTextField(
            value = pin,
            onValueChange = onPinChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .size(0.dp) // Make it invisible
                .focusRequester(focusRequester),
            cursorBrush = SolidColor(Color.Transparent) // Hide cursor
        )

        // Visible PIN boxes
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
}

enum class PinScreenMode {
    SET, ENTER
}