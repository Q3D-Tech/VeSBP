package com.example.vesbp

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.content.FileProvider
import com.example.vesbp.ui.theme.VeSBPTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val NSPK_PREFIX = "https://qr.nspk.ru/"
private const val UGLEMETBANK_SCHEME = "bank100000000093"
private const val SETTINGS_NAME = "vesbp_settings"
private const val THEME_MODE_KEY = "theme_mode"
private const val THEME_FADE_DURATION_MS = 240L
private const val THEME_SNAPSHOT_SCALE = 0.60f
private const val GITHUB_URL = "https://github.com/Q3D-Tech/VeSBP"
private const val TELEGRAM_URL = "https://t.me/verisbp"
private const val VERISHOP_URL = "https://t.me/VeriShopBot"
private const val DEBUG_UPDATE_PREVIEW_EXTRA = "vesbp_update_preview"

private data class SupportAddress(val asset: String, val network: String, val address: String)

private val supportAddresses = listOf(
    SupportAddress("USDT", "TRC20", "THDdetaN5hyL8ZxctC75aTBSDT4bDUKtYC"),
    SupportAddress("USDT", "ERC20", "0x4ead462500337829bB6cDEB437187CC4eBE755dC"),
    SupportAddress("USDT", "BEP20", "0x4ead462500337829bB6cDEB437187CC4eBE755dC"),
    SupportAddress("USDT", "Polygon", "0x4ead462500337829bB6cDEB437187CC4eBE755dC"),
    SupportAddress("USDT", "Solana", "EdURB1MUpTQjUi4UWLBpw7tQARWkcHCm3tSzBFnYHHB6"),
    SupportAddress("USDC", "Arbitrum One", "0x4ead462500337829bB6cDEB437187CC4eBE755dC"),
    SupportAddress("USDC", "ERC20", "0x4ead462500337829bB6cDEB437187CC4eBE755dC"),
    SupportAddress("USDC", "Base", "0x4ead462500337829bB6cDEB437187CC4eBE755dC"),
    SupportAddress("ETH", "ERC20", "0x4ead462500337829bB6cDEB437187CC4eBE755dC"),
    SupportAddress("POL", "Polygon", "0x4ead462500337829bB6cDEB437187CC4eBE755dC"),
    SupportAddress("BTC", "Bitcoin", "bc1q6k5xqdvwt9mmy38d9fnmwsk6efnrn1r8jqe0mj"),
    SupportAddress("TRX", "Tron", "THDdetaN5hyL8ZxctC75aTBSDT4bDUKtYC"),
    SupportAddress("SOL", "Solana", "EdURB1MUpTQjUi4UWLBpw7tQARWkcHCm3tSzBFnYHHB6"),
    SupportAddress("GRAM", "TON", "UQBIET64WYFty_VVPfH0NAruWnan91H6fbt81VSY0GN5i1Q8")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { VeSbpRoot(incomingUrl(intent), debugUpdatePreview(intent)) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setContent { VeSbpRoot(incomingUrl(intent), debugUpdatePreview(intent)) }
    }
}

private fun debugUpdatePreview(intent: Intent?): String? =
    intent?.getStringExtra(DEBUG_UPDATE_PREVIEW_EXTRA)?.takeIf { BuildConfig.DEBUG }

private enum class ThemeMode { LIGHT, SYSTEM, DARK }

@Composable
private fun VeSbpRoot(initialUrl: String?, debugUpdatePreview: String? = null) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings = remember(context) {
        context.applicationContext.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
    }
    var themeMode by rememberSaveable {
        mutableStateOf(
            settings.getString(THEME_MODE_KEY, null)
                ?.let { saved -> ThemeMode.values().firstOrNull { it.name == saved } }
                ?: ThemeMode.SYSTEM
        )
    }
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
    VeSBPTheme(darkTheme = dark) {
        VeSbpApp(initialUrl, themeMode, debugUpdatePreview) { selectedMode ->
            if (selectedMode != themeMode) {
                (context as? Activity)?.crossfadeTheme(view) {
                    themeMode = selectedMode
                    settings.edit().putString(THEME_MODE_KEY, selectedMode.name).apply()
                } ?: run {
                    themeMode = selectedMode
                    settings.edit().putString(THEME_MODE_KEY, selectedMode.name).apply()
                }
            }
        }
    }
}

