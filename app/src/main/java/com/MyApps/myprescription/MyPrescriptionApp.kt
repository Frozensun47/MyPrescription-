package com.MyApps.myprescription

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.MyApps.myprescription.navigation.AppNavHost
import com.MyApps.myprescription.ui.theme.MyPrescriptionTheme

@Composable
fun MyPrescriptionApp() {
    MyPrescriptionTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            AppNavHost(navController = navController)
        }
    }
}