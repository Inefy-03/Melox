package com.melox.player.ui.screen.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.BuildConfig
import com.melox.player.R
import com.melox.player.ui.component.MiuixBlurredBar
import com.melox.player.ui.component.effect.AboutEffectBackground
import com.melox.player.ui.component.rememberMiuixBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun AboutScreen(
    blurEnabled: Boolean,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val scrollProgress by remember {
        derivedStateOf {
            when {
                listState.firstVisibleItemIndex > 0 -> 1f
                else -> {
                    val spacer = listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.key == "aboutHeaderSpacer" }
                    if (spacer != null && spacer.size > 0) {
                        (listState.firstVisibleItemScrollOffset.toFloat() / spacer.size)
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
        }
    }
    val collapsed by remember {
        derivedStateOf { scrollProgress >= 0.999f }
    }
    val topBarBackdrop = rememberMiuixBlurBackdrop(enabled = blurEnabled)
    val barBackdrop = if (collapsed) topBarBackdrop else null
    val barColor = if (collapsed && barBackdrop == null) {
        MiuixTheme.colorScheme.surface
    } else {
        Color.Transparent
    }

    Scaffold(
        topBar = {
            MiuixBlurredBar(
                backdrop = barBackdrop,
                modifier = Modifier.background(barColor),
            ) {
                SmallTopAppBar(
                    title = stringResource(R.string.settings_about_title),
                    scrollBehavior = scrollBehavior,
                    color = Color.Transparent,
                    titleColor = MiuixTheme.colorScheme.onSurface.copy(
                        alpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f),
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        ) {
            AboutContent(
                padding = padding,
                listState = listState,
                scrollBehavior = scrollBehavior,
                scrollProgress = { scrollProgress },
                blurEnabled = blurEnabled,
                bottomContentPadding = bottomContentPadding,
            )
        }
    }
}

@Composable
private fun AboutContent(
    padding: PaddingValues,
    listState: androidx.compose.foundation.lazy.LazyListState,
    scrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior,
    scrollProgress: () -> Float,
    blurEnabled: Boolean,
    bottomContentPadding: Dp,
) {
    val uriHandler = LocalUriHandler.current
    val density = LocalDensity.current
    val versionName = BuildConfig.VERSION_NAME.ifBlank { "1.0.0" }
    var headerHeight by remember { mutableStateOf(190.dp) }
    val contentBackdrop = rememberMiuixBlurBackdrop(enabled = blurEnabled)
    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val logoBlend = remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(Color(0xE6A1A1A1), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x4DE6E6E6), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xFF1AF500), BlurBlendMode.Lab),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0xCC4A4A4A), BlurBlendMode.ColorBurn),
                BlendColorEntry(Color(0xFF4F4F4F), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xFF1AF200), BlurBlendMode.Lab),
            )
        }
    }
    val cardBlend = remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(Color(0x4DA9A9A9), BlurBlendMode.Luminosity),
                BlendColorEntry(Color(0x1A9C9C9C), BlurBlendMode.PlusDarker),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0x340034F9), BlurBlendMode.Overlay),
                BlendColorEntry(Color(0xB3FFFFFF), BlurBlendMode.HardLight),
            )
        }
    }

    AboutEffectBackground(
        modifier = Modifier.fillMaxSize(),
        backgroundModifier =
            contentBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier,
        alpha = { 1f - scrollProgress() },
    ) {
        AboutHeader(
            versionName = versionName,
            contentPadding = padding,
            scrollProgress = scrollProgress,
            logoBlend = logoBlend,
            backdrop = contentBackdrop,
            onHeightChanged = { height ->
                headerHeight = with(density) { height.toDp() }
            },
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = maxOf(
                    padding.calculateBottomPadding(),
                    bottomContentPadding,
                ),
            ),
            overscrollEffect = null,
        ) {
            item(key = "aboutHeaderSpacer") {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight + 218.dp),
                )
            }
            item(key = "aboutOptions") {
                Box {
                    Spacer(Modifier.fillParentMaxHeight())
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                            .then(
                                contentBackdrop?.let {
                                    Modifier.textureBlur(
                                        backdrop = it,
                                        shape = RoundedCornerShape(16.dp),
                                        blurRadius = 60f,
                                        noiseCoefficient = BlurDefaults.NoiseCoefficient,
                                        colors = BlurDefaults.blurColors(
                                            blendColors = cardBlend,
                                        ),
                                    )
                                } ?: Modifier,
                            ),
                        colors = CardDefaults.defaultColors(
                            color = if (contentBackdrop != null) {
                                Color.Transparent
                            } else {
                                MiuixTheme.colorScheme.surfaceContainer
                            },
                            contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
                        ),
                    ) {
                        ArrowPreference(
                            title = stringResource(R.string.about_project_title),
                            onClick = {
                                uriHandler.openUri(PROJECT_URL)
                            },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.about_developer_title),
                            onClick = {
                                uriHandler.openUri(DEVELOPER_GITHUB_URL)
                            },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.about_telegram_title),
                            onClick = {
                                uriHandler.openUri(TELEGRAM_CHANNEL_URL)
                            },
                        )
                    }
                }
            }
        }
    }
}