private fun Activity.crossfadeTheme(composeView: View, applyTheme: () -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || composeView.width == 0 || composeView.height == 0) {
        applyTheme()
        return
    }
    val root = findViewById<ViewGroup>(android.R.id.content) ?: run {
        applyTheme()
        return
    }
    val width = (composeView.width * THEME_SNAPSHOT_SCALE).toInt()
    val height = (composeView.height * THEME_SNAPSHOT_SCALE).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    PixelCopy.request(window, bitmap, { result ->
        if (result != PixelCopy.SUCCESS || isFinishing || isDestroyed) {
            bitmap.recycle()
            applyTheme()
            return@request
        }
        val snapshot = ImageView(this).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            isClickable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        root.addView(snapshot, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        applyTheme()
        snapshot.animate()
            .alpha(0f)
            .setDuration(THEME_FADE_DURATION_MS)
            .setInterpolator(LinearInterpolator())
            .withEndAction {
                root.removeView(snapshot)
                bitmap.recycle()
            }
            .start()
    }, Handler(Looper.getMainLooper()))
}

private fun incomingUrl(intent: Intent?): String? = when (intent?.action) {
    Intent.ACTION_VIEW -> intent.dataString
    Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
    else -> null
}

private fun nspkUrl(value: String?): String? {
    val text = value?.trim().orEmpty()
    if (text.startsWith(NSPK_PREFIX, true)) return text.substringBefore('?').trimEnd('/')
    val code = if (text.startsWith("$UGLEMETBANK_SCHEME://", true)) {
        text.substringAfter("$UGLEMETBANK_SCHEME://").substringAfter('/', "")
    } else ""
    return code.substringBefore('?').substringBefore('#').trim('/').takeIf { it.isNotBlank() }?.let { "$NSPK_PREFIX$it" }
}

private fun openExternalUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
    }
}

private fun canInstallPackages(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

private fun openUnknownSourcesSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}")
    )
    runCatching { context.startActivity(intent) }.onFailure {
        Toast.makeText(context, "Не удалось открыть настройки установки", Toast.LENGTH_SHORT).show()
    }
}

private fun openPackageInstaller(context: Context, apkFile: File): Boolean = runCatching {
    val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apkFile)
    context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
    true
}.getOrElse {
    Toast.makeText(context, "Не удалось открыть установщик", Toast.LENGTH_SHORT).show()
    false
}

