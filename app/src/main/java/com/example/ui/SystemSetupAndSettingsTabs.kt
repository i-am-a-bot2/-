package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KeyboardConfigState
import com.example.model.KeyboardLanguage
import com.example.model.KeyboardLayoutMode
import com.example.model.KeyboardStateBus
import com.example.model.WinClassicSkin

@Composable
fun SystemWideSetupTab(
    uiState: WinKeyUiState,
    viewModel: WinKeyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "הפעלה גלובלית בכל מקום במכשיר",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "כדי שלחיצה ארוכה על מקש 0 תקפיץ את מקלדת הווינדוס גם כשאתה במסך הבית וגם באמצע אפליקציות אחרות (כמו וואטסאפ, דפדפן או חייגן), הפעל את שלושת המנגנונים הבאים:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Step 1: Global Overlay (SYSTEM_ALERT_WINDOW)
        SetupStepCard(
            stepNumber = "1",
            title = "שכבה צפה מעל מסך הבית ואפליקציות (Overlay)",
            description = "מאפשר למקלדת הקלאסית של ווינדוס ולמקש ה-0 הצף לעלות על המסך גם כשאתה במסך הבית (ללא שדה טקסט פתוח) וגם מעל כל אפליקציה אחרת.",
            isActivated = uiState.isOverlayPermissionGranted,
            statusLabel = if (uiState.isOverlayPermissionGranted) "מאושר ומוכן" else "דרוש אישור הרשאה",
            primaryButtonText = if (uiState.isOverlayPermissionGranted) {
                "הפעל מקש 0 צף ומקלדת מעל כל המסך"
            } else {
                "אשר הרשאת תצוגה מעל אפליקציות"
            },
            primaryButtonTag = "setup_overlay_primary_button",
            onPrimaryClick = {
                if (uiState.isOverlayPermissionGranted) {
                    viewModel.startOrPopupGlobalOverlayService(context)
                } else {
                    openOverlayPermissionSettings(context)
                }
            },
            secondaryButtonText = if (uiState.isOverlayServiceRunning) "עצור שכבה צפה" else "בדוק סטטוס מחדש",
            onSecondaryClick = {
                if (uiState.isOverlayServiceRunning) {
                    viewModel.stopGlobalOverlayService(context)
                } else {
                    viewModel.refreshSystemStatuses(context)
                }
            }
        )

        // Step 2: Accessibility Service for Hardware '0' Key Interception
        SetupStepCard(
            stepNumber = "2",
            title = "שירות נגישות לזיהוי לחיצה ארוכה על מקש 0 פיזי",
            description = "מזהה לחיצה ארוכה על מקש ה-0 הפיזי של הפלאפון (KEYCODE_0) בכל מקום במערכת הפעלה (מסך הבית, חייגן, הודעות) ומקפיץ אוטומטית את מקלדת הווינדוס.",
            isActivated = uiState.isAccessibilityEnabled,
            statusLabel = if (uiState.isAccessibilityEnabled) "שירות נגישות פעיל" else "כבוי — לחץ להפעלה",
            primaryButtonText = "פתח הגדרות נגישות (Accessibility)",
            primaryButtonTag = "setup_accessibility_button",
            onPrimaryClick = {
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Throwable) {
                }
            },
            secondaryButtonText = "רענן בדיקת נגישות",
            onSecondaryClick = { viewModel.refreshSystemStatuses(context) }
        )

        // Step 3: System Keyboard (InputMethodService IME)
        SetupStepCard(
            stepNumber = "3",
            title = "הגדרה כמקלדת מערכת ראשית (IME)",
            description = "מאפשר לבחור ב-'מקלדת ווינדוס קלאסית (עברית/אנגלית)' כמקלדת ברירת המחדל של אנדרואיד בכל שדה טקסט.",
            isActivated = uiState.isImeEnabled,
            statusLabel = if (uiState.isImeEnabled) "המקלדת מופעלת במערכת" else "הפעל בהגדרות שפה ומקלדת",
            primaryButtonText = "1. הפעל מקלדת בהגדרות המכשיר",
            primaryButtonTag = "setup_ime_enable_button",
            onPrimaryClick = {
                try {
                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Throwable) {
                }
            },
            secondaryButtonText = "2. בחר מקלדת פעילה (Picker)",
            onSecondaryClick = {
                try {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showInputMethodPicker()
                } catch (_: Throwable) {
                }
            }
        )

        Spacer(modifier = Modifier.height(300.dp))
    }
}

