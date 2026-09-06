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
import com.example.vesbp.ui.theme.VeSBPTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay

private const val NSPK_PREFIX = "https://qr.nspk.ru/"
private const val UGLEMETBANK_SCHEME = "bank100000000093"
private const val SETTINGS_NAME = "vesbp_settings"
private const val THEME_MODE_KEY = "theme_mode"
private const val THEME_FADE_DURATION_MS = 240L
private const val THEME_SNAPSHOT_SCALE = 0.60f
private const val GITHUB_URL = "https://github.com/Q3D-Tech/VeSBP"
private const val TELEGRAM_URL = "https://t.me/verisbp"
private const val VERISHOP_URL = "https://t.me/VeriShopBot"

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
        setContent { VeSbpRoot(incomingUrl(intent)) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setContent { VeSbpRoot(incomingUrl(intent)) }
    }
}

private enum class ThemeMode { LIGHT, SYSTEM, DARK }

@Composable
private fun VeSbpRoot(initialUrl: String?) {
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
        VeSbpApp(initialUrl, themeMode) { selectedMode ->
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VeSbpApp(initialUrl: String?, themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    var link by rememberSaveable { mutableStateOf(nspkUrl(initialUrl)) }
    var bankMenu by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val rounded = RoundedCornerShape(22.dp)
    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500)
            copied = false
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
        SocialLinks(onInfo = { showAbout = true })
        Spacer(Modifier.height(28.dp))
    }

    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun SocialLinks(onInfo: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
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
            modifier = Modifier.size(46.dp).background(colors.surfaceVariant, shape).border(1.dp, colors.outline.copy(alpha = .55f), shape)
        ) {
            Icon(Icons.Outlined.Info, contentDescription = "О приложении", tint = colors.onSurface.copy(alpha = .78f), modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("VeSBP · версия 1.0.0")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("By ")
                    Text(
                        "VeriShop",
                        modifier = Modifier.clickable { openExternalUrl(context, VERISHOP_URL) },
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Понятно", color = colors.primary) }
        }
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
