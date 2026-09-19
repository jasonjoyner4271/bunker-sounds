package com.bunkersounds.app

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.audiofx.PresetReverb
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

private data class Sound(val name: String, val detail: String)
private data class ReverbProfile(val preset: Short, val direct: Float, val wet: Float, val feedback: Float, val natureGain: Float)
private val sounds = listOf(Sound("Green noise", "Soft, balanced hush"), Sound("Brown noise", "Deep and warm"), Sound("Pink noise", "Gentle and even"), Sound("Grey noise", "Hearing-balanced hush"), Sound("White noise", "Bright and steady"), Sound("Box fan", "Steady low room hum"), Sound("Air conditioner", "Cool, even background hum"), Sound("Creek / river", "Georgia creek recording"), Sound("Morning river", "River ambience recording"), Sound("Waterfall", "Waterfall creek recording"))
private val basePresets = mapOf("Molly Falls" to mapOf("Brown noise" to .6f, "Grey noise" to .25f), "Angel's creek" to mapOf("Creek / river" to .6f, "Brown noise" to .4f), "Soft water" to mapOf("Morning river" to .75f, "Waterfall" to .35f), "Jason's Oasis" to mapOf("Green noise" to .5f, "Grey noise" to .15f, "Creek / river" to .4f, "Waterfall" to .25f), "Green Creek Falls (ALAS Work Mode)" to mapOf("Green noise" to .5f, "Creek / river" to .5f, "Waterfall" to .3f), "Brown Falls" to mapOf("Brown noise" to .55f, "Waterfall" to .75f), "Green River Falls" to mapOf("Green noise" to .45f, "Creek / river" to .45f, "Waterfall" to .35f), "Brown River Falls" to mapOf("Brown noise" to .5f, "Creek / river" to .45f, "Waterfall" to .35f), "Angel Falls" to mapOf("Pink noise" to .5f, "Waterfall" to .4f), "Angel Creek Falls" to mapOf("Pink noise" to .5f, "Creek / river" to .45f, "Waterfall" to .35f), "Grey Falls" to mapOf("Grey noise" to .5f, "Waterfall" to .4f), "Grey Creek Falls" to mapOf("Grey noise" to .5f, "Creek / river" to .45f, "Waterfall" to .35f), "Brown Creek" to mapOf("Brown noise" to .5f, "Creek / river" to .45f), "Brown River" to mapOf("Brown noise" to .5f, "Morning river" to .45f), "Brown Creek Falls" to mapOf("Brown noise" to .5f, "Creek / river" to .4f, "Waterfall" to .35f), "Grey Creek" to mapOf("Grey noise" to .5f, "Creek / river" to .45f), "Grey River" to mapOf("Grey noise" to .5f, "Morning river" to .45f), "Grey River Falls" to mapOf("Grey noise" to .5f, "Morning river" to .4f, "Waterfall" to .35f), "Green River" to mapOf("Green noise" to .45f, "Morning river" to .45f), "Pink River" to mapOf("Pink noise" to .5f, "Morning river" to .45f), "Pink Creek" to mapOf("Pink noise" to .5f, "Creek / river" to .45f))
private val noiseNames = setOf("Green noise", "Brown noise", "Pink noise", "Grey noise", "White noise")
private val reverbPresetNames = listOf("Green Creek Falls - Distant Deck")
private val effectNames = listOf("Off", "Close", "Distant Deck", "Open Valley", "Canyon Echo", "Hidden Cave", "Stone Chamber")
private val reverbProfiles = mapOf(
    "Close" to ReverbProfile(PresetReverb.PRESET_MEDIUMHALL, .78f, .28f, .86f, .9f),
    "Distant Deck" to ReverbProfile(PresetReverb.PRESET_MEDIUMHALL, .58f, .14f, .62f, .62f),
    "Open Valley" to ReverbProfile(PresetReverb.PRESET_LARGEHALL, .68f, .28f, .9f, .78f),
    "Canyon Echo" to ReverbProfile(PresetReverb.PRESET_PLATE, .55f, .45f, .94f, .7f),
    "Hidden Cave" to ReverbProfile(PresetReverb.PRESET_LARGEHALL, .42f, .6f, .96f, .62f),
    "Stone Chamber" to ReverbProfile(PresetReverb.PRESET_MEDIUMROOM, .5f, .52f, .9f, .68f)
)
private fun reverbProfileForEffect(name: String): ReverbProfile? = reverbProfiles[name]
private fun rebalancePreset(name: String, levels: Map<String, Float>): Map<String, Float> {
    if (name == "Waterfall River Escape" || name == "Jason's Oasis") return levels
    val primary = levels.keys.firstOrNull { it in noiseNames } ?: return levels
    val secondary = levels.keys.filter { it != primary }
    return buildMap {
        putAll(levels)
        put(primary, .8f)
        when (secondary.size) {
            1 -> put(secondary.single(), .2f)
            else -> secondary.forEach { put(it, if (it == "Waterfall") .2f else .4f) }
        }
    }
}
private val presets = (basePresets + reverbPresetNames.associateWith { basePresets["Green Creek Falls (ALAS Work Mode)"] ?: emptyMap<String, Float>() } + mapOf("Waterfall River Escape" to mapOf("Brown noise" to .5f, "Green noise" to .5f, "Creek / river" to .5f, "Morning river" to .5f, "Waterfall" to .5f, "Grey noise" to .3f), "Muddy River Falls" to emptyMap<String, Float>()) + buildMap<String, Map<String, Float>> {
    listOf("Green noise", "Brown noise", "Pink noise", "Grey noise", "White noise").forEach { noise ->
        val label = noise.removeSuffix(" noise")
        put("$label noise", mapOf(noise to .5f))
        put("$label Creek", mapOf(noise to .5f, "Creek / river" to .45f))
        put("$label River", mapOf(noise to .5f, "Morning river" to .45f))
        put("$label Falls", if (noise == "Brown noise") mapOf(noise to .6f, "Waterfall" to .4f) else mapOf(noise to .5f, "Waterfall" to .4f))
        put("$label Creek River", mapOf(noise to .5f, "Creek / river" to .4f, "Morning river" to .4f))
        if (label != "Green") put("$label Creek Falls", mapOf(noise to .5f, "Creek / river" to .4f, "Waterfall" to .35f))
        put("$label River Falls", mapOf(noise to .5f, "Morning river" to .4f, "Waterfall" to .35f))
        put("$label Creek River Falls", mapOf(noise to .5f, "Creek / river" to .35f, "Morning river" to .35f, "Waterfall" to .3f))
    }
}).mapValues { (name, levels) -> rebalancePreset(name, levels) }

