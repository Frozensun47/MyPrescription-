package com.example.myprescription.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.myprescription.R
import com.google.firebase.auth.FirebaseUser

@Composable
fun AppMenuTray(
    user: FirebaseUser?,
    onChangeAccountClick: () -> Unit, // New callback for changing account
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp)
        ) {
            // User Profile Section
            if (user != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile picture (DP) made smaller
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current)
                                .data(user.photoUrl)
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .error(R.drawable.ic_launcher_foreground)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = "User Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp) // Smaller DP size
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Name and Email column
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.displayName ?: "User",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = user.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // Fallback view if user is null
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Default User Icon",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Welcome",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Change Account Button
            TextButton(
                onClick = onChangeAccountClick,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "Change Account",
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("Change account", color = MaterialTheme.colorScheme.primary)
            }


            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Spacer to push items to the bottom
            Spacer(modifier = Modifier.weight(1f))

            // Menu Items Section (now inside a Column to respect padding)
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = onSettingsClick,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Help") },
                    selected = false,
                    onClick = onHelpClick,
                    icon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("About") },
                    selected = false,
                    onClick = onAboutClick,
                    icon = { Icon(Icons.Default.Info, contentDescription = "About") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    }
}