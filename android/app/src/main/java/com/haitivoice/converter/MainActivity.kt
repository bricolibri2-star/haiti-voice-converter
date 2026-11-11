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
            MaterialTheme(colorScheme = darkColorScheme(background = Night, surface = Panel, primary = Cyan, secondary = Violet, onBackground = Color.White, onSurface = Color.White)) {
                VoiceConverterScreen()
            }
        }
    }
}

@Composable
private fun VoiceConverterScreen() {
    val context = LocalContext.current
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf<String?>(null) }
    var references by remember { mutableStateOf<List<Pair<Uri, String>>>(emptyList()) }
    var consent by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ajoutez l’audio source et les références vocales.") }
    var fidelity by remember { mutableFloatStateOf(0.75f) }
    var pitch by remember { mutableFloatStateOf(0f) }

    val sourcePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        sourceUri = uri
        sourceName = uri?.let { displayName(context, it) }
    }
    val referencePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        references = uris.map { it to displayName(context, it) }
        status = "${references.size} fichier(s) de référence ajouté(s)."
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF111A2B), Night, Color(0xFF090D18))))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("KONVÈTI", color = Cyan, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("Voice Studio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { showSettings = true }) { Text("⚙ PARAMÈTRES", color = Color.White) }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF172338)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Cyan, CircleShape))
                        Text("  CONVERSION VOIX À VOIX", color = Cyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("Le rythme reste. Le timbre change.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Source : paroles et intonation. Références : caractéristiques du timbre.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            AudioCard("01", "Audio source", "Paroles • cadence • intonation", sourceName ?: "Choisir un fichier", false) {
                sourcePicker.launch(arrayOf("audio/*"))
            }
            AudioCard("02", "Voix de référence", "Ajoutez plusieurs extraits propres et variés", "${references.size} fichier(s) sélectionné(s) — limite serveur : 20", true) {
                referencePicker.launch(arrayOf("audio/*"))
            }
            if (references.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        references.take(5).forEach { (_, name) ->
                            Text("• $name", color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        }
                        if (references.size > 5) Text("… et ${references.size - 5} autre(s)", color = Violet, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consent, onCheckedChange = { consent = it })
                    Text("J’ai l’autorisation d’utiliser la voix de référence.", color = Color(0xFFE7ECF5), style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(
                onClick = {
                    status = when {
                        sourceUri == null -> "Sélectionnez l’audio source."
                        references.isEmpty() -> "Sélectionnez au moins une référence vocale."
                        !consent -> "Confirmez votre autorisation pour cette voix."
                        else -> "Fichiers prêts. Le moteur de conversion doit être configuré côté serveur."
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF06110F))
            ) { Text("PRÉPARER LA CONVERSION →", fontWeight = FontWeight.Bold) }
            Text(status, color = Muted, style = MaterialTheme.typography.bodySmall)
            Text("Haïti d’abord • Kreyòl / Français", color = Violet, style = MaterialTheme.typography.labelMedium)
            Text("Une meilleure quantité peut aider si les extraits sont propres et variés, sans garantir une fidélité parfaite.", color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false }, containerColor = Panel,
            title = { Text("Paramètres audio", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Fidélité · ${(fidelity * 100).toInt()} %")
                    Slider(value = fidelity, onValueChange = { fidelity = it }, valueRange = 0f..1f)
                    Text("Hauteur · ${pitch.toInt()} demi-tons")
                    Slider(value = pitch, onValueChange = { pitch = it }, valueRange = -12f..12f, steps = 23)
                    Text("Créole haïtien / français", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text("Réglages d’interface ; ils ne changent pas le traitement tant que le moteur n’est pas connecté.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Terminé", color = Cyan) } }
        )
    }
}

@Composable
private fun AudioCard(number: String, title: String, subtitle: String, buttonText: String, references: Boolean, onClick: () -> Unit) {
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
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202C40))) {
                Text(if (references) "AJOUTER DES RÉFÉRENCES" else buttonText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun displayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index)
        }
    }
    return uri.lastPathSegment ?: "Audio sélectionné"
}