private fun muddyRiverFallsLevels(): Map<String, Float> {
    val values = mutableListOf<Float>()
    while (values.size < sounds.size) {
        val value = Random.nextInt(15, 96) / 100f
        if (value !in values) values += value
    }
    return sounds.mapIndexed { index, sound -> sound.name to values[index] }.toMap()
}

private fun levelsForPreset(name: String, prefs: SharedPreferences): Map<String, Float> {
    if (name == "Muddy River Falls") return muddyRiverFallsLevels()
    if (name.startsWith("Muddy River Falls v")) {
        val version = name.removePrefix("Muddy River Falls v").toIntOrNull() ?: return emptyMap()
        return sounds.associate { it.name to prefs.getFloat("muddy_${version}_${it.name}", 0f) }
    }
    return sounds.associate { it.name to (presets[name]?.get(it.name) ?: 0f) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001); val wakePreset = intent.getStringExtra("wake_preset"); val wakeSound = intent.getStringExtra("wake_sound"); val wakeMusic = intent.getStringExtra("wake_music"); setContent { BunkerTheme { BunkerApp(wakePreset, wakeSound, wakeMusic) } } }
}

@Composable private fun BunkerApp(wakePresetFromAlarm: String? = null, wakeSoundFromAlarm: String? = null, wakeMusicFromAlarm: String? = null) {
    val context = LocalContext.current
    val engine = remember { NoiseEngine(context) }
    val usage = remember { UsageDatabase(context) }
    val prefs = remember { context.getSharedPreferences("bunker_settings", Context.MODE_PRIVATE) }
    var savedMuddyVersions by remember { mutableIntStateOf(prefs.getInt("muddy_saved_count", 0)) }
    var selectedEffect by remember { mutableStateOf(prefs.getString("selected_effect", "Off") ?: "Off") }
    var autoMuddy by remember { mutableStateOf(prefs.getBoolean("auto_muddy", false)) }
    var autoMuddyMinutes by remember { mutableIntStateOf(prefs.getInt("auto_muddy_minutes", 30)) }
    var master by remember { mutableFloatStateOf(prefs.getFloat("master", .75f)) }
    var timer by remember { mutableIntStateOf(prefs.getInt("timer", 0)) }
    var playing by remember { mutableStateOf(false) }
    var sessionId by remember { mutableLongStateOf(0L) }
    var mixMode by remember { mutableStateOf(prefs.getBoolean("mix_mode", true)) }
    var selectedPreset by remember { mutableStateOf<String?>(prefs.getString("selected_preset", "Molly Falls")) }
    var nightLight by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    val clockText by produceState(initialValue = "00:00") { while (true) { value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date()); delay(1000) } }
    var alarmOn by remember { mutableStateOf(prefs.getBoolean("alarm_on", false)) }
    var alarmHour by remember { mutableIntStateOf(prefs.getInt("alarm_hour", 7)) }
    var alarmMinute by remember { mutableIntStateOf(prefs.getInt("alarm_minute", 0)) }
    var wakePreset by remember { mutableStateOf(prefs.getString("wake_preset", "Molly Falls") ?: "Angel's creek") }
    var wakeSound by remember { mutableStateOf(prefs.getString("wake_sound", "Morning river") ?: "Morning river") }
    var wakeMusic by remember { mutableStateOf(prefs.getString("wake_music", "") ?: "") }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION); wakeMusic = it.toString(); if (alarmOn) scheduleAlarm(context, alarmHour, alarmMinute, wakePreset, wakeSound, it.toString()) } }
    var levels by remember { mutableStateOf(if (prefs.getString("selected_preset", "") == "Muddy River Falls") muddyRiverFallsLevels() else sounds.associate { it.name to if (prefs.contains("selected_preset")) prefs.getFloat("level_${it.name}", .55f) else presets["Molly Falls"]?.get(it.name) ?: .55f }) }

    LaunchedEffect(Unit) { engine.setLevels(levels, master) }
    LaunchedEffect(selectedEffect) { engine.setReverbProfile(reverbProfileForEffect(selectedEffect)) }
    LaunchedEffect(wakePresetFromAlarm) { wakePresetFromAlarm?.let { preset -> wakePreset = preset; levels = levelsForPreset(preset, prefs); selectedPreset = preset; engine.setLevels(levels, master); engine.startFadeIn(); playing = true } }
    LaunchedEffect(wakeSoundFromAlarm) { wakeSoundFromAlarm?.let { sound -> wakeSound = sound; levels = sounds.associate { it.name to if (it.name == sound) .75f else 0f }; selectedPreset = null; engine.setLevels(levels, master); engine.startFadeIn(); playing = true } }
    LaunchedEffect(wakeMusicFromAlarm) { wakeMusicFromAlarm?.takeIf { it.isNotBlank() }?.let { uri -> engine.fadeOut(); delay(10_000L); engine.stopSleepSounds(); engine.startMusic(Uri.parse(uri)); playing = true } }
    LaunchedEffect(levels, master, timer, mixMode, alarmOn, alarmHour, alarmMinute, wakePreset, wakeSound, wakeMusic, autoMuddy, autoMuddyMinutes, selectedEffect) { prefs.edit().putFloat("master", master).putInt("timer", timer).putBoolean("mix_mode", mixMode).putBoolean("alarm_on", alarmOn).putInt("alarm_hour", alarmHour).putInt("alarm_minute", alarmMinute).putString("wake_preset", wakePreset).putString("selected_preset", selectedPreset).putString("wake_sound", wakeSound).putString("wake_music", wakeMusic).putBoolean("auto_muddy", autoMuddy).putInt("auto_muddy_minutes", autoMuddyMinutes).putString("selected_effect", selectedEffect).apply().also { levels.forEach { (name, value) -> prefs.edit().putFloat("level_$name", value).apply() } } }
    DisposableEffect(Unit) { onDispose { if (sessionId != 0L) usage.stop(sessionId); engine.stop(); usage.close() } }
    LaunchedEffect(playing, timer) { if (playing && timer > 0) { delay(timer * 60_000L); engine.fadeOut(); delay(10_000L); playing = false; engine.stop() } }
    LaunchedEffect(autoMuddy, autoMuddyMinutes, selectedPreset, playing) {
        if (autoMuddy && playing && selectedPreset == "Muddy River Falls") {
            while (true) {
                delay(autoMuddyMinutes * 60_000L)
                levels = muddyRiverFallsLevels()
                engine.setLevels(levels, master)
            }
        }
    }
    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Image(painter = androidx.compose.ui.res.painterResource(com.bunkersounds.app.R.drawable.angel), contentDescription = "Angel sleeping", contentScale = ContentScale.Crop, alpha = .5f, modifier = Modifier.fillMaxSize())
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("BUNKER SOUNDS", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFD9E8D0)); Text("A quiet place to land.", color = Color(0xFF9EB19A)); Spacer(Modifier.height(6.dp)); Text("Currently playing", color = Color.Red, style = MaterialTheme.typography.labelLarge); Text(if (playing) (selectedPreset ?: levels.filter { it.value > 0f }.keys.joinToString(" + ").ifBlank { "Custom mix" }) else "Nothing playing", color = Color.White, style = MaterialTheme.typography.bodyLarge) }
            item { Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC1B251E))) { Column(Modifier.padding(16.dp)) { Text(if (playing) "Playing your mix" else "Ready when you are", color = Color.White); Spacer(Modifier.height(8.dp)); Button(onClick = { playing = !playing; if (playing) { sessionId = usage.start(selectedPreset ?: "Custom mix"); engine.start() } else { if (sessionId != 0L) usage.stop(sessionId); engine.stop() } }, modifier = Modifier.fillMaxWidth()) { Text(if (playing) "Pause mix" else "Start mix") } } } }
            item { Text("SOUNDS", color = Color(0xFFA9D18E), style = MaterialTheme.typography.labelLarge) }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("MIX MODE", color = Color.Red, style = MaterialTheme.typography.labelLarge); Text(if (mixMode) "Multiple sounds at once" else "One sound at a time", color = Color(0xFFB0B0B0), style = MaterialTheme.typography.bodySmall) }; Switch(checked = mixMode, onCheckedChange = { enabled -> mixMode = enabled; if (!enabled) { val keep = levels.entries.firstOrNull { it.value > 0f }?.key ?: sounds.first().name; levels = sounds.associate { it.name to if (it.name == keep) (levels[keep] ?: .55f) else 0f }; engine.setLevels(levels, master) } }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color.Red, uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color(0xFF3A1515))) } }
            item { BasicSounds(selectedPreset, onSelect = { name -> selectedPreset = name; selectedEffect = "Off"; levels = sounds.associate { it.name to if (it.name == name) .8f else 0f }; mixMode = false; engine.setLevels(levels, master) }) }
            item { AmbientSounds(selectedPreset, onSelect = { name -> selectedPreset = name; selectedEffect = "Off"; levels = sounds.associate { it.name to if (it.name == name) .8f else 0f }; mixMode = false; engine.setLevels(levels, master) }) }
            item { PresetGroups(savedMuddyVersions, selectedPreset, onSelect = { name -> selectedPreset = name; selectedEffect = if (name == "Green Creek Falls - Distant Deck") "Distant Deck" else "Off"; levels = levelsForPreset(name, prefs); mixMode = true; engine.setLevels(levels, master) }, onDeleteMuddy = { name ->
                val deleted = name.removePrefix("Muddy River Falls v").toIntOrNull()
                val count = savedMuddyVersions
                if (deleted != null && deleted in 1..count) {
                    for (version in deleted until count) {
                        sounds.forEach { sound -> prefs.edit().putFloat("muddy_${version}_${sound.name}", prefs.getFloat("muddy_${version + 1}_${sound.name}", 0f)).apply() }
                    }
                    sounds.forEach { sound -> prefs.edit().remove("muddy_${count}_${sound.name}").apply() }
                    prefs.edit().putInt("muddy_saved_count", count - 1).apply()
                    savedMuddyVersions = count - 1
                    if (selectedPreset == name) {
                        selectedPreset = "Muddy River Falls"
                        levels = muddyRiverFallsLevels()
                        engine.setLevels(levels, master)
                    }
                }
            }) }
            item { EffectSelector(selectedEffect, onSelect = { selectedEffect = it }) }
            if (selectedPreset == "Muddy River Falls") {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            levels = muddyRiverFallsLevels()
                            engine.setLevels(levels, master)
                        }, modifier = Modifier.fillMaxWidth()) { Text("Generate new mix") }
                        OutlinedButton(onClick = {
                            val version = savedMuddyVersions + 1
                            levels.forEach { (sound, value) -> prefs.edit().putFloat("muddy_${version}_$sound", value).apply() }
                            prefs.edit().putInt("muddy_saved_count", version).apply()
                            savedMuddyVersions = version
                            selectedPreset = "Muddy River Falls v$version"
                        }, modifier = Modifier.fillMaxWidth()) { Text("Save this Muddy River Falls blend", color = Color.Red) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("AUTO-REGENERATE", color = Color.Red, style = MaterialTheme.typography.labelLarge)
                                Text("New random mix while playing", color = Color(0xFFB0B0B0), style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = autoMuddy, onCheckedChange = { autoMuddy = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color.Red, uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color(0xFF3A1515)))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { autoMuddyMinutes = 30 }, colors = ButtonDefaults.outlinedButtonColors(contentColor = if (autoMuddyMinutes == 30) Color.Red else Color.White)) { Text("30 min") }
                            OutlinedButton(onClick = { autoMuddyMinutes = 60 }, colors = ButtonDefaults.outlinedButtonColors(contentColor = if (autoMuddyMinutes == 60) Color.Red else Color.White)) { Text("60 min") }
                        }
                    }
                }
            }
            items(sounds) { sound -> Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC101010))) { Column(Modifier.padding(14.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(sound.name, color = Color.White); Text(sound.detail, color = Color(0xFFB0B0B0), style = MaterialTheme.typography.bodySmall) }; Slider(value = levels[sound.name] ?: 0f, onValueChange = { value -> levels = if (mixMode) levels + (sound.name to value) else sounds.associate { it.name to if (it.name == sound.name) value else 0f }; engine.setLevels(levels, master) }, colors = SliderDefaults.colors(thumbColor = Color(0xB3FF0000), activeTrackColor = Color(0x66FF0000), inactiveTrackColor = Color(0x265A2020))) } } }
            item { Text("MASTER VOLUME", color = Color.Red, style = MaterialTheme.typography.labelLarge); Slider(value = master, onValueChange = { master = it; engine.setLevels(levels, master) }, colors = SliderDefaults.colors(thumbColor = Color(0xB3FF0000), activeTrackColor = Color(0x66FF0000), inactiveTrackColor = Color(0x265A2020))) }
            item { Text("SLEEP TIMER", color = Color(0xFFA9D18E), style = MaterialTheme.typography.labelLarge); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0, 15, 30, 60).forEach { minutes -> FilterChip(selected = timer == minutes, onClick = { timer = minutes }, label = { Text(if (minutes == 0) "Off" else "${minutes}m") }) } } }
            item { OutlinedButton(onClick = { nightLight = true }, modifier = Modifier.fillMaxWidth()) { Text("Open red night light", color = Color.Red) } }
            item { OutlinedButton(onClick = { showStats = true }, modifier = Modifier.fillMaxWidth()) { Text("View listening stats", color = Color.Red) } }
            item { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101010))) { Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("ALARM CLOCK", color = Color.Red, style = MaterialTheme.typography.labelLarge); Text(formatAlarmTime(alarmHour, alarmMinute), color = Color.White) }; Row { TextButton(onClick = { TimePickerDialog(context, { _, h, m -> alarmHour = h; alarmMinute = m; if (alarmOn) scheduleAlarm(context, h, m, wakePreset, wakeSound) }, alarmHour, alarmMinute, false).show() }) { Text("Set time", color = Color.Red) }; Switch(checked = alarmOn, onCheckedChange = { alarmOn = it; if (it) scheduleAlarm(context, alarmHour, alarmMinute, wakePreset, wakeSound) else cancelAlarm(context) }) } } } }
            item { Text("WAKE PRESET", color = Color.Red, style = MaterialTheme.typography.labelLarge); LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(presets.keys.toList()) { name -> FilterChip(selected = wakePreset == name, onClick = { wakePreset = name; if (alarmOn) scheduleAlarm(context, alarmHour, alarmMinute, name, wakeSound) }, label = { Text(name, maxLines = 1) }) } } }
            item { Text("WAKE SOUND", color = Color.Red, style = MaterialTheme.typography.labelLarge); LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(sounds) { sound -> FilterChip(selected = wakeSound == sound.name, onClick = { wakeSound = sound.name; if (alarmOn) scheduleAlarm(context, alarmHour, alarmMinute, wakePreset, sound.name, wakeMusic) }, label = { Text(sound.name, maxLines = 1) }) } } }
            item { Text("WAKE MUSIC", color = Color.Red, style = MaterialTheme.typography.labelLarge); OutlinedButton(onClick = { musicPicker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) { Text(if (wakeMusic.isBlank()) "Choose music from this phone" else "Music selected", color = Color.Red) } }
            item { Text("Everything stays on this device. Listening sessions are logged locally so you can see which presets you use most.", color = Color(0xFF819182), style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (nightLight) Dialog(onDismissRequest = { nightLight = false }) { Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text(clockText, color = Color(0xFFAA0000), fontSize = 96.sp); TextButton(onClick = { nightLight = false }, modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)) { Text("Close", color = Color(0xFF660000)) } } } }
        if (showStats) { val stats = usage.summary(); Dialog(onDismissRequest = { showStats = false }) { Surface(color = Color(0xFF101010), modifier = Modifier.padding(24.dp)) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("LISTENING STATS", color = Color.Red, style = MaterialTheme.typography.titleLarge); Text("Sessions: ${stats.sessions}", color = Color.White); Text("Listening time: ${stats.durationMs / 3_600_000}h ${(stats.durationMs / 60_000) % 60}m", color = Color.White); Text("Most used: ${stats.favorite}", color = Color.White); TextButton(onClick = { showStats = false }) { Text("Close", color = Color.Red) } } } } }
    }
}

