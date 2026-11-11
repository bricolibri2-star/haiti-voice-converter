package com.haitivoice.converter

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val Night = Color(0xFF080B14)
private val Panel = Color(0xFF121827)
private val Cyan = Color(0xFF49E6D2)
private val Violet = Color(0xFF9B8CFF)
private val Muted = Color(0xFF9AA6B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Night,
                surface = Panel,
                primary = Cyan,
                secondary = Violet,
                onBackground = Color(0xFFF4F7FF),
                onSurface = Color(0xFFF4F7FF)
            )) {
                VoiceConverterScreen()
            }
        }
    }
}

@Composable
private fun VoiceConverterScreen() {
    val context = LocalContext.current
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var referenceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf<String?>(null) }
    var referenceName by remember { mutableStateOf<String?>(null) }
    var consent by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ajoutez les deux fichiers audio pour commencer.") }
    var fidelity by remember { mutableFloatStateOf(0.75f) }
    var pitch by remember { mutableFloatStateOf(0f) }

    val sourcePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        sourceUri = uri
        sourceName = uri?.let { displayName(context, it) }
        status = if (uri != null) "Audio source ajouté." else status
    }
    val referencePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        referenceUri = uri
        referenceName = uri?.let { displayName(context, it) }
        status = if (uri != null) "Voix de référence ajoutée." else status
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF111A2B), Night, Color(0xFF090D18))))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("KONVÈTI", color = Cyan, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("Voice Studio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { showSettings = true }) {
                    Text("⚙  PARAMÈTRES", color = Color.White)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF172338)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Cyan, CircleShape))
                        Text("  CONVERSION VOIX À VOIX", color = Cyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("Le rythme reste. Le timbre change.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("La source apporte les paroles et l’intonation ; la référence apporte le timbre.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
            }

            AudioPickerCard(
                number = "01",
                title = "Audio source",
                subtitle = "Paroles • cadence • intonation",
                fileName = sourceName,
                onClick = { sourcePicker.launch(arrayOf("audio/*")) }
            )
            AudioPickerCard(
                number = "02",
                title = "Voix de référence",
                subtitle = "Timbre vocal à appliquer",
                fileName = referenceName,
                onClick = { referencePicker.launch(arrayOf("audio/*")) }
            )

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = consent, onCheckedChange = { consent = it })
                    Text(
                        "J’ai l’autorisation d’utiliser la voix de référence.",
                        color = Color(0xFFE7ECF5),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = {
                    status = when {
                        sourceUri == null || referenceUri == null -> "Sélectionnez les deux fichiers audio."
                        !consent -> "Confirmez votre autorisation pour la voix de référence."
                        else -> "Fichiers prêts. Connectez le backend de conversion pour générer l’audio."
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF06110F))
            ) {
                Text("PRÉPARER LA CONVERSION  →", fontWeight = FontWeight.Bold)
            }

            Text(status, color = Muted, style = MaterialTheme.typography.bodySmall)
            Divider(color = Color(0xFF252F42))
            Text("Haïti d’abord • Créole / Français", color = Violet, style = MaterialTheme.typography.labelMedium)
            Text(
                "Prototype d’interface : la conversion réelle nécessite un moteur vocal côté serveur. Les paramètres sont préparatoires.",
                color = Muted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            containerColor = Panel,
            title = { Text("Paramètres audio", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Fidélité du timbre · ${(fidelity * 100).toInt()} %", color = Color.White)
                    Slider(value = fidelity, onValueChange = { fidelity = it }, valueRange = 0f..1f)
                    Spacer(Modifier.height(2.dp))
                    Text("Ajustement de hauteur · ${pitch.toInt()}", color = Color.White)
                    Slider(value = pitch, onValueChange = { pitch = it }, valueRange = -12f..12f, steps = 23)
                    Text("Langue : créole haïtien / français", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text("Ces réglages seront transmis au moteur lorsqu’il sera connecté.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Terminé", color = Cyan) } }
        )
    }
}

@Composable
private fun AudioPickerCard(
    number: String,
    title: String,
    subtitle: String,
    fileName: String?,
    onClick: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(number, color = Cyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text("♫", color = Violet, style = MaterialTheme.typography.titleLarge)
            }
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202C40))
            ) {
                Text(fileName?.let { "✓  $it" } ?: "CHOISIR UN FICHIER AUDIO", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun displayName(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return it.getString(index)
        }
    }
    return uri.lastPathSegment ?: "Fichier audio sélectionné"
}
