package com.example.team1application


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team1application.ui.theme.Team1ApplicationTheme


class MainActivity : ComponentActivity() {
    //  今後このoncleate多分消えてなくなるからどっかに避難
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Team1ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize(),containerColor = Color(0xFFC8A2C8)) { innerPadding ->
                    CurrentTimeDisplay(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