private fun scheduleAlarm(context: Context, hour: Int, minute: Int, wakePreset: String, wakeSound: String, wakeMusic: String = "") { val calendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }; val intent = PendingIntent.getBroadcast(context, 42, Intent(context, AlarmReceiver::class.java).putExtra("wake_preset", wakePreset).putExtra("wake_sound", wakeSound).putExtra("wake_music", wakeMusic), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); val alarms = context.getSystemService(AlarmManager::class.java); if (Build.VERSION.SDK_INT >= 31 && alarms.canScheduleExactAlarms()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, intent) else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, intent) }

private fun formatAlarmTime(hour: Int, minute: Int): String { val displayHour = if (hour % 12 == 0) 12 else hour % 12; val amPm = if (hour < 12) "AM" else "PM"; return String.format("%d:%02d %s daily", displayHour, minute, amPm) }
private fun cancelAlarm(context: Context) { val intent = PendingIntent.getBroadcast(context, 42, Intent(context, AlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); context.getSystemService(AlarmManager::class.java).cancel(intent) }

private class NoiseEngine(private val context: Context) {
    @Volatile private var levels = emptyMap<String, Float>()
    @Volatile private var master = .75f
    @Volatile private var fade = 1f
    private var track: AudioTrack? = null
    private val nature = mutableMapOf<String, MediaPlayer>()
    private var reverbProfile: ReverbProfile? = null
    private var noiseReverb: PresetReverb? = null
    private val natureReverbs = mutableMapOf<String, PresetReverb>()
    private fun gain(name: String) = when (name) { "Morning river" -> 1f; "Creek / river" -> 1f; "Waterfall" -> .8f; else -> 1f }
    fun setLevels(newLevels: Map<String, Float>, newMaster: Float) { levels = newLevels; master = newMaster; listOf("Creek / river", "Morning river", "Waterfall").forEach { name -> val v = ((newLevels[name] ?: 0f) * newMaster * gain(name) * (reverbProfile?.natureGain ?: 1f)).coerceAtMost(1f); nature[name]?.setVolume(v, v) } }
    fun setReverbProfile(profile: ReverbProfile?) { reverbProfile = profile; nature.values.forEach { player -> try { player.setAuxEffectSendLevel(0f) } catch (_: IllegalStateException) { } }; natureReverbs.values.forEach { it.release() }; natureReverbs.clear(); if (profile != null) { nature.forEach { (name, player) -> if (name != "Wake music") { createReverb(profile)?.let { effect -> player.attachAuxEffect(effect.id); player.setAuxEffectSendLevel(profile.wet.coerceIn(0f, 1f)); natureReverbs[name] = effect } } } }; setLevels(levels, master) }
    private fun createReverb(profile: ReverbProfile): PresetReverb? = try { PresetReverb(0, 0).apply { preset = profile.preset; enabled = true } } catch (_: RuntimeException) { null }
    fun startFadeIn() { start(); fade = 0f; Thread { repeat(100) { if (track == null) return@Thread; fade = (it + 1) / 100f; nature.values.toList().forEach { p -> try { val v = fade * master; p.setVolume(v, v) } catch (_: IllegalStateException) { } }; Thread.sleep(100) } }.start() }
    fun start() { if (track != null) return; fade = 1f; startNature("Creek / river", com.bunkersounds.app.R.raw.creek_georgia); startNature("Morning river", com.bunkersounds.app.R.raw.river_morning); startNature("Waterfall", com.bunkersounds.app.R.raw.waterfall_creek); val rate = 44100; val size = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT); val bufferSize = (size * 4).coerceAtLeast(8192); track = AudioTrack(AudioManager.STREAM_MUSIC, rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM).also { it.play(); Thread { val b = ShortArray(bufferSize / 2); var brown = 0f; var greenLow = 0f; var greenVeryLow = 0f; var grey = 0f; var fanLow = 0f; var fanHigh = 0f; var acLow = 0f; var acHigh = 0f; var acPhase = 0f; var pink0 = 0f; var pink1 = 0f; var pink2 = 0f; var pink3 = 0f; var pink4 = 0f; var pink5 = 0f; var pink6 = 0f; var reverbTail = 0f; var warmupSamples = 0; while (track != null) { for (i in b.indices) { val white = Random.nextFloat() * 2f - 1f; brown = ((brown + white * .02f) / 1.02f).coerceIn(-1f, 1f); greenLow += (white - greenLow) * .08f; greenVeryLow += (greenLow - greenVeryLow) * .01f; grey += (white - grey) * .18f; fanLow += (white - fanLow) * .035f; fanHigh += (white - fanHigh) * .22f; acLow += (white - acLow) * .012f; acHigh += (white - acHigh) * .16f; acPhase += .0007f; val green = (greenLow - greenVeryLow) * 1.8f; val brownSound = brown * 2.7f + white * .035f; val boxFan = (fanLow * 1.7f + fanHigh * .12f).coerceIn(-1f, 1f); val airConditioner = (acLow * 2.2f + acHigh * .18f + sin(acPhase) * .035f).coerceIn(-1f, 1f); pink0 = .99886f * pink0 + white * .0555179f; pink1 = .99332f * pink1 + white * .0750759f; pink2 = .96900f * pink2 + white * .1538520f; pink3 = .86650f * pink3 + white * .3104856f; pink4 = .55000f * pink4 + white * .5329522f; pink5 = -.7616f * pink5 - white * .0168980f; pink6 = white * .115926f; val pink = (pink0 + pink1 + pink2 + pink3 + pink4 + pink5 + pink6 + white * .5362f) * .11f; val dryMix = (green * .85f * (levels["Green noise"] ?: 0f) + brownSound * (levels["Brown noise"] ?: 0f) + pink * .85f * (levels["Pink noise"] ?: 0f) + grey * .85f * (levels["Grey noise"] ?: 0f) + white * .85f * (levels["White noise"] ?: 0f) + boxFan * .85f * (levels["Box fan"] ?: 0f) + airConditioner * .85f * (levels["Air conditioner"] ?: 0f)) * master * fade; val profile = reverbProfile; reverbTail = reverbTail * (profile?.feedback ?: 0f) + dryMix * (profile?.wet ?: 0f); val mix = dryMix * (profile?.direct ?: 1f) + reverbTail; b[i] = if (warmupSamples-- > 0) 0 else (mix * 15000).toInt().coerceIn(-32768, 32767).toShort() }; it.write(b, 0, b.size, AudioTrack.WRITE_BLOCKING) } }.start() } }
    private fun startNature(name: String, resource: Int) { nature[name]?.release(); nature[name] = MediaPlayer.create(context, resource)?.apply { reverbProfile?.let { profile -> createReverb(profile)?.let { effect -> attachAuxEffect(effect.id); setAuxEffectSendLevel(profile.wet.coerceIn(0f, 1f)); natureReverbs[name] = effect } }; isLooping = true; val v = ((levels[name] ?: 0f) * master * gain(name) * (reverbProfile?.natureGain ?: 1f)).coerceAtMost(1f); setVolume(v, v); start() } ?: return }
    fun startMusic(uri: Uri) { nature["Wake music"]?.release(); nature["Wake music"] = MediaPlayer.create(context, uri)?.apply { isLooping = true; setVolume(0f, 0f); start() } ?: return; Thread { repeat(100) { try { nature["Wake music"]?.setVolume((it + 1) / 100f, (it + 1) / 100f) } catch (_: IllegalStateException) { }; Thread.sleep(100) } }.start() }
    fun stopSleepSounds() { val audio = track; track = null; audio?.let { try { it.stop() } catch (_: IllegalStateException) { }; it.release() }; noiseReverb?.release(); noiseReverb = null; natureReverbs.values.forEach { it.release() }; natureReverbs.clear(); nature.entries.filter { it.key != "Wake music" }.forEach { (_, player) -> try { player.stop() } catch (_: IllegalStateException) { }; player.release() }; nature.keys.removeAll { it != "Wake music" }; fade = 1f }
    fun fadeOut() { Thread { repeat(100) { if (track == null) return@Thread; fade = 1f - (it + 1) / 100f; nature.values.toList().forEach { p -> try { p.setVolume(fade * master, fade * master) } catch (_: IllegalStateException) { } }; Thread.sleep(100) }; fade = 0f }.start() }
    fun stop() { val audio = track; track = null; audio?.let { try { it.stop() } catch (_: IllegalStateException) { }; it.release() }; noiseReverb?.release(); noiseReverb = null; natureReverbs.values.forEach { it.release() }; natureReverbs.clear(); nature.values.forEach { try { it.stop() } catch (_: IllegalStateException) { }; it.release() }; nature.clear() }
}