@Composable
private fun SetupStepCard(
    stepNumber: String,
    title: String,
    description: String,
    isActivated: Boolean,
    statusLabel: String,
    primaryButtonText: String,
    primaryButtonTag: String,
    onPrimaryClick: () -> Unit,
    secondaryButtonText: String,
    onSecondaryClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.5.dp,
            if (isActivated) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stepNumber,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = if (isActivated) Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = if (isActivated) Color(0xFF1B5E20) else Color(0xFFE65100),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPrimaryClick,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag(primaryButtonTag)
                ) {
                    Text(text = primaryButtonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onSecondaryClick,
                    modifier = Modifier.weight(0.9f)
                ) {
                    Text(text = secondaryButtonText, fontSize = 11.5.sp)
                }
            }
        }
    }
}

private fun openOverlayPermissionSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
        }
    }
}

@Composable
fun SettingsAndSkinsTab(
    configState: KeyboardConfigState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Skin Selection Card
        Card(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "ערכת נושא למקלדת (Windows Classic Skins)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                WinClassicSkin.entries.forEach { skin ->
                    val selected = configState.skin == skin
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            if (selected) 2.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("skin_option_${skin.id}")
                            .clickable { KeyboardStateBus.setSkin(context, skin) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { KeyboardStateBus.setSkin(context, skin) }
                                )
                                Column {
                                    Text(text = skin.titleHe, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    Text(
                                        text = skin.subtitleHe,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(skin.chassisColor), RoundedCornerShape(6.dp))
                            )
                        }
                    }
                }
            }
        }

        // 2. Layout Mode Selection Card
        Card(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "פריסת מקשים ושפת התחלה",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                KeyboardLayoutMode.entries.forEach { mode ->
                    val selected = configState.layoutMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { KeyboardStateBus.setLayoutMode(context, mode) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { KeyboardStateBus.setLayoutMode(context, mode) }
                        )
                        Text(text = mode.titleHe, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("שפת כתיבה נוכחית (Alt + Shift):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    FilledTonalButton(
                        onClick = { KeyboardStateBus.toggleLanguage(context) },
                        modifier = Modifier.testTag("settings_toggle_language_button")
                    ) {
                        Text(
                            text = if (configState.language == KeyboardLanguage.HEBREW) {
                                "עברית (HE) ⇄ עבור לאנגלית"
                            } else {
                                "English (EN) ⇄ עבור לעברית"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Long-Press '0' Timing & Scale Sliders
        Card(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "זמן לחיצה ארוכה על מקש 0 וגודל מקלדת",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = "זמן החזקת מקש 0 להקפצת המקלדת: ${configState.longPressZeroThresholdMs} אלפיות שנייה (ms)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = configState.longPressZeroThresholdMs.toFloat(),
                    onValueChange = { KeyboardStateBus.setLongPressThreshold(context, it.toInt()) },
                    valueRange = 250f..1000f,
                    steps = 14
                )

                Text(
                    text = "גובה וגודל המקלדת על המסך: ${configState.keyboardScalePercent}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = configState.keyboardScalePercent.toFloat(),
                    onValueChange = { KeyboardStateBus.setKeyboardScalePercent(context, it.toInt()) },
                    valueRange = 80f..130f,
                    steps = 9
                )

                HorizontalDivider()

                SettingSwitchRow(
                    title = "רטט בלחיצה ארוכה על 0 ובהקלדה",
                    checked = configState.vibrationEnabled,
                    onCheckedChange = { KeyboardStateBus.setVibrationEnabled(context, it) }
                )
                SettingSwitchRow(
                    title = "צליל נקישת מקשים קלאסי של ווינדוס",
                    checked = configState.soundEnabled,
                    onCheckedChange = { KeyboardStateBus.setSoundEnabled(context, it) }
                )
                SettingSwitchRow(
                    title = "יירוט מקש 0 פיזי של הטלפון בכל מקום במכשיר",
                    checked = configState.interceptHardwareZeroKey,
                    onCheckedChange = { KeyboardStateBus.setInterceptHardwareZeroKey(context, it) }
                )
                SettingSwitchRow(
                    title = "הצג כפתור 0 צף בקצה המסך (למכשירי מגע ללא מקשים)",
                    checked = configState.showFloatingZeroBadge,
                    onCheckedChange = { KeyboardStateBus.setShowFloatingZeroBadge(context, it) }
                )
                SettingSwitchRow(
                    title = "מצב תאימות מנוע אנדרואיד 4.4 (SDK 19 KitKat)",
                    checked = configState.api19CompatMode,
                    onCheckedChange = { KeyboardStateBus.setApi19CompatMode(context, it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(300.dp))
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ApkAndSdk19GuideTab(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Direct APK Download Instructions for Users without Android Studio
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF002B66)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "איך להוריד קובץ APK מוכן להתקנה (ללא אנדרואיד סטודיו)",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = "אין צורך בתוכנת Android Studio במחשב! המערכת בנתה וקימפלה עבורך קובץ APK חתום ומוכן להתקנה ישירות על הפלאפון:",
                    color = Color(0xFFE0E8FF),
                    fontSize = 13.sp
                )

                ApkStepItem(
                    num = "1",
                    text = "לחץ על כפתור הייצוא (Export) שמוריד את קובץ ה-ZIP של הפרויקט."
                )
                ApkStepItem(
                    num = "2",
                    text = "פתח את קובץ ה-ZIP שירד — בתיקייה הראשית בפנים נמצא הקובץ המוכן: WinKey-Classic-HE-EN.apk."
                )
                ApkStepItem(
                    num = "3",
                    text = "חלץ את הקובץ WinKey-Classic-HE-EN.apk מתוך ה-ZIP, העבר אותו לטלפון ולחץ עליו להתקנה."
                )
                ApkStepItem(
                    num = "4",
                    text = "לאחר ההתקנה, פתח את האפליקציה, הפעל את השכבה הצפה ושירות הנגישות בלשונית 'הפעלה בכל המכשיר', ומעתה לחיצה ארוכה על 0 בכל מקום תעלה את המקלדת!"
                )
            }
        }

        // Android 4.4 (SDK 19) Architecture Compatibility Card
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "אבחון תאימות אנדרואיד 4.4 (SDK 19) ומכשירי מקשים",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = "המכשיר הנוכחי מריץ: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )

                val compatItems = listOf(
                    "מנוע ציור המקלדת (ClassicWindowsKeyboardView)" to "מבוסס Canvas של Android View בסיסי (תואם מ-API 1 עד API 36 ללא תלות בספריות כבדות)",
                    "סוג חלון צף מעל מסך הבית (Window Type)" to "תומך אוטומטית ב-TYPE_PHONE עבור אנדרואיד 4.4 (SDK 19), ב-TYPE_ACCESSIBILITY_OVERLAY מ-SDK 22, וב-TYPE_APPLICATION_OVERLAY מ-SDK 26+",
                    "יירוט לחיצה ארוכה על מקש ה-0 (KEYCODE_0)" to "פועל גם דרך AccessibilityService.onKeyEvent (מ-SDK 18+) וגם דרך InputMethodService.onKeyDown (מ-SDK 3+)",
                    "מנוע רטט וצלילי הקשה (Vibrator / ToneGenerator)" to "כולל תמיכה ישירה ב-vibrator.vibrate(ms) של אנדרואיד 4.4 לצד VibrationEffect החדש",
                    "תקן פריסת מקשים" to "תקן ישראלי רשמי SI-1452 (עברית קראטוןםפ + אנגלית QWERTY) עם Alt+Shift, Caps Lock ולוח ניקוד"
                )

                compatItems.forEach { (title, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Text(
                                text = desc,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(300.dp))
    }
}

@Composable
private fun ApkStepItem(num: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = Color(0xFFFFB300),
            shape = CircleShape,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = num,
                    color = Color(0xFF001B47),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.5.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
