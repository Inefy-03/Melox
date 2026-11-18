package com.melox.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melox.player.model.AppSettings
import com.melox.player.model.NavigationTransitionStyle
import com.melox.player.ui.component.library.LibrarySearchBar
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
                                                    query = "retained query",
                                                    label = "Search",
                                                    onQueryChange = {},
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
    fun returningToVisibleUnfocusedSearchNeverRequestsFocusOrShowsIme() {
        val mounted = mutableStateOf(true)
        val focused = mutableStateOf(false)
        val query = mutableStateOf("retained query")
        val focusRequests = mutableListOf<Boolean>()
        var bounds = Rect.Zero
        lateinit var view: ComposeView
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                view = ComposeView(activity).apply {
                    setContent {
                        MeloxTheme(AppSettings(blurEnabled = false)) {
                            CompositionLocalProvider(
                                LocalTopBarBlurSettings provides TopBarBlurSettings(false, false),
                            ) {
                                PageScaffold {
                                    if (mounted.value) {
                                        Box(Modifier.fillMaxWidth().onGloballyPositioned { bounds = it.boundsInRoot() }) {
                                            LibrarySearchBar(
                                                visible = true,
                                                focused = focused.value,
                                                query = query.value,
                                                label = "Search",
                                                onQueryChange = { query.value = it },
                                                onFocusedChange = {
                                                    focusRequests += it
                                                    focused.value = it
                                                },
                                                onVisibleChange = {},
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                activity.setContentView(view)
            }
            awaitFrames(scenario, view)
            repeat(2) {
                scenario.onActivity { mounted.value = false }
                awaitFrames(scenario, view, count = 3)
                scenario.onActivity { mounted.value = true }
                awaitFrames(scenario, view)
                scenario.onActivity {
                    assertTrue("Visible search field was not laid out", bounds.height > 0f)
                    assertFalse("Remount requested input focus", focusRequests.any { it })
                    assertFalse(focused.value)
                    assertEquals("retained query", query.value)
                }
            }
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