private fun formatUpdateDate(value: String): String? = runCatching {
    val source = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    val result = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
    result.format(source.parse(value) ?: return null)
}.getOrNull()

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes Б"
    bytes < 1_024 * 1_024 -> "${bytes / 1_024} КБ"
    else -> String.format(Locale.US, "%.1f МБ", bytes / (1_024f * 1_024f))
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VeSbpApp(
    initialUrl: String?,
    themeMode: ThemeMode,
    debugUpdatePreview: String?,
    onThemeChange: (ThemeMode) -> Unit,
) {
    var link by rememberSaveable { mutableStateOf(nspkUrl(initialUrl)) }
    var bankMenu by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showUpdateProgress by rememberSaveable { mutableStateOf(false) }
    var showInstallPermission by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val updateManager = remember(context) { UpdateManager(context) }
    val updateScope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Checking) }
    val colors = MaterialTheme.colorScheme
    val rounded = RoundedCornerShape(22.dp)
    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500)
            copied = false
        }
    }
    LaunchedEffect(updateManager, debugUpdatePreview) {
        if (debugUpdatePreview != null) {
            val sampleUpdate = AppUpdate(
                version = "1.0.1",
                publishedAt = "2026-09-06T12:00:00Z",
                notes = """
                    В версии 1.0.1 обновлён механизм получения новых релизов.

                    - Добавлена проверка последней версии VeSBP через GitHub Releases.
                    - При наличии новой версии под значком информации появляется статус обновления.
                    - Скачивание показывает реальный объём файла, скорость и оставшееся время.
                    - После загрузки обновление можно установить из информации о приложении.
                    - Добавлена подсказка для разрешения установки APK из VeSBP в настройках Android.
                    - Улучшены оформление статуса и отображение длинного списка изменений.

                    Это демонстрационный текст для проверки полного раскрытия описания обновления.
                """.trimIndent(),
                downloadUrl = "",
                assetName = "VeSBP-1.0.1-universal.apk",
            )
            updateState = when (debugUpdatePreview) {
                "downloaded" -> UpdateState.Downloaded(sampleUpdate, File(context.filesDir, "updates/demo.apk"))
                else -> UpdateState.Available(sampleUpdate)
            }
            return@LaunchedEffect
        }
        val savedUpdate = updateManager.downloadedUpdate()
            ?.takeIf { updateManager.isNewerThanInstalled(it.first.version, BuildConfig.VERSION_NAME) }
        if (savedUpdate != null) updateState = UpdateState.Downloaded(savedUpdate.first, savedUpdate.second)

        updateManager.fetchLatest()
            .onSuccess { latest ->
                val matchingDownload = savedUpdate?.takeIf { it.first.version == latest.version }
                updateState = when {
                    matchingDownload != null -> UpdateState.Downloaded(matchingDownload.first, matchingDownload.second)
                    updateManager.isNewerThanInstalled(latest.version, BuildConfig.VERSION_NAME) -> UpdateState.Available(latest)
                    else -> UpdateState.UpToDate(latest)
                }
            }
            .onFailure {
                if (savedUpdate == null) updateState = UpdateState.Failed("Не удалось проверить обновления")
            }
    }
    fun downloadUpdate(update: AppUpdate) {
        showUpdateProgress = true
        updateState = UpdateState.Downloading(update, DownloadProgress(0L, 0L, 0L))
        updateScope.launch {
            updateManager.download(update) { progress ->
                updateScope.launch { updateState = UpdateState.Downloading(update, progress) }
            }.onSuccess { apk ->
                updateState = UpdateState.Downloaded(update, apk)
                showUpdateProgress = false
            }.onFailure {
                updateState = UpdateState.Failed("Не удалось скачать обновление")
                showUpdateProgress = false
            }
        }
    }
    fun installUpdate(update: AppUpdate, apkFile: File) {
        if (!canInstallPackages(context)) {
            showInstallPermission = true
            return
        }
        updateState = UpdateState.Installing(update, apkFile)
        updateScope.launch {
            delay(160)
            if (openPackageInstaller(context, apkFile)) {
                delay(500)
                updateState = UpdateState.Downloaded(update, apkFile)
            } else {
                updateState = UpdateState.Downloaded(update, apkFile)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(44.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.veri_logo_mark), contentDescription = "Veri", modifier = Modifier.size(34.dp))
            Text("eSBP", modifier = Modifier.offset(x = (-2).dp), color = colors.onBackground, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { link = null; copied = false },
                enabled = link != null,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Outlined.CleaningServices,
                    contentDescription = "Очистить QR-код и ссылку",
                    tint = if (link != null) colors.primary else colors.onBackground.copy(alpha = .24f),
                    modifier = Modifier.size(23.dp)
                )
            }
            IconButton(onClick = { showHelp = true }, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Outlined.HelpOutline,
                    contentDescription = "Как пользоваться VeSBP",
                    tint = colors.onBackground.copy(alpha = .72f),
                    modifier = Modifier.size(25.dp)
                )
            }
        }
        Text("QR-код для оплаты через СБП", modifier = Modifier.fillMaxWidth(), color = colors.onBackground.copy(alpha = .62f), fontSize = 15.sp)
        Spacer(Modifier.height(26.dp))
        Text("Банк", color = colors.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = bankMenu, onExpandedChange = { bankMenu = it }) {
            OutlinedTextField(
                value = "Углеметбанк", onValueChange = {}, readOnly = true,
                modifier = Modifier.width(218.dp).menuAnchor(), shape = RoundedCornerShape(50.dp),
                textStyle = TextStyle(textAlign = TextAlign.Center),
                leadingIcon = { Spacer(Modifier.size(48.dp)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankMenu) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedContainerColor = colors.surface, unfocusedContainerColor = colors.surface
                )
            )
            ExposedDropdownMenu(expanded = bankMenu, onDismissRequest = { bankMenu = false }, shape = rounded) {
                DropdownMenuItem(text = { Text("Углеметбанк") }, onClick = { bankMenu = false })
            }
        }
        Spacer(Modifier.height(20.dp))
        QrArea(link, rounded)
        Spacer(Modifier.height(20.dp))
        PaymentLink(link, copied, rounded) {
            link?.let {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("Платёжная ссылка", it))
                copied = true
                Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
            }
        }
        Spacer(Modifier.height(16.dp))
        ThemeButtons(themeMode, onThemeChange)
        Spacer(Modifier.height(18.dp))
        SupportProject()
        Spacer(Modifier.height(16.dp))
        SocialLinks(updateState = updateState, onInfo = { showAbout = true })
        Spacer(Modifier.height(28.dp))
    }

    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }
    if (showAbout) {
        AboutDialog(
            updateState = updateState,
            onDismiss = { showAbout = false },
            onDownload = { downloadUpdate(it) },
            onInstall = { update, file -> installUpdate(update, file) },
            onRetry = {
                updateState = UpdateState.Checking
                updateScope.launch {
                    updateManager.fetchLatest()
                        .onSuccess { latest ->
                            updateState = if (updateManager.isNewerThanInstalled(latest.version, BuildConfig.VERSION_NAME)) {
                                UpdateState.Available(latest)
                            } else UpdateState.UpToDate(latest)
                        }
                        .onFailure { updateState = UpdateState.Failed("Не удалось проверить обновления") }
                }
            }
        )
    }
    if (showUpdateProgress) {
        UpdateProgressDialog(updateState = updateState, onDismiss = { showUpdateProgress = false })
    }
    if (showInstallPermission) {
        InstallPermissionDialog(
            onDismiss = { showInstallPermission = false },
            onOpenSettings = {
                showInstallPermission = false
                openUnknownSourcesSettings(context)
            }
        )
    }
}

