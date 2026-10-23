@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package one.only.player.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import one.only.player.core.ui.R as UiR
import one.only.player.core.ui.components.isTvDevice
import one.only.player.core.ui.components.surfaceBlur
import one.only.player.core.ui.components.tvFocusRing
import one.only.player.core.ui.designsystem.AppIcons
import one.only.player.core.ui.extensions.LocalRootBottomBarPadding
import one.only.player.ui.component.FloatingBottomBar
import one.only.player.ui.component.FloatingBottomBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 根 Tab 定义，每项对应一个顶级导航目的地
enum class RootDestination(
    val labelRes: Int,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
    val tag: String,
) {
    HOME(UiR.string.tab_home, AppIcons.HomeLine, AppIcons.HomeFilled, "root_tab_home"),
    CLOUD(UiR.string.tab_cloud, AppIcons.CloudLine, AppIcons.CloudFilled, "root_tab_cloud"),
    FAVORITES(UiR.string.tab_favorites, AppIcons.FavoritesLine, AppIcons.FavoritesFilled, "root_tab_favorites"),
    SETTINGS(UiR.string.tab_settings, AppIcons.SettingsLine, AppIcons.SettingsFilled, "root_tab_settings"),
}

@Serializable
data object RootPagerRoute

// 底栏与分页只渲染可见目的地，隐藏项连页面一起移除
@Composable
fun rememberVisibleRootDestinations(shouldShowCloudTab: Boolean): List<RootDestination> = remember(shouldShowCloudTab) {
    RootDestination.entries.filter { it != RootDestination.CLOUD || shouldShowCloudTab }
}

@Composable
fun RootScaffold(
    rootNavigationState: RootNavigationState,
    modifier: Modifier = Modifier,
    shouldUseFloatingNavigationBar: Boolean = false,
    shouldBlurFloatingNavigationBar: Boolean = true,
    shouldShowBottomBar: Boolean = true,
    content: @Composable (RootDestination) -> Unit,
) {
    val destinations = rootNavigationState.destinations
    val currentPage = rootNavigationState.pagerState.currentPage
    LaunchedEffect(currentPage) {
        rootNavigationState.syncPage()
    }
    LaunchedEffect(destinations) {
        rootNavigationState.clampToDestinations()
    }
    if (!shouldShowBottomBar) {
        Box(modifier = modifier.fillMaxSize()) {
            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = rootNavigationState.pagerState,
                // 首帧只组合首页，优先显示已缓存的媒体内容。
                beyondViewportPageCount = 0,
                key = { page -> destinations[page] },
            ) { page ->
                content(destinations[page])
            }
        }
        return
    }

    val bottomBarPadding = rememberRootBottomBarPadding(shouldUseFloatingNavigationBar)
    val floatingBlurBackdrop = rememberRootBlurBackdrop(
        shouldBlurNavigationBar = shouldBlurFloatingNavigationBar,
    )

    // 底栏 UP 的落点：当前页内容容器。底栏与分页是兄弟节点，几何搜索不会可靠地跨回去，
    // 焦点一到这排就回不到上面的列表；显式交给它才回得去。
    // 容器自身也可聚焦：FocusRequester 只能落在可聚焦节点上，否则 requestFocus 无效。
    val pageContentFocusRequester = remember { FocusRequester() }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier
                .fillMaxSize()
                .then(if (floatingBlurBackdrop != null) Modifier.layerBackdrop(floatingBlurBackdrop) else Modifier),
            state = rootNavigationState.pagerState,
            // 首帧只组合首页，优先显示已缓存的媒体内容。
            beyondViewportPageCount = 0,
            key = { page -> destinations[page] },
        ) { page ->
            CompositionLocalProvider(LocalRootBottomBarPadding provides bottomBarPadding) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(pageContentFocusRequester)
                        .focusable(),
                ) {
                    content(destinations[page])
                }
            }
        }
        RootBottomBar(
            currentRoot = rootNavigationState.selectedDestination,
            destinations = destinations,
            shouldUseFloatingNavigationBar = shouldUseFloatingNavigationBar,
            floatingBlurBackdrop = floatingBlurBackdrop,
            onTabSelected = rootNavigationState::animateTo,
            modifier = Modifier.align(Alignment.BottomCenter),
            contentFocusRequester = pageContentFocusRequester,
        )
    }
}

@Composable
fun rememberRootBottomBarPadding(
    shouldUseFloatingNavigationBar: Boolean,
): PaddingValues {
    // 内容区底部预留：系统导航栏 + 底栏高度，避免导航栏遮挡内容。
    val navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navigationBarHeight = if (shouldUseFloatingNavigationBar) FLOATING_NAV_BAR_RESERVED_HEIGHT else NAV_BAR_CONTENT_HEIGHT
    return remember(navigationBarsBottom, navigationBarHeight) {
        PaddingValues(bottom = navigationBarsBottom + navigationBarHeight)
    }
}

