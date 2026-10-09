package com.melox.player

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.melox.player.model.AppSettings
import com.melox.player.model.LocalPlaylist
import com.melox.player.ui.LibrarySearchBar
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.FixedPageBackgroundHost
import com.melox.player.ui.component.trackFixedWallpaperMotion
import com.melox.player.ui.component.LocalCustomPageBackground
import com.melox.player.ui.component.LocalPageCardBlurRadius
import com.melox.player.ui.component.LocalPageCardSurfaceAlpha
import com.melox.player.ui.component.LocalPageSurfaceBackdrop
import com.melox.player.ui.component.LocalTopBarBlurSettings
import com.melox.player.ui.component.PageCard
import com.melox.player.ui.component.PageScaffold
import com.melox.player.ui.component.TopBarBlurSettings
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.component.playlist.PlaylistGridItem
import com.melox.player.ui.theme.MeloxTheme
import com.melox.player.ui.screen.settings.AboutScreen
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FixedWallpaperRenderingTest {
    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun wallpaperSearchUsesNativeEightyPercentFillOverTheExistingTopBar() {
        val blurEnabled = mutableStateOf(false)
        val progressiveEnabled = mutableStateOf(false)
        val source = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.RED)
        }
        val image = source.asImageBitmap()
        var bounds = ComposeRect.Zero
        var fillColor = 0
        var density = 1f
        lateinit var view: ComposeView
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    density = activity.resources.displayMetrics.density
                    view = ComposeView(activity).apply {
                        setContent {
                            MeloxTheme(AppSettings(blurEnabled = blurEnabled.value)) {
                                fillColor = MiuixTheme.colorScheme.surfaceContainerHigh.toArgb()
                                CompositionLocalProvider(
                                    LocalCustomPageBackground provides image,
                                    LocalTopBarBlurSettings provides TopBarBlurSettings(blurEnabled.value, progressiveEnabled.value),
                                ) {
                                    val backdrop = rememberBlurBackdrop()
                                    PageScaffold(
                                        topBar = {
                                            BlurredBar(backdrop, blurEnabled.value) {
                                                SmallTopAppBar(
                                                    title = "",
                                                    color = backdrop.miuixBarColor(),
                                                    bottomContent = {
                                                        Box(Modifier.onGloballyPositioned { bounds = it.boundsInRoot() }) {
                                                            LibrarySearchBar(
                                                                visible = true,
                                                                focused = false,
                                                                query = "",
                                                                label = "",
                                                                onQueryChange = {},
                                                                onFocusedChange = {},
                                                                onVisibleChange = {},
                                                            )
                                                        }
                                                    },
                                                )
                                            }
                                        },
                                    ) {
                                        Box(Modifier.fillMaxSize().then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier))
                                    }
                                }
                            }
                        }
                    }
                    activity.setContentView(view)
                }
                for ((blur, progressive) in listOf(false to false, true to false, true to true)) {
                    scenario.onActivity {
                        blurEnabled.value = blur
                        progressiveEnabled.value = progressive
                    }
                    // Let the actual search entrance and backdrop captures finish.
                    repeat(16) { copyFrame(scenario, view).recycle() }
                    val frame = copyFrame(scenario, view)
                    try {
                        assertTrue("Search field was not laid out", bounds.width > 0f && bounds.height > 0f)
                        val y = (bounds.top + 22.5f * density).toInt()
                        val base = frame.getPixel((bounds.left + density).toInt(), y)
                        val actual = frame.getPixel((bounds.left + bounds.width * 0.7f).toInt(), y)
                        for (channel in listOf(AndroidColor::red, AndroidColor::green, AndroidColor::blue)) {
                            val expected = channel(fillColor) * 0.8f + channel(base) * 0.2f
                            assertEquals("Search masks the top bar with blur=$blur progressive=$progressive", expected, channel(actual).toFloat(), 3f)
                        }
                    } finally {
                        frame.recycle()
                    }
                }
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun aboutPageRendersAfterRepeatedEntryWithWallpaperAndBlurChanges() {
        val visible = mutableStateOf(false)
        val wallpaperEnabled = mutableStateOf(false)
        val wallpaperSelected = mutableStateOf(false)
        var surfaceArgb = 0
        val blurEnabled = mutableStateOf(false)
        val source = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.RED)
        }
        val image = source.asImageBitmap()
        lateinit var view: ComposeView
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    view = ComposeView(activity).apply {
                        setContent {
                            MeloxTheme(AppSettings(blurEnabled = blurEnabled.value)) {
                                surfaceArgb = MiuixTheme.colorScheme.surface.toArgb()
                                CompositionLocalProvider(
                                    LocalCustomPageBackground provides image.takeIf { wallpaperEnabled.value },
                                    LocalTopBarBlurSettings provides TopBarBlurSettings(blurEnabled.value, true),
                                ) {
                                    FixedPageBackgroundHost(Modifier.fillMaxSize(), refreshSignal = { 0f }) {
                                        if (visible.value) {
                                            AboutScreen(
                                                bottomContentPadding = 0.dp,
                                                customBackgroundSelected = wallpaperSelected.value,
                                                onBack = {},
                                                onOpenSponsor = {},
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    activity.setContentView(view)
                }
                repeat(2) {
                    for ((selected, loaded, blur) in listOf(
                        Triple(true, false, true),
                        Triple(true, true, true),
                        Triple(true, true, false),
                        Triple(true, false, false),
                        Triple(false, false, true),
                    )) {
                        scenario.onActivity { visible.value = false }
                        copyFrame(scenario, view).recycle()
                        scenario.onActivity {
                            wallpaperSelected.value = selected
                            wallpaperEnabled.value = loaded
                            blurEnabled.value = blur
                            visible.value = true
                        }
                        // PixelCopy waits for actual RenderThread frames, exposing recursive backdrop drawing.
                        repeat(3) {
                            val frame = copyFrame(scenario, view)
                            try {
                                if (selected && !loaded) {
                                    assertEquals("OS3 must stay absent while wallpaper loads", surfaceArgb, frame.getPixel(1, frame.height / 3))
                                }
                            } finally {
                                frame.recycle()
                            }
                        }
                        scenario.onActivity { assertTrue("About view detached unexpectedly", view.isAttachedToWindow) }
                    }
                }
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun aboutWallpaperTopBarUsesTheSameBackgroundPolicyAsOtherPages() {
        val blurEnabled = mutableStateOf(true)
        val progressiveEnabled = mutableStateOf(false)
        val source = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.RED)
        }
        val image = source.asImageBitmap()
        lateinit var view: ComposeView
        var sampleY = 0
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    view = ComposeView(activity).apply {
                        setContent {
                            MeloxTheme(AppSettings(blurEnabled = blurEnabled.value)) {
                                CompositionLocalProvider(
                                    LocalCustomPageBackground provides image,
                                    LocalTopBarBlurSettings provides TopBarBlurSettings(blurEnabled.value, progressiveEnabled.value),
                                ) {
                                    FixedPageBackgroundHost(Modifier.fillMaxSize(), refreshSignal = { 0f }) {
                                        AboutScreen(
                                            bottomContentPadding = 0.dp,
                                            customBackgroundSelected = true,
                                            onBack = {},
                                            onOpenSponsor = {},
                                        )
                                    }
                                }
                            }
                        }
                    }
                    activity.setContentView(view)
                }
                copyFrame(scenario, view).recycle()
                scenario.onActivity { activity ->
                    val statusBarHeight = ViewCompat.getRootWindowInsets(view)
                        ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
                    sampleY = statusBarHeight + (24 * activity.resources.displayMetrics.density).toInt()
                }
                for ((blur, progressive) in listOf(true to false, false to false, true to true, false to true)) {
                    scenario.onActivity {
                        blurEnabled.value = blur
                        progressiveEnabled.value = progressive
                    }
                    val frame = copyFrame(scenario, view)
                    try {
                        val pixel = frame.getPixel(1, sampleY)
                        if (blur && progressive) {
                            assertTrue("About top bar bypassed the shared progressive effect", pixel != AndroidColor.RED)
                        } else {
                            assertEquals("About top bar must preserve the page wallpaper without extra default blur", AndroidColor.RED, pixel)
                        }
                    } finally {
                        frame.recycle()
                    }
                }
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun sheetRoleCardKeepsItsMaterialWhenPageBlurAndOpacityChange() {
        val opacity = mutableFloatStateOf(0f)
        val blurRadius = mutableFloatStateOf(0f)
        var bounds = ComposeRect.Zero
        val source = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.RED)
        }
        val image = source.asImageBitmap()
        lateinit var view: ComposeView
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    view = ComposeView(activity).apply {
                        setContent {
                            MeloxTheme(AppSettings()) {
                                val backdrop = rememberLayerBackdrop { drawRect(Color.Green) }
                                Box(Modifier.fillMaxSize().background(Color.Green), contentAlignment = Alignment.Center) {
                                    Box(Modifier.matchParentSize().layerBackdrop(backdrop))
                                    CompositionLocalProvider(
                                        LocalCustomPageBackground provides image,
                                        LocalPageSurfaceBackdrop provides backdrop,
                                        LocalPageCardSurfaceAlpha provides opacity.floatValue,
                                        LocalPageCardBlurRadius provides blurRadius.floatValue,
                                    ) {
                                        PageCard(
                                            modifier = Modifier.width(220.dp).onGloballyPositioned { bounds = it.boundsInRoot() },
                                            colors = CardDefaults.defaultColors(color = Color.Blue.copy(alpha = 0.6f)),
                                            usePageMaterial = false,
                                        ) {
                                            Box(Modifier.fillMaxWidth().height(80.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    activity.setContentView(view)
                }
                val before = copyFrame(scenario, view)
                try {
                    assertTrue("Sheet Card was not laid out", bounds.width > 0f && bounds.height > 0f)
                    val y = bounds.center.y.toInt()
                    val xs = listOf(0.25f, 0.5f, 0.75f).map { (bounds.left + bounds.width * it).toInt() }
                    assertTrue("Sheet material must remain visible", AndroidColor.blue(before.getPixel(xs[1], y)) > 0)
                    for ((alpha, radius) in listOf(0.8f to 25f, 0f to 100f, 1f to 0f)) {
                        scenario.onActivity {
                            opacity.floatValue = alpha
                            blurRadius.floatValue = radius
                        }
                        val after = copyFrame(scenario, view)
                        try {
                            xs.forEach { x ->
                                assertEquals("Page adjustments changed the sheet Card", before.getPixel(x, y), after.getPixel(x, y))
                            }
                        } finally {
                            after.recycle()
                        }
                    }
                } finally {
                    before.recycle()
                }
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun sharpBarsSampleTheFixedWallpaperWithBlurDisabled() {
        checkMovingSample(blurEnabled = false, sampleCard = false)
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun cardMaterialResamplesInsideAMovingParentGraphicsLayer() {
        checkMovingSample(blurEnabled = true, sampleCard = true)
    }

    @Test
    fun topBarWallpaperResamplesDuringEdgeMotionWithUnchangedPagerProgress() {
        checkMovingSample(blurEnabled = false, sampleCard = false, constantPagerProgress = true)
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun blurredMaterialResamplesDuringEdgeMotionWithUnchangedPagerProgress() {
        checkMovingSample(blurEnabled = true, sampleCard = true, constantPagerProgress = true)
    }

    @Test
    fun playlistCardOpacityReachesPaddingAndLabelAreaWithBlurDisabled() {
        val opacity = mutableFloatStateOf(0.8f)
        var bounds = ComposeRect.Zero
        var density = 1f
        val source = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.RED)
        }
        val image = source.asImageBitmap()
        lateinit var view: ComposeView
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    density = activity.resources.displayMetrics.density
                    view = ComposeView(activity).apply {
                        setContent {
                            MeloxTheme(AppSettings(blurEnabled = false)) {
                                CompositionLocalProvider(
                                    LocalCustomPageBackground provides image,
                                    LocalPageSurfaceBackdrop provides null,
                                    LocalPageCardSurfaceAlpha provides opacity.floatValue,
                                ) {
                                    Box(
                                        Modifier.fillMaxSize().background(Color.Red),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        PlaylistGridItem(
                                            playlist = LocalPlaylist("test", "Playlist", 0L, 0L),
                                            onClick = {},
                                            modifier = Modifier.width(220.dp)
                                                .onGloballyPositioned { bounds = it.boundsInRoot() },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    activity.setContentView(view)
                }
                val before = copyFrame(scenario, view)
                try {
                    assertTrue("Playlist Card was not laid out", bounds.width > 0f && bounds.height > 0f)
                    val samples = listOf(
                        (bounds.left + bounds.width * 0.5f).toInt() to (bounds.bottom - 4f * density).toInt(),
                        (bounds.left + bounds.width * 0.9f).toInt() to (bounds.bottom - 24f * density).toInt(),
                    )
                    scenario.onActivity { opacity.floatValue = 0f }
                    val after = copyFrame(scenario, view)
                    try {
                        for ((x, y) in samples) {
                            assertTrue("Initial Card fill must be visible", before.getPixel(x, y) != AndroidColor.RED)
                            assertEquals("An inner playlist fill masks Card opacity", AndroidColor.RED, after.getPixel(x, y))
                        }
                    } finally {
                        after.recycle()
                    }
                } finally {
                    before.recycle()
                }
            }
        } finally {
            source.recycle()
        }
    }

    private fun checkMovingSample(
        blurEnabled: Boolean,
        sampleCard: Boolean,
        constantPagerProgress: Boolean = false,
    ) {
        val movement = mutableFloatStateOf(0f)
        val cardCenter = mutableStateOf(0f)
        val source = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        Canvas(source).apply {
            drawColor(AndroidColor.RED)
            drawRect(200f, 0f, 400f, 400f, Paint().apply { color = AndroidColor.BLUE })
        }
        lateinit var view: ComposeView
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    view = ComposeView(activity).apply {
                        setContent {
                            MeloxTheme(AppSettings()) {
                                CompositionLocalProvider(
                                    LocalCustomPageBackground provides source.asImageBitmap(),
                                    LocalTopBarBlurSettings provides TopBarBlurSettings(blurEnabled, false),
                                ) {
                                    FixedPageBackgroundHost(
                                        modifier = Modifier.fillMaxSize(),
                                        refreshSignal = { if (constantPagerProgress) 0f else movement.floatValue },
                                    ) {
                                        // A cached moving ancestor reproduces the pager's layer handoff.
                                        Box(
                                            Modifier.fillMaxSize().graphicsLayer {
                                                translationX = movement.floatValue * size.width
                                            }.trackFixedWallpaperMotion(),
                                        ) {
                                            PageScaffold(
                                                topBar = { Box(Modifier.fillMaxWidth().height(80.dp)) },
                                            ) {
                                                Box(
                                                    Modifier.fillMaxSize()
                                                        .background(Color.Green)
                                                        .onSizeChanged { cardCenter.value = it.height / 2f },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (sampleCard) {
                                                        PageCard(Modifier.fillMaxWidth().height(80.dp)) {
                                                            Box(Modifier.fillMaxWidth().height(80.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    activity.setContentView(view)
                }
                val before = copyFrame(scenario, view)
                try {
                    val x = before.width * 35 / 100
                    val y = if (sampleCard) cardCenter.value.toInt() else 10
                    val expected = before.getPixel(x, y)
                    val opposite = before.getPixel(before.width * 65 / 100, y)
                    assertTrue("Source regions should be distinguishable", AndroidColor.red(expected) > AndroidColor.red(opposite) + 20)
                    for (offset in listOf(-0.25f, 0f, 0.25f, 0f)) {
                        scenario.onActivity { movement.floatValue = offset }
                        val after = copyFrame(scenario, view)
                        try {
                            for (sampleX in listOf(x, before.width * 65 / 100)) {
                                val actual = after.getPixel(sampleX, y)
                                val reference = before.getPixel(sampleX, y)
                                for (channel in listOf(AndroidColor::red, AndroidColor::green, AndroidColor::blue)) {
                                    assertEquals("Sample moved with its parent at offset $offset", channel(reference).toFloat(), channel(actual).toFloat(), 5f)
                                }
                            }
                        } finally {
                            after.recycle()
                        }
                    }
                } finally {
                    before.recycle()
                }
            }
        } finally {
            source.recycle()
        }
    }

    private fun copyFrame(scenario: ActivityScenario<MainActivity>, view: ComposeView): Bitmap {
        val ready = CountDownLatch(1)
        lateinit var bitmap: Bitmap
        var result = -1
        scenario.onActivity { activity ->
            view.postOnAnimation {
                view.postOnAnimation {
                    val location = IntArray(2)
                    view.getLocationInWindow(location)
                    bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    PixelCopy.request(
                        activity.window,
                        Rect(location[0], location[1], location[0] + view.width, location[1] + view.height),
                        bitmap,
                        { result = it; ready.countDown() },
                        Handler(Looper.getMainLooper()),
                    )
                }
            }
        }
        assertTrue("Frame was not copied", ready.await(5, TimeUnit.SECONDS))
        assertEquals(PixelCopy.SUCCESS, result)
        return bitmap
    }
}