@Composable
private fun SocialLinks(updateState: UpdateState, onInfo: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50.dp)
    val updateMarker = when (updateState) {
        is UpdateState.Available -> "Найдено обновление"
        is UpdateState.Downloaded -> "Обновление скачано"
        else -> null
    }
    val infoBorder = when (updateState) {
        is UpdateState.Available -> colors.primary
        is UpdateState.Downloaded -> Color(0xFF16914B)
        else -> colors.outline.copy(alpha = .55f)
    }
    Box(
        modifier = Modifier.fillMaxWidth().height(if (updateMarker == null) 46.dp else 112.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            IconButton(
                onClick = { openExternalUrl(context, GITHUB_URL) },
                modifier = Modifier.size(46.dp).background(colors.surfaceVariant, shape).border(1.dp, colors.outline.copy(alpha = .55f), shape)
            ) {
                Icon(painterResource(R.drawable.ic_github), contentDescription = "GitHub", tint = colors.onSurface, modifier = Modifier.size(22.dp))
            }
            IconButton(
                onClick = { openExternalUrl(context, TELEGRAM_URL) },
                modifier = Modifier.size(46.dp).background(colors.surfaceVariant, shape).border(1.dp, colors.outline.copy(alpha = .55f), shape)
            ) {
                Icon(painterResource(R.drawable.ic_telegram), contentDescription = "Telegram", tint = Color(0xFF229ED9), modifier = Modifier.size(23.dp))
            }
            IconButton(
                onClick = onInfo,
                modifier = Modifier.size(46.dp).background(colors.surfaceVariant, shape).border(1.5.dp, infoBorder, shape)
            ) {
                Icon(Icons.Outlined.Info, contentDescription = "О приложении", tint = colors.onSurface.copy(alpha = .78f), modifier = Modifier.size(23.dp))
            }
        }
        if (updateMarker != null) {
            UpdateBubble(
                label = updateMarker,
                color = infoBorder,
                modifier = Modifier.align(Alignment.TopCenter).offset(x = 58.dp, y = 46.dp),
                onClick = onInfo,
            )
        }
    }
}

