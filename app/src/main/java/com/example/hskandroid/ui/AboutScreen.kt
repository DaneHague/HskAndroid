package com.hskmaster.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "HSK Master",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Version 1.1",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Privacy Policy
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = PRIVACY_POLICY_TEXT,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Terms of Service
            Text(
                text = "Terms of Service",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = TERMS_OF_SERVICE_TEXT,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Contact
            Text(
                text = "Contact",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@hskmaster.app")
                        putExtra(Intent.EXTRA_SUBJECT, "HSK Master - Support")
                    }
                    context.startActivity(Intent.createChooser(intent, "Send email"))
                }
            ) {
                Text("support@hskmaster.app")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private const val PRIVACY_POLICY_TEXT = """
HSK Master ("the App") respects your privacy. This policy describes what information the App collects and how it is used.

Information We Collect
The App stores your learning progress, quiz scores, and streak data locally on your device. This data is not transmitted to any external servers.

In-App Purchases
The App uses Google Play Billing for premium purchases. Purchase transactions are handled entirely by Google Play. We do not collect or store any payment information.

Third-Party Services
The App uses Google Play Services for billing. Google's privacy policy applies to data collected by their services.

Data Storage
All user data (progress, settings, purchase status) is stored locally on your device using Android SharedPreferences and a local Room database. No data is sent to external servers.

Children's Privacy
The App does not knowingly collect personal information from children under 13.

Changes to This Policy
We may update this privacy policy from time to time. Changes will be reflected in the App update.

Contact Us
If you have questions about this privacy policy, please contact us at support@hskmaster.app.
"""

private const val TERMS_OF_SERVICE_TEXT = """
By using HSK Master ("the App"), you agree to these terms.

Use of the App
The App is an educational tool for learning HSK Chinese vocabulary. You may use it for personal, non-commercial purposes.

In-App Purchases
Premium features are available as a one-time purchase through Google Play. All purchases are processed by Google Play and are subject to Google Play's terms and refund policies.

Intellectual Property
The App and its content are protected by copyright. You may not reproduce, distribute, or create derivative works from the App's content.

Disclaimer
The App is provided "as is" without warranties of any kind. We do not guarantee that the App will be error-free or uninterrupted.

Limitation of Liability
To the maximum extent permitted by law, we shall not be liable for any indirect, incidental, or consequential damages arising from your use of the App.

Contact
For questions about these terms, contact us at support@hskmaster.app.
"""
