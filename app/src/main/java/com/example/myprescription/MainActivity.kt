package com.example.myprescription

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.myprescription.ui.theme.MyPrescriptionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyPrescriptionTheme {
                MyPrescriptionApp()
            }
        }
    }
}
