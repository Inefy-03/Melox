package com.melox.player

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melox.player.model.AppSettings
import com.melox.player.model.NavigationTransitionStyle
import com.melox.player.ui.component.library.LibrarySearchBar
import com.melox.player.ui.component.LocalGlassSearchQuery
import com.melox.player.ui.component.LocalTopBarBlurSettings
import com.melox.player.ui.component.PageScaffold
import com.melox.player.ui.component.TopBarBlurSettings
import com.melox.player.ui.theme.MeloxTheme
import com.melox.player.ui.navigation.PredictiveNavDisplay
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.navBackStackOf

@RunWith(AndroidJUnit4::class)
class SearchFocusLifecycleTest {
    private data class SearchRoute(val depth: Int) : NavKey

    @Test
    fun ordinaryBackClosesSearchBeforeRootExitOrDetailNavigation() {
        for (initialPredictiveEnabled in listOf(false, true)) {
            for (depth in 0..2) {
                val stack = navBackStackOf(*Array(depth + 1) { SearchRoute(it) })
                val visible = mutableStateOf(true)
                val active = mutableStateOf(true)
                val navigationEnabled = mutableStateOf(true)
                val predictiveEnabled = mutableStateOf(initialPredictiveEnabled)
                var closes = 0
                var closedDepth = -1
                var navigations = 0
                var fallbacks = 0
                lateinit var view: ComposeView
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    scenario.onActivity { activity ->
                        activity.onBackPressedDispatcher.addCallback(activity,
                            object : androidx.activity.OnBackPressedCallback(true) {
                                override fun handleOnBackPressed() { fallbacks++ }
                            },
                        )
                        view = ComposeView(activity).apply {
                            setContent {
                                MeloxTheme(AppSettings(blurEnabled = false)) {
                                    PredictiveNavDisplay(
                                        backStack = stack,
                                        predictiveBackEnabled = predictiveEnabled.value,
                                        transitionStyle = NavigationTransitionStyle.MIUIX,
                                        isDark = false,
                                        backEnabled = navigationEnabled.value,
                                        onBack = { navigations++ },
                                    ) {
                                        entry<SearchRoute> { route ->
                                            PageScaffold {
                                                LibrarySearchBar(
                                                    visible = visible.value,
                                                    focused = false,
                                                    queryState = rememberTextFieldState("retained query"),
                                                    label = "Search",
                                                    onFocusedChange = {},
                                                    onVisibleChange = {
                                                        visible.value = it
                                                        closes++
                                                        closedDepth = route.depth
                                                    },
                                                    backEnabled = active.value,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        activity.setContentView(view)
                    }
                    awaitFrames(scenario, view)
                    // Cover both predictive-off startup and a live toggle without replacing the host.
                    scenario.onActivity { predictiveEnabled.value = false }
                    awaitFrames(scenario, view)
                    scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
                    awaitFrames(scenario, view)
                    assertFalse(visible.value)
                    assertEquals(1, closes)
                    assertEquals("A covered entry intercepted search Back", depth, closedDepth)
                    assertEquals(0, navigations)
                    assertEquals(0, fallbacks)
                    scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
                    assertEquals(if (depth > 0) 1 else 0, navigations)
                    assertEquals(if (depth == 0) 1 else 0, fallbacks)

                    scenario.onActivity { visible.value = true; active.value = false }
                    awaitFrames(scenario, view)
                    scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
                    assertTrue("An inactive pager page consumed Back", visible.value)
                    assertEquals(1, closes)

                    scenario.onActivity { active.value = true; navigationEnabled.value = false }
                    awaitFrames(scenario, view)
                    val previousFallbacks = fallbacks
                    scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
                    assertTrue("Search consumed Back while player input owns the host", visible.value)
                    assertEquals(previousFallbacks + 1, fallbacks)
                    assertEquals(1, closes)
                }
            }
        }
    }

    @Test
    fun queryEditsKeepBothViewportsMountedAndDismissPreservesTheOutgoingDraft() {
        val expanded = mutableStateOf(false)
        val query = TextFieldState()
        val resultQueries = mutableListOf<String>()
        var browseMounts = 0
        var browseDisposals = 0
        var resultMounts = 0
        var resultDisposals = 0
        var draftOnDisposal: String? = null
        lateinit var view: ComposeView
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                view = ComposeView(activity).apply {
                    setContent {
                        MeloxTheme(AppSettings(blurEnabled = false)) {
                            PageScaffold(topBar = {
                                LibrarySearchBar(
                                    visible = expanded.value,
                                    focused = false,
                                    queryState = query,
                                    label = "Search",
                                    onFocusedChange = {},
                                    onVisibleChange = { expanded.value = it },
                                )
                            }) {
                                val draft = LocalGlassSearchQuery.current
                                if (draft == null) {
                                    DisposableEffect(Unit) {
                                        browseMounts++
                                        onDispose { browseDisposals++ }
                                    }
                                } else {
                                    DisposableEffect(Unit) {
                                        resultMounts++
                                        onDispose {
                                            resultDisposals++
                                            draftOnDisposal = query.text.toString()
                                        }
                                    }
                                    SideEffect { resultQueries.add(draft) }
                                }
                            }
                        }
                    }
                }
                activity.setContentView(view)
            }
            awaitFrames(scenario, view)
            scenario.onActivity { expanded.value = true }
            awaitFrames(scenario, view, count = 36)
            for (draft in listOf("test", "", "final query")) {
                scenario.onActivity { query.setTextAndPlaceCursorAtEnd(draft) }
                awaitFrames(scenario, view, count = 4)
                scenario.onActivity { assertEquals(draft, resultQueries.last()) }
            }
            scenario.onActivity {
                resultQueries.clear()
                expanded.value = false
            }
            awaitFrames(scenario, view, count = 85)
            scenario.onActivity {
                assertEquals(1, browseMounts)
                assertEquals(0, browseDisposals)
                assertEquals(1, resultMounts)
                assertEquals(1, resultDisposals)
                assertEquals("final query", draftOnDisposal)
                assertEquals("", query.text.toString())
                assertTrue(resultQueries.all { it == "final query" })
            }
        }
    }

    @Test
    fun reopeningSearchClearsThePageQuery() {
        val mounted = mutableStateOf(true)
        val query = TextFieldState("previous query")
        lateinit var view: ComposeView
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                view = ComposeView(activity).apply {
                    setContent {
                        MeloxTheme(AppSettings(blurEnabled = false)) {
                            CompositionLocalProvider(
                                LocalTopBarBlurSettings provides TopBarBlurSettings(false),
                            ) {
                                PageScaffold {
                                    if (mounted.value) {
                                        LibrarySearchBar(
                                            visible = true,
                                            focused = false,
                                            queryState = query,
                                            label = "Search",
                                            onFocusedChange = {},
                                            onVisibleChange = {},
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                activity.setContentView(view)
            }
            awaitFrames(scenario, view)
            assertEquals("", query.text.toString())
            scenario.onActivity { mounted.value = false }
            awaitFrames(scenario, view, count = 3)
            scenario.onActivity {
                query.setTextAndPlaceCursorAtEnd("previous query")
                mounted.value = true
            }
            awaitFrames(scenario, view)
            assertEquals("", query.text.toString())
        }
    }

    private fun awaitFrames(
        scenario: ActivityScenario<MainActivity>,
        view: ComposeView,
        count: Int = 20,
    ) {
        val ready = CountDownLatch(1)
        var imeVisible = false
        scenario.onActivity {
            view.postOnAnimation(object : Runnable {
                var remaining = count
                override fun run() {
                    imeVisible = imeVisible ||
                        (ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true)
                    if (--remaining == 0) ready.countDown() else view.postOnAnimation(this)
                }
            })
        }
        assertTrue("Frames did not finish", ready.await(5, TimeUnit.SECONDS))
        assertFalse("Unfocused search transiently showed the IME", imeVisible)
    }
}