@Composable
private fun UpdateBubble(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bubbleShape = RoundedCornerShape(16.dp)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(9.dp))
        Canvas(Modifier.width(38.dp).height(13.dp)) {
            val middle = size.width / 2f
            val path = Path().apply {
                moveTo(3.dp.toPx(), size.height)
                cubicTo(size.width * .24f, size.height * .78f, middle - 4.dp.toPx(), 6.dp.toPx(), middle, 1.dp.toPx())
                cubicTo(middle + 4.dp.toPx(), 6.dp.toPx(), size.width * .76f, size.height * .78f, size.width - 3.dp.toPx(), size.height)
                close()
            }
            drawPath(path, color.copy(alpha = .14f))
        }
        Text(
            label,
            modifier = Modifier
                .background(color.copy(alpha = .14f), bubbleShape)
                .border(1.dp, color.copy(alpha = .44f), bubbleShape)
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun AboutDialog(
    updateState: UpdateState,
    onDismiss: () -> Unit,
    onDownload: (AppUpdate) -> Unit,
    onInstall: (AppUpdate, File) -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurface.copy(alpha = .72f),
        title = { Text("О приложении", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("VeSBP · версия ${BuildConfig.VERSION_NAME}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("By ")
                    Text(
                        "VeriShop",
                        modifier = Modifier.clickable { openExternalUrl(context, VERISHOP_URL) },
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                HorizontalDivider(color = colors.outline.copy(alpha = .42f))
                UpdateInfo(
                    updateState = updateState,
                    onDownload = onDownload,
                    onInstall = onInstall,
                    onRetry = onRetry,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть", color = colors.primary) }
        }
    )
}

@Composable
private fun UpdateInfo(
    updateState: UpdateState,
    onDownload: (AppUpdate) -> Unit,
    onInstall: (AppUpdate, File) -> Unit,
    onRetry: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier.fillMaxWidth().background(colors.surfaceVariant, shape).border(1.dp, colors.outline.copy(alpha = .52f), shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text("Обновления", color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        when (updateState) {
            UpdateState.Checking -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Проверяем наличие обновлений", color = colors.onSurface.copy(alpha = .68f), fontSize = 13.sp)
                }
            }
            is UpdateState.UpToDate -> {
                Text("Установлена последняя версия", color = colors.onSurface.copy(alpha = .72f), fontSize = 13.sp)
                updateState.latest?.let { update ->
                    Text("Последняя версия: ${update.version}", color = colors.onSurface.copy(alpha = .56f), fontSize = 12.sp)
                }
            }
            is UpdateState.Available -> {
                Text("Обнаружено новое обновление", color = colors.primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                UpdateReleaseDetails(updateState.update)
                Button(
                    onClick = { onDownload(updateState.update) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                ) { Text("Обновить") }
            }
            is UpdateState.Downloading -> {
                Text("Обновление скачивается", color = colors.onSurface.copy(alpha = .72f), fontSize = 13.sp)
                UpdateReleaseDetails(updateState.update, showNotes = false)
            }
            is UpdateState.Downloaded -> {
                Text("Обновление скачано", color = Color(0xFF16914B), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                UpdateReleaseDetails(updateState.update)
                Button(
                    onClick = { onInstall(updateState.update, updateState.file) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16914B), contentColor = Color.White)
                ) { Text("Установить") }
            }
            is UpdateState.Installing -> {
                Text("Открываем установщик Android", color = colors.onSurface.copy(alpha = .72f), fontSize = 13.sp)
            }
            is UpdateState.Failed -> {
                Text(updateState.message, color = colors.error, fontSize = 13.sp)
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Проверить ещё раз")
                }
            }
        }
    }
}

@Composable
private fun UpdateReleaseDetails(update: AppUpdate, showNotes: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Text("Версия ${update.version}", color = colors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    formatUpdateDate(update.publishedAt)?.let { date ->
        Text("Опубликовано $date", color = colors.onSurface.copy(alpha = .58f), fontSize = 12.sp)
    }
    if (showNotes) UpdateNotes(update.notes)
}

@Composable
private fun UpdateNotes(notes: String) {
    val colors = MaterialTheme.colorScheme
    var expanded by rememberSaveable(notes) { mutableStateOf(false) }
    val text = notes.trim().ifBlank { "Описание обновления не добавлено." }
    Text("Что нового", color = colors.onSurface.copy(alpha = .68f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    Text(
        text,
        color = colors.onSurface.copy(alpha = .70f),
        fontSize = 12.sp,
        maxLines = if (expanded) Int.MAX_VALUE else 3,
        overflow = TextOverflow.Ellipsis,
    )
    if (text.length > 105) {
        Text(
            if (expanded) "Свернуть" else "Показать полностью",
            modifier = Modifier.clickable { expanded = !expanded },
            color = colors.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun UpdateProgressDialog(updateState: UpdateState, onDismiss: () -> Unit) {
    val downloading = updateState as? UpdateState.Downloading ?: return
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurface.copy(alpha = .72f),
        title = { Text("Скачивание обновления", fontWeight = FontWeight.Bold) },
        text = {
            DownloadingStatus(downloading.progress)
        },
        confirmButton = {},
    )
}

@Composable
private fun DownloadingStatus(progress: DownloadProgress) {
    val colors = MaterialTheme.colorScheme
    val fraction = if (progress.totalBytes > 0) (progress.downloadedBytes.toFloat() / progress.totalBytes).coerceIn(0f, 1f) else null
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (progress.downloadedBytes == 0L) {
            Text("Подготавливаем скачивание…", color = colors.onSurface.copy(alpha = .72f))
        } else {
            if (fraction == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            }
            val amount = if (progress.totalBytes > 0) "${formatFileSize(progress.downloadedBytes)} из ${formatFileSize(progress.totalBytes)}" else formatFileSize(progress.downloadedBytes)
            val speed = if (progress.bytesPerSecond > 0) " · ${formatFileSize(progress.bytesPerSecond)}/с" else ""
            val remaining = if (progress.totalBytes > 0 && progress.bytesPerSecond > 0) {
                val seconds = ((progress.totalBytes - progress.downloadedBytes) / progress.bytesPerSecond).coerceAtLeast(0)
                " · осталось ${seconds} с"
            } else ""
            Text("$amount$speed$remaining", color = colors.onSurface.copy(alpha = .68f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun InstallPermissionDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurface.copy(alpha = .72f),
        title = { Text("Разрешите установку", fontWeight = FontWeight.Bold) },
        text = { Text("Чтобы установить скачанное обновление, разрешите VeSBP устанавливать приложения из этого источника.") },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = colors.onSurface.copy(alpha = .72f)) } },
        confirmButton = { TextButton(onClick = onOpenSettings) { Text("Открыть настройки", color = colors.primary) } }
    )
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurface.copy(alpha = .72f),
        title = { Text("Как пользоваться VeSBP", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("VeSBP преобразует переданную банком ссылку СБП в стандартную ссылку НСПК и QR-код для оплаты.")
                Text("1. При оплате через СБП выберите банк, который сейчас выбран в VeSBP (например, Углеметбанк).")
                Text("2. Откройте сформированную ссылку в VeSBP. Если приложение не предлагается, используйте «Поделиться → VeSBP».")
                Text("3. Отсканируйте QR-код с другого устройства или скопируйте платёжную ссылку.")
                Text("VeSBP не проводит платёж, не списывает средства и не получает доступ к банковскому аккаунту.", color = colors.onSurface.copy(alpha = .60f), fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Понятно", color = colors.primary) }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SupportProject() {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var selectedAsset by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var copied by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(copied) {
        if (copied) { delay(1_500); copied = false }
    }
    Text(
        "Поддержать проект",
        modifier = Modifier.clickable { showSheet = true }, color = colors.primary,
        fontWeight = FontWeight.SemiBold, fontSize = 14.sp
    )
    Spacer(Modifier.height(4.dp))
    Text("Будем рады вашей поддержке!", color = colors.onBackground.copy(alpha = .58f), fontSize = 12.sp)

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false; selectedAsset = null; selectedIndex = null },
            dragHandle = { SupportSheetHandle() },
        ) {
            when {
                selectedAsset == null -> AssetPicker { selectedAsset = it }
                selectedIndex == null -> NetworkPicker(
                    asset = selectedAsset!!,
                    onBack = { selectedAsset = null },
                ) { selectedIndex = it; copied = false }
                else -> {
                    val selected = supportAddresses[selectedIndex!!]
                    DonationDetails(selected, copied, onBack = { selectedIndex = null }) {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText("Адрес ${selected.asset} (${selected.network})", selected.address))
                        copied = true
                        Toast.makeText(context, "Адрес скопирован", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetPicker(onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val assets = supportAddresses.map { it.asset }.distinct()
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        SheetHeader("Поддержать проект", "Выберите криптовалюту")
        Spacer(Modifier.height(14.dp))
        assets.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { asset -> SupportChoice(asset, Modifier.weight(1f)) { onSelect(asset) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun NetworkPicker(
    asset: String,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val options = supportAddresses.withIndex().filter { it.value.asset == asset }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        SheetHeader(asset, "Выберите сеть", onBack)
        Spacer(Modifier.height(14.dp))
        options.forEach { option ->
            SupportChoice(option.value.network, Modifier.fillMaxWidth()) { onSelect(option.index) }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun DonationDetails(selected: SupportAddress, copied: Boolean, onBack: () -> Unit, onCopy: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val rounded = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        SheetHeader("${selected.asset} (${selected.network})", onBack = onBack)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.size(216.dp).background(Color.White, rounded).border(1.dp, colors.outline.copy(alpha = .6f), rounded), contentAlignment = Alignment.Center) {
            QrCode(selected.address, Modifier.size(208.dp), Color.Black)
        }
        Spacer(Modifier.height(14.dp))
        SupportAddressField(selected.address, copied, rounded, onCopy)
        Spacer(Modifier.height(12.dp))
        Text("Этот адрес принимает только ${selected.asset} (${selected.network}). Не отправляйте на него другие активы.", modifier = Modifier.fillMaxWidth().background(colors.surfaceVariant, rounded).border(1.dp, colors.outline.copy(alpha = .55f), rounded).padding(14.dp), color = colors.onSurface.copy(alpha = .72f), fontSize = 12.sp)
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun SupportSheetHandle() {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.padding(top = 10.dp, bottom = 4.dp)
            .width(34.dp)
            .height(4.dp)
            .background(colors.onSurface.copy(alpha = .32f), RoundedCornerShape(50.dp))
    )
}

@Composable
private fun SheetHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
                    .size(40.dp)
                    .background(colors.surfaceVariant, RoundedCornerShape(50.dp))
                    .border(1.dp, colors.outline.copy(alpha = .45f), RoundedCornerShape(50.dp))
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад", tint = colors.onSurface)
            }
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = colors.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, color = colors.onSurface.copy(alpha = .62f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SupportChoice(label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val rounded = RoundedCornerShape(16.dp)
    Box(modifier.height(52.dp).background(colors.surfaceVariant, rounded).border(1.dp, colors.outline.copy(alpha = .55f), rounded).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, color = colors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun SupportAddressField(address: String, copied: Boolean, shape: RoundedCornerShape, onCopy: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 62.dp).background(colors.surface, shape).border(1.dp, colors.outline.copy(alpha = .7f), shape).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(address, Modifier.weight(1f), color = colors.onSurface, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 3)
        Icon(if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy, "Копировать адрес", tint = if (copied) Color(0xFF16803C) else colors.primary, modifier = Modifier.size(44.dp).padding(10.dp).clickable { onCopy() })
    }
}

@Composable
private fun ThemeButtons(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shell = RoundedCornerShape(18.dp)
    val selectedIndex = when (selected) {
        ThemeMode.LIGHT -> 0
        ThemeMode.SYSTEM -> 1
        ThemeMode.DARK -> 2
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(44.dp).background(colors.surfaceVariant, shell).padding(4.dp)
    ) {
        val segmentWidth = maxWidth / 3
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(durationMillis = 220),
            label = "themeSelection"
        )
        Box(
            Modifier.offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .background(colors.surface, RoundedCornerShape(14.dp))
        )
        Row(Modifier.fillMaxSize()) {
            ThemeOption("Светлая", ThemeMode.LIGHT, selected, colors, onSelect)
            ThemeOption("Система", ThemeMode.SYSTEM, selected, colors, onSelect)
            ThemeOption("Тёмная", ThemeMode.DARK, selected, colors, onSelect)
        }
    }
}

@Composable
private fun RowScope.ThemeOption(label: String, mode: ThemeMode, selected: ThemeMode, colors: ColorScheme, onSelect: (ThemeMode) -> Unit) {
    val active = mode == selected
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier.weight(1f).fillMaxHeight().clickable(
            interactionSource = interactionSource,
            indication = null,
        ) { onSelect(mode) },
        contentAlignment = Alignment.Center
    ) { Text(label, color = if (active) colors.primary else colors.onSurface.copy(alpha = .58f), fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp) }
}

@Composable
private fun QrArea(link: String?, shape: RoundedCornerShape) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(252.dp).background(if (link == null) colors.surface else Color.White, shape).border(1.dp, colors.outline.copy(alpha = .7f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (link == null) EmptyQrIcon(Modifier.size(170.dp), colors.onSurface.copy(alpha = .30f))
        else QrCode(link, Modifier.size(244.dp), Color.Black)
    }
}

@Composable
private fun EmptyQrIcon(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val unit = size.minDimension / 10f
        val stroke = unit * 1.05f
        val corner = CornerRadius(unit * 1.05f)
        fun finder(x: Float, y: Float) {
            drawRoundRect(color, Offset(x * unit, y * unit), Size(unit * 3.3f, unit * 3.3f), corner, style = Stroke(stroke))
            drawRoundRect(color, Offset((x + 1.02f) * unit, (y + 1.02f) * unit), Size(unit * 1.25f, unit * 1.25f), CornerRadius(unit * .32f))
        }
        finder(.55f, .55f); finder(6.15f, .55f); finder(.55f, 6.15f)
        drawRoundRect(color, Offset(5.0f * unit, 4.8f * unit), Size(1.05f * unit, .9f * unit), CornerRadius(unit * .35f))
        drawRoundRect(color, Offset(6.3f * unit, 4.8f * unit), Size(3.1f * unit, .9f * unit), CornerRadius(unit * .35f))
        drawRoundRect(color, Offset(5.0f * unit, 6.2f * unit), Size(.9f * unit, 3.1f * unit), CornerRadius(unit * .35f))
        drawRoundRect(color, Offset(6.4f * unit, 6.2f * unit), Size(2.85f * unit, .9f * unit), CornerRadius(unit * .35f))
        drawRoundRect(color, Offset(7.8f * unit, 7.6f * unit), Size(1.45f * unit, 1.7f * unit), CornerRadius(unit * .35f))
        drawRoundRect(color, Offset(6.4f * unit, 8.4f * unit), Size(1.0f * unit, .9f * unit), CornerRadius(unit * .35f))
    }
}

@Composable
private fun QrCode(value: String, modifier: Modifier, color: Color) {
    val matrix = remember(value) { QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 43, 43, mapOf(EncodeHintType.MARGIN to 0)) }
    Canvas(modifier) {
        val left = (0 until matrix.width).first { x -> (0 until matrix.height).any { y -> matrix[x, y] } }
        val right = (matrix.width - 1 downTo 0).first { x -> (0 until matrix.height).any { y -> matrix[x, y] } }
        val top = (0 until matrix.height).first { y -> (0 until matrix.width).any { x -> matrix[x, y] } }
        val bottom = (matrix.height - 1 downTo 0).first { y -> (0 until matrix.width).any { x -> matrix[x, y] } }
        val modules = maxOf(right - left + 1, bottom - top + 1)
        val quietZone = 4
        val cell = size.width / (modules + quietZone * 2)
        val start = (size.width - modules * cell) / 2f
        for (x in left..right) for (y in top..bottom) if (matrix[x, y]) {
            drawRect(color, Offset(start + (x - left) * cell, start + (y - top) * cell), Size(cell + .5f, cell + .5f))
        }
    }
}

@Composable
private fun PaymentLink(link: String?, copied: Boolean, shape: RoundedCornerShape, onCopy: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val active = link != null
    Row(
        Modifier.fillMaxWidth().height(58.dp).background(colors.surface, shape).border(1.dp, colors.outline.copy(alpha = .7f), shape).padding(start = 18.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(link ?: "Платёжная ссылка", Modifier.weight(1f), color = if (active) colors.onSurface else colors.onSurface.copy(alpha = .45f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
        Icon(if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy, "Копировать ссылку", tint = if (copied) Color(0xFF16803C) else if (active) colors.primary else colors.onSurface.copy(alpha = .25f), modifier = Modifier.size(44.dp).padding(10.dp).then(if (active) Modifier.clickable { onCopy() } else Modifier))
    }
}

@Preview(showBackground = true)
@Composable private fun PreviewVeSbp() { VeSbpRoot(null) }
