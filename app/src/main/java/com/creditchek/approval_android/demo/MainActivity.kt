package com.creditchek.approval_android.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.CreditChekApproval
import com.creditchek.approval_android.core.session.AUserData
import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.ApprovalEnv
import com.creditchek.approval_android.core.session.ApprovalModule
import com.creditchek.approval_android.core.session.SessionResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoAppScreen()
        }
    }
}

@Composable
fun DemoAppScreen() {
    val context = LocalContext.current
    var sessionStatus by remember { mutableStateOf("No verification started yet.") }
    var statusColor by remember { mutableStateOf(Color(0xFF4D515B)) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FB))
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CreditChek SDK Demo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C1E)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Test the complete Identity & Liveness verification flow",
                fontSize = 14.sp,
                color = Color(0xFF4D515B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ── Status Result Card ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Last Verification Status:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9A9A9A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sessionStatus,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ── Launch Button ──
            Button(
                onClick = {
                    sessionStatus = "Verification in progress..."
                    statusColor = Color(0xFF064BEF)

                    val publicKey = BuildConfig.PUBLIC_KEY

                    // 👉 Launch the SDK with 1 line of code:
                    CreditChekApproval.start(
                        context = context,
                        config = ApprovalConfig(
                            publicKey = "vy6LZWI/l/pOc868z8LAgEBCdvsSomPev2TxLqIdlNZIueMM0Agl8G88zxyE65LN",
                            environment = ApprovalEnv.SANDBOX,
                            modules = listOf(ApprovalModule.IDENTITY, ApprovalModule.LIVELINESS),
                            userData = AUserData(
                                firstName = "Marvellous",
                                lastName = "Ogbo",
                                email = "johndoe@example.com",
                                dob = "03/09/2002",
                                bvn = "22577700013"
                            )
                        )
                    ) { result ->
                        when (result) {
                            is SessionResult.Success -> {
                                sessionStatus = "✅ Success! Session ID:\n${result.sessionId}"
                                statusColor = Color(0xFF12A84A)
                            }

                            is SessionResult.Cancelled -> {
                                sessionStatus = "⚠️ Verification was dismissed/cancelled by user."
                                statusColor = Color(0xFFFF6D22)
                            }

                            is SessionResult.Error -> {
                                sessionStatus = "❌ Error [${result.code}]:\n${result.message}"
                                statusColor = Color(0xFFFF2543)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064BEF))
            ) {
                Text(
                    text = "Launch Verification Flow",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