private const val PROJECT_URL = "https://github.com/Inefy-03/Melox"
private const val DEVELOPER_GITHUB_URL = "https://github.com/Inefy-03"
private const val TELEGRAM_CHANNEL_URL = "https://t.me/MeloxPlayer"

@Composable
private fun AboutHeader(
    versionName: String,
    contentPadding: PaddingValues,
    scrollProgress: () -> Float,
    logoBlend: List<BlendColorEntry>,
    backdrop: LayerBackdrop?,
    onHeightChanged: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = contentPadding.calculateTopPadding() + 92.dp)
            .onSizeChanged { onHeightChanged(it.height) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val iconProgress = ((scrollProgress() - 0.35f) / 0.15f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .requiredSize(112.dp)
                .graphicsLayer {
                    alpha = 1f - iconProgress
                    scaleX = 1f - iconProgress * 0.05f
                    scaleY = 1f - iconProgress * 0.05f
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .requiredSize(200.dp)
                    .aboutHeaderBlur(
                        backdrop = backdrop,
                        cornerRadius = 24.dp,
                        blurRadius = 200f,
                        logoBlend = logoBlend,
                    ),
            )
        }

        val nameProgress = ((scrollProgress() - 0.20f) / 0.15f).coerceIn(0f, 1f)
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier
                .padding(top = 12.dp, bottom = 5.dp)
                .graphicsLayer {
                    alpha = 1f - nameProgress
                    scaleX = 1f - nameProgress * 0.05f
                    scaleY = 1f - nameProgress * 0.05f
                }
                .aboutHeaderBlur(
                    backdrop = backdrop,
                    cornerRadius = 16.dp,
                    blurRadius = 150f,
                    logoBlend = logoBlend,
                ),
            fontWeight = FontWeight.Bold,
            fontSize = 35.sp,
        )

        val versionProgress = ((scrollProgress() - 0.05f) / 0.15f).coerceIn(0f, 1f)
        Text(
            text = stringResource(R.string.about_version, versionName),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = 1f - versionProgress
                    scaleX = 1f - versionProgress * 0.05f
                    scaleY = 1f - versionProgress * 0.05f
                },
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Modifier.aboutHeaderBlur(
    backdrop: LayerBackdrop?,
    cornerRadius: androidx.compose.ui.unit.Dp,
    blurRadius: Float,
    logoBlend: List<BlendColorEntry>,
): Modifier = if (backdrop == null) {
    this
} else {
    textureBlur(
        backdrop = backdrop,
        shape = RoundedCornerShape(cornerRadius),
        blurRadius = blurRadius,
        noiseCoefficient = BlurDefaults.NoiseCoefficient,
        colors = BlurColors(blendColors = logoBlend),
        contentBlendMode = BlendMode.DstIn,
    )
}