@Composable
fun rememberRootBlurBackdrop(
    shouldBlurNavigationBar: Boolean,
): LayerBackdrop? {
    if (!shouldBlurNavigationBar || !isRuntimeShaderSupported()) return null

    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

@Composable
fun RootBottomBar(
    currentRoot: RootDestination,
    destinations: List<RootDestination>,
    shouldUseFloatingNavigationBar: Boolean,
    floatingBlurBackdrop: Backdrop?,
    modifier: Modifier = Modifier,
    contentFocusRequester: FocusRequester? = null,
    onTabSelected: (RootDestination) -> Unit,
) {
    if (shouldUseFloatingNavigationBar) {
        FloatingRootBottomBar(
            currentRoot = currentRoot,
            destinations = destinations,
            blurBackdrop = floatingBlurBackdrop,
            modifier = modifier,
            contentFocusRequester = contentFocusRequester,
            onTabSelected = onTabSelected,
        )
        return
    }

    val isBlurEnabled = floatingBlurBackdrop != null
    val barColor = if (isBlurEnabled) Color.Transparent else MiuixTheme.colorScheme.surface
    val isTv = isTvDevice()
    val bottomBarFocusRequester = remember { FocusRequester() }
    if (isTv) {
        // TV 上请求初始焦点到当前选中的 Tab，使遥控器 DPAD 能开始导航（Tab 本身已是 clickable 可聚焦）。
        LaunchedEffect(Unit) { bottomBarFocusRequester.requestFocus() }
    }
    NavigationBar(
        modifier = modifier.surfaceBlur(floatingBlurBackdrop),
        color = barColor,
    ) {
        destinations.forEach { target ->
            RootNavigationBarItem(
                destination = target,
                isSelected = currentRoot == target,
                onClick = { onTabSelected(target) },
                focusRequester = if (currentRoot == target) bottomBarFocusRequester else null,
                upFocusRequester = contentFocusRequester,
            )
        }
    }
}

@Composable
private fun FloatingRootBottomBar(
    currentRoot: RootDestination,
    destinations: List<RootDestination>,
    blurBackdrop: Backdrop?,
    modifier: Modifier = Modifier,
    contentFocusRequester: FocusRequester? = null,
    onTabSelected: (RootDestination) -> Unit,
) {
    val navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // isBlurEnabled 为 false 时 backdrop 不被采样，兜底一个空 backdrop 即可
    val fallbackBackdrop = rememberLayerBackdrop { drawContent() }
    val backdrop = blurBackdrop ?: fallbackBackdrop
    val selectedIndex = destinations.indexOf(currentRoot).coerceAtLeast(0)
    val isTv = isTvDevice()
    val bottomBarFocusRequester = remember { FocusRequester() }
    if (isTv) {
        // TV 上请求初始焦点到当前选中的 Tab，使遥控器 DPAD 能开始导航。
        LaunchedEffect(Unit) { bottomBarFocusRequester.requestFocus() }
    }

    FloatingBottomBar(
        modifier = modifier.padding(bottom = navigationBarsBottom + 12.dp),
        selectedIndex = { selectedIndex },
        onSelected = { index -> destinations.getOrNull(index)?.let(onTabSelected) },
        backdrop = backdrop,
        tabsCount = destinations.size,
        isBlurEnabled = blurBackdrop != null,
    ) {
        destinations.forEach { target ->
            val label = stringResource(target.labelRes)
                FloatingBottomBarItem(
                    onClick = { onTabSelected(target) },
                    modifier = Modifier
                        .defaultMinSize(minWidth = 76.dp)
                        .testTag(target.tag)
                        .then(if (currentRoot == target) Modifier.focusRequester(bottomBarFocusRequester) else Modifier)
                        .focusProperties { up = contentFocusRequester ?: FocusRequester.Default }
                        .tvFocusRing(shape = RoundedCornerShape(16.dp)),
            ) {
                // 图标恒用 onSurface，选中态由上层 tint 采样药丸表现
                Icon(
                    imageVector = if (currentRoot == target) target.selectedIcon else target.unselectedIcon,
                    contentDescription = label,
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = label,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RowScope.RootNavigationBarItem(
    destination: RootDestination,
    isSelected: Boolean,
    itemHeight: androidx.compose.ui.unit.Dp = NAV_BAR_CONTENT_HEIGHT,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
) {
    val tint = if (isSelected) {
        MiuixTheme.colorScheme.onSurfaceContainer
    } else {
        MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.45f)
    }
    val label = stringResource(destination.labelRes)
    val focusModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier

    Column(
        modifier = Modifier
            .height(itemHeight)
            .weight(1f)
            .testTag(destination.tag)
            .then(focusModifier)
            // 底栏与分页内容是兄弟节点，UP 的几何搜索不会可靠地跨回去，焦点一到这排就回不去上面的列表。
            // 显式把 UP 交给当前页首行，遥控器才能从底栏回到文件列表。
            .focusProperties { up = upFocusRequester ?: FocusRequester.Default }
            .tvFocusRing(shape = RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = label,
            color = tint,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val NAV_BAR_CONTENT_HEIGHT = 72.dp
private val FLOATING_NAV_BAR_RESERVED_HEIGHT = 76.dp