@Composable private fun BunkerTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = darkColorScheme(primary = Color.Red, onPrimary = Color.White, surface = Color.Black, background = Color.Black), content = content) }

@Composable private fun EffectSelector(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("EFFECT", color = Color.Red, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text(selected, color = Color.White) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                effectNames.forEach { name ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { expanded = false; onSelect(name) }, trailingIcon = { if (selected == name) Text("✓", color = Color.Red) })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun PresetGroups(savedMuddyVersions: Int, selected: String?, onSelect: (String) -> Unit, onDeleteMuddy: (String) -> Unit) {
    val groups = listOf("Brown" to { it: String -> it.startsWith("Brown") || it == "Molly Falls" }, "Green" to { it: String -> it.startsWith("Green") }, "Grey" to { it: String -> it.startsWith("Grey") }, "Pink" to { it: String -> it.startsWith("Pink") || it.startsWith("Angel") }, "White" to { it: String -> it.startsWith("White") }, "Water" to { it: String -> it == "Angel's creek" || it == "Soft water" }, "Blends" to { it: String -> it == "Waterfall River Escape" || it == "Jason's Oasis" || it.startsWith("Muddy River Falls") || it == "Green Creek Falls - Distant Deck" })
    val presetNames = presets.keys + (1..savedMuddyVersions).map { "Muddy River Falls v$it" }
    var deleteCandidate by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PRESETS", color = Color.Red, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(groups) { (title, matches) ->
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text("$title noise", color = Color.White) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        presetNames.filter { matches(it) && !it.endsWith(" noise") }.sorted().forEach { name ->
                            DropdownMenuItem(modifier = Modifier.combinedClickable(onClick = { expanded = false; onSelect(name) }, onLongClick = { if (name.startsWith("Muddy River Falls v")) { expanded = false; deleteCandidate = name } }), text = { Text(name) }, onClick = { expanded = false; onSelect(name) }, trailingIcon = { if (selected == name) Text("✓", color = Color.Red) })
                        }
                    }
                }
            }
        }
    }
    deleteCandidate?.let { name ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete saved blend?") },
            text = { Text("Delete $name? This cannot be undone.") },
            confirmButton = { TextButton(onClick = { onDeleteMuddy(name); deleteCandidate = null }) { Text("Delete", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") } }
        )
    }
}

@Composable private fun BasicSounds(selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("BASIC SOUNDS", color = Color.Red, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Brown noise", "Green noise", "Grey noise", "Pink noise", "White noise")) { name ->
                OutlinedButton(onClick = { onSelect(name) }) { Text(name.removeSuffix(" noise"), color = if (selected == name) Color.Red else Color.White) }
            }
        }
    }
}

@Composable private fun AmbientSounds(selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AMBIENT SOUNDS", color = Color.Red, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Air conditioner", "Box fan")) { name ->
                OutlinedButton(onClick = { onSelect(name) }) { Text(name, color = if (selected == name) Color.Red else Color.White) }
            }
        }
    }
}
