package com.example.myprescription.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.example.myprescription.ui.components.PinInput
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

    // When an error occurs, trigger the shake animation and clear the pin
    LaunchedEffect(error) {
        if (error != null) {
            pin = "" // Clear PIN on error
            shake = true
            delay(500) // Duration of shake
            shake = false
        }
    }

    // When the pin reaches full length, trigger the appropriate action
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            if (mode == PinScreenMode.SET) {
                onPinSet(pin)
            } else {
                onPinEntered(pin)
            }
        }
    }

    val scale: Float by animateFloatAsState(
        targetValue = if (shake) 1.05f else 1.0f,
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
                .systemBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (mode == PinScreenMode.SET) "Create a Secure PIN" else "Enter Your PIN",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (mode == PinScreenMode.SET) "Create a 4-digit PIN for quick access." else "Enter your 4-digit PIN to unlock.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Use the new, stable PinInput component
            PinInput(
                pin = pin,
                onPinChange = { newPin -> pin = newPin },
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

enum class PinScreenMode {
    SET, ENTER
}