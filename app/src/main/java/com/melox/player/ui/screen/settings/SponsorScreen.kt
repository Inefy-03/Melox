package com.melox.player.ui.screen.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import com.melox.player.ui.component.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.ui.component.AdaptiveTopAppBar
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.component.PageCard as Card
import top.yukonga.miuix.kmp.basic.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import com.melox.player.ui.component.PageScaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import com.melox.player.ui.component.os4.navigationBackIcon
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun SponsorScreen(
    bottomContentPadding: Dp,
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    var backdropCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val backdrop = rememberBlurBackdrop(captureCoordinates = { backdropCaptureCoordinates })
    val layoutDirection = LocalLayoutDirection.current

    PageScaffold(
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                blurEnabled = backdrop != null,
                scrollBehavior = scrollBehavior,
            ) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.about_sponsor_title),
                    color = backdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = navigationBackIcon(),
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
                .then(backdrop?.let { Modifier.onGloballyPositioned { coordinates -> backdropCaptureCoordinates = coordinates }.layerBackdrop(it) } ?: Modifier),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection) + 12.dp,
                    top = padding.calculateTopPadding() + 12.dp,
                    end = padding.calculateEndPadding(layoutDirection) + 12.dp,
                    bottom = maxOf(padding.calculateBottomPadding(), bottomContentPadding) + 16.dp,
                ),
                overscrollEffect = null,
            ) {
                item(key = "sponsorshipCode") {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Card(
                            modifier = Modifier
                                .widthIn(max = 520.dp)
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            cornerRadius = 20.dp,
                            insideMargin = PaddingValues(0.dp),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.sponsorship_code),
                                contentDescription = stringResource(R.string.sponsor_code_description),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                                    .squircleClip(10.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                }
            }
        }
    }
}
