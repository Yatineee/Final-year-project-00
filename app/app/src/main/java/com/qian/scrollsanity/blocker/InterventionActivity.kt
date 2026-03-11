package com.qian.scrollsanity.blocker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class InterventionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PROMPT = "prompt_type"
        const val EXTRA_Z = "z"
        const val RESULT_USAGE_TYPE = "result_usage_type"
        const val USAGE_FUNCTIONAL = "functional"
        const val USAGE_HABITUAL = "habitual"

        const val PROMPT_ASK_USAGE_TYPE = "ask_usage_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prompt = intent.getStringExtra(EXTRA_PROMPT) ?: ""
        val z = intent.getDoubleExtra(EXTRA_Z, 0.0)

        setContent {
            MaterialTheme {
                if (prompt == PROMPT_ASK_USAGE_TYPE) {
                    AskUsageTypeDialog(
                        z = z,
                        onPick = { picked ->
                            val data = Intent().putExtra(RESULT_USAGE_TYPE, picked)
                            setResult(Activity.RESULT_OK, data)
                            finish()
                        }
                    )
                } else {
                    finish()
                }
            }
        }
    }
}

@Composable
private fun AskUsageTypeDialog(
    z: Double,
    onPick: (String) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

            Card(modifier = Modifier.padding(20.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Quick check", style = MaterialTheme.typography.titleLarge)
                    Text("Is this use intentional, or just habit?", style = MaterialTheme.typography.bodyMedium)
                    Text("z = ${"%.2f".format(z)}", style = MaterialTheme.typography.labelMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { onPick(InterventionActivity.USAGE_FUNCTIONAL) }
                        ) { Text("Intentional") }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { onPick(InterventionActivity.USAGE_HABITUAL) }
                        ) { Text("Habit") }
                    }
                }
            }
        }
    }
}