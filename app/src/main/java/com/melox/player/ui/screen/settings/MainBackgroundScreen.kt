package com.melox.player.ui.screen.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.model.AppSettings
import com.melox.player.model.MIN_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT
import com.melox.player.model.MAX_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT
import com.melox.player.model.MAX_CUSTOM_BACKGROUND_DIM_PERCENT
import com.melox.player.model.normalizeCustomBackgroundBlurPercent
import com.melox.player.model.normalizeCustomBackgroundDimPercent
import com.melox.player.model.normalizeCustomBackgroundCardBlurPercent
import com.melox.player.model.normalizeCustomBackgroundCardOpacityPercent
import com.melox.player.ui.component.AdaptiveTopAppBar
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.PageScaffold
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.screen.playback.TappableSliderPreference
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import com.melox.player.ui.component.PageCard as Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun MainBackgroundScreen(
    settings: AppSettings,
    bottomContentPadding: Dp,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    onBack: () -> Unit,
    onImageChange: suspend (Uri) -> Boolean,
    onDeleteImage: suspend () -> Boolean,
    onBlurPercentChange: (Int) -> Unit,
    onDimPercentChange: (Int) -> Unit,
    onCardBlurPercentChange: (Int) -> Unit,
    onCardOpacityPercentChange: (Int) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current
    val cardBlurEnabled = settings.blurEnabled && isRuntimeShaderSupported()
    val scope = rememberCoroutineScope()
    var importingImage by remember { mutableStateOf(false) }
    var deletingImage by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var blurPercent by remember(settings.customBackgroundId) { mutableIntStateOf(settings.customBackgroundBlurPercent) }
    var dimPercent by remember(settings.customBackgroundId) { mutableIntStateOf(settings.customBackgroundDimPercent) }
    var cardBlurPercent by remember(settings.customBackgroundId) { mutableIntStateOf(settings.customBackgroundCardBlurPercent) }
    var cardOpacityPercent by remember(settings.customBackgroundId) { mutableIntStateOf(normalizeCustomBackgroundCardOpacityPercent(settings.customBackgroundCardOpacityPercent)) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                importingImage = true
                try {
                    if (!onImageChange(uri)) {
                        Toast.makeText(context, R.string.settings_custom_background_failed, Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    importingImage = false
                }
            }
        }
    }
    val topBarBackdrop = rememberBlurBackdrop()
    PageScaffold(
        topBar = {
            BlurredBar(
                backdrop = topBarBackdrop,
                blurEnabled = topBarBackdrop != null,
                scrollBehavior = scrollBehavior,
            ) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.settings_custom_background_title),
                    color = topBarBackdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.back))
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = listState,
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding() + 12.dp,
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = maxOf(padding.calculateBottomPadding(), bottomContentPadding) + 16.dp,
                ),
                overscrollEffect = null,
            ) {
                item(key = "main_background") {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        ArrowPreference(
                            title = stringResource(R.string.settings_background_choose_image),
                            enabled = !importingImage && !deletingImage,
                            onClick = {
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        )
                        AnimatedVisibility(visible = settings.customBackgroundId != null) {
                            Column {
                                TappableSliderPreference(
                                    title = stringResource(R.string.settings_custom_background_blur_title),
                                    value = blurPercent.toFloat(),
                                    valueText = stringResource(R.string.settings_percent_value, blurPercent),
                                    valueRange = 0f..100f,
                                    steps = 99,
                                    onValueChange = { value ->
                                        val percent = normalizeCustomBackgroundBlurPercent(value.roundToInt())
                                        if (percent != blurPercent) {
                                            blurPercent = percent
                                            onBlurPercentChange(percent)
                                        }
                                    },
                                )
                                TappableSliderPreference(
                                    title = stringResource(R.string.settings_background_dim),
                                    value = dimPercent.toFloat(),
                                    valueText = stringResource(R.string.settings_percent_value, dimPercent),
                                    valueRange = 0f..MAX_CUSTOM_BACKGROUND_DIM_PERCENT.toFloat(),
                                    steps = MAX_CUSTOM_BACKGROUND_DIM_PERCENT - 1,
                                    onValueChange = { value ->
                                        val percent = normalizeCustomBackgroundDimPercent(value.roundToInt())
                                        if (percent != dimPercent) {
                                            dimPercent = percent
                                            onDimPercentChange(percent)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                if (settings.customBackgroundId != null) {
                    item(key = "card_blur") {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 12.dp)) {
                            TappableSliderPreference(
                                title = stringResource(R.string.settings_background_card_blur),
                                enabled = cardBlurEnabled,
                                summary = if (cardBlurEnabled) null else stringResource(R.string.settings_background_card_blur_disabled_summary),
                                value = cardBlurPercent.toFloat(),
                                valueText = stringResource(R.string.settings_percent_value, cardBlurPercent),
                                valueRange = 0f..100f,
                                steps = 99,
                                onValueChange = { value ->
                                    val percent = normalizeCustomBackgroundCardBlurPercent(value.roundToInt())
                                    if (percent != cardBlurPercent) {
                                        cardBlurPercent = percent
                                        onCardBlurPercentChange(percent)
                                    }
                                },
                            )
                            TappableSliderPreference(
                                title = stringResource(R.string.settings_background_card_opacity),
                                value = cardOpacityPercent.toFloat(),
                                valueText = stringResource(R.string.settings_percent_value, cardOpacityPercent),
                                valueRange = MIN_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT.toFloat()..MAX_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT.toFloat(),
                                steps = MAX_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT - MIN_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT - 1,
                                onValueChange = { value ->
                                    val percent = normalizeCustomBackgroundCardOpacityPercent(value.roundToInt())
                                    if (percent != cardOpacityPercent) {
                                        cardOpacityPercent = percent
                                        onCardOpacityPercentChange(percent)
                                    }
                                },
                            )
                        }
                    }
                    item(key = "delete_background") {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 12.dp)) {
                            BasicComponent(
                                title = stringResource(R.string.settings_background_delete),
                                titleColor = BasicComponentDefaults.titleColor(color = MiuixTheme.colorScheme.primary),
                                enabled = !importingImage && !deletingImage,
                                onClick = { showDeleteDialog = true },
                            )
                        }
                    }
                }
            }
        }
        OverlayDialog(
            show = showDeleteDialog,
            title = stringResource(R.string.settings_background_delete_title),
            enableWindowDim = true,
            onDismissRequest = { if (!deletingImage) showDeleteDialog = false },
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = stringResource(R.string.clear_queue_confirm_cancel),
                    onClick = { showDeleteDialog = false },
                    enabled = !deletingImage,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = stringResource(R.string.clear_queue_confirm_confirm),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    enabled = !deletingImage,
                    onClick = {
                        scope.launch {
                            deletingImage = true
                            try {
                                if (onDeleteImage()) {
                                    showDeleteDialog = false
                                } else {
                                    Toast.makeText(context, R.string.settings_background_delete_failed, Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                deletingImage = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
