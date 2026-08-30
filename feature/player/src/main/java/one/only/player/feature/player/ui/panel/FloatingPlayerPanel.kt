package one.only.player.feature.player.ui.panel

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlin.math.roundToInt
import one.only.player.core.ui.R
import one.only.player.core.ui.designsystem.AppIcons
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalFloatingPlayerPanelState = compositionLocalOf<FloatingPlayerPanelState?> { null }

internal val LocalFloatingPlayerPanelOnDismiss = compositionLocalOf<(() -> Unit)?> { null }

internal object FloatingPlayerPanelDefaults {
    val Margin = 12.dp
    val MinWidth = 280.dp
    val MinHeight = 220.dp
    val MaxPortraitWidth = 560.dp
    val MaxLandscapeWidth = 400.dp
    val Elevation = 16.dp
    val HeaderTopPadding = 10.dp
    const val DefaultPortraitHeightFraction = 0.45f
    const val DefaultLandscapeWidthFraction = 0.45f
}

@Stable
class FloatingPlayerPanelState {
    var viewportWidth by mutableIntStateOf(0)
        private set
    var viewportHeight by mutableIntStateOf(0)
        private set

    var intendedWidthPx by mutableFloatStateOf(Float.NaN)
        private set
    var intendedHeightPx by mutableFloatStateOf(Float.NaN)
        private set
    var intendedOffsetXPx by mutableFloatStateOf(Float.NaN)
        private set
    var intendedOffsetYPx by mutableFloatStateOf(Float.NaN)
        private set

    internal var lastResolved by mutableStateOf<FloatingPanelLayout?>(null)

    fun updateViewport(
        width: Int,
        height: Int,
    ) {
        if (width == viewportWidth && height == viewportHeight) return
        if (width <= 0 || height <= 0) return
        viewportWidth = width
        viewportHeight = height
    }

    fun reset() {
        intendedWidthPx = Float.NaN
        intendedHeightPx = Float.NaN
        intendedOffsetXPx = Float.NaN
        intendedOffsetYPx = Float.NaN
    }

    fun resetOffset() {
        intendedOffsetXPx = Float.NaN
        intendedOffsetYPx = Float.NaN
    }

    fun expandToMax() {
        val layout = lastResolved
        if (layout == null) {
            intendedWidthPx = Float.MAX_VALUE
            intendedHeightPx = Float.MAX_VALUE
            return
        }
        intendedWidthPx = layout.maxWidth.toFloat()
        intendedHeightPx = layout.maxHeight.toFloat()
    }

    fun expandHeightToMax() {
        val layout = lastResolved
        intendedHeightPx = layout?.maxHeight?.toFloat() ?: Float.MAX_VALUE
    }

    fun expandWidthToMax() {
        val layout = lastResolved
        intendedWidthPx = layout?.maxWidth?.toFloat() ?: Float.MAX_VALUE
    }

    fun shrinkToMin() {
        val layout = lastResolved
        if (layout == null) {
            intendedWidthPx = 0f
            intendedHeightPx = 0f
            return
        }
        intendedWidthPx = layout.minWidth.toFloat()
        intendedHeightPx = layout.minHeight.toFloat()
    }

    fun moveToCenter() {
        val layout = lastResolved ?: return
        intendedOffsetXPx = ((layout.contentWidth - layout.width) / 2f).coerceAtLeast(0f)
        intendedOffsetYPx = ((layout.contentHeight - layout.height) / 2f).coerceAtLeast(0f)
    }

    internal fun beginMove(layout: FloatingPanelLayout) {
        if (intendedOffsetXPx.isSpecified()) return
        intendedOffsetXPx = (layout.x - layout.contentLeft).toFloat()
        intendedOffsetYPx = (layout.y - layout.contentTop).toFloat()
    }

    internal fun moveBy(
        dragX: Float,
        dragY: Float,
        layout: FloatingPanelLayout,
    ) {
        beginMove(layout)
        val maxX = (layout.contentWidth - layout.width).coerceAtLeast(0).toFloat()
        val maxY = (layout.contentHeight - layout.height).coerceAtLeast(0).toFloat()
        intendedOffsetXPx = (intendedOffsetXPx + dragX).coerceIn(0f, maxX)
        intendedOffsetYPx = (intendedOffsetYPx + dragY).coerceIn(0f, maxY)
    }

    fun applyDebugCommand(command: String): Boolean {
        when (command.trim().lowercase()) {
            "max" -> expandToMax()
            "min" -> shrinkToMin()
            "default" -> reset()
            "height_max" -> expandHeightToMax()
            "width_max" -> expandWidthToMax()
            else -> return false
        }
        return true
    }

    fun applyDebugMove(command: String): Boolean {
        when (command.trim().lowercase()) {
            "default" -> resetOffset()
            "center" -> moveToCenter()
            else -> return false
        }
        return true
    }

    fun debugSnapshot(): String {
        val layout = lastResolved
        return buildString {
            append("w=").append(layout?.width ?: -1)
            append(",h=").append(layout?.height ?: -1)
            append(",x=").append(layout?.x ?: -1)
            append(",y=").append(layout?.y ?: -1)
            append(",minW=").append(layout?.minWidth ?: -1)
            append(",minH=").append(layout?.minHeight ?: -1)
            append(",maxW=").append(layout?.maxWidth ?: -1)
            append(",maxH=").append(layout?.maxHeight ?: -1)
            append(",customSize=").append(intendedWidthPx.isSpecified() || intendedHeightPx.isSpecified())
            append(",customOffset=").append(intendedOffsetXPx.isSpecified() || intendedOffsetYPx.isSpecified())
            append(",portrait=").append(layout?.isPortrait ?: false)
            append(",viewport=").append(viewportWidth).append("x").append(viewportHeight)
        }
    }
}

@Composable
fun rememberFloatingPlayerPanelState(): FloatingPlayerPanelState = remember { FloatingPlayerPanelState() }

internal data class FloatingPanelLayout(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val minWidth: Int,
    val minHeight: Int,
    val maxWidth: Int,
    val maxHeight: Int,
    val contentLeft: Int,
    val contentTop: Int,
    val contentWidth: Int,
    val contentHeight: Int,
    val isPortrait: Boolean,
)

internal fun resolveFloatingPanelLayout(
    containerWidth: Int,
    containerHeight: Int,
    isPortrait: Boolean,
    isRtl: Boolean,
    density: Float,
    insetLeft: Int,
    insetTop: Int,
    insetRight: Int,
    insetBottom: Int,
    intendedWidthPx: Float,
    intendedHeightPx: Float,
    intendedOffsetXPx: Float,
    intendedOffsetYPx: Float,
): FloatingPanelLayout {
    val margin = (FloatingPlayerPanelDefaults.Margin.value * density).roundToInt()
    val contentLeft = insetLeft + margin
    val contentTop = insetTop + margin
    val contentRight = (containerWidth - insetRight - margin).coerceAtLeast(contentLeft)
    val contentBottom = (containerHeight - insetBottom - margin).coerceAtLeast(contentTop)
    val contentWidth = (contentRight - contentLeft).coerceAtLeast(0)
    val contentHeight = (contentBottom - contentTop).coerceAtLeast(0)

    val minWidth = minOf((FloatingPlayerPanelDefaults.MinWidth.value * density).roundToInt(), contentWidth)
        .coerceAtLeast(0)
    val minHeight = minOf((FloatingPlayerPanelDefaults.MinHeight.value * density).roundToInt(), contentHeight)
        .coerceAtLeast(0)
    val maxWidth = contentWidth
    val maxHeight = contentHeight

    val defaultWidth = if (isPortrait) {
        minOf(contentWidth, (FloatingPlayerPanelDefaults.MaxPortraitWidth.value * density).roundToInt())
    } else {
        minOf(
            (contentWidth * FloatingPlayerPanelDefaults.DefaultLandscapeWidthFraction).roundToInt(),
            (FloatingPlayerPanelDefaults.MaxLandscapeWidth.value * density).roundToInt(),
        )
    }.coerceIn(minWidth, maxWidth.coerceAtLeast(minWidth))

    val defaultHeight = if (isPortrait) {
        (contentHeight * FloatingPlayerPanelDefaults.DefaultPortraitHeightFraction).roundToInt()
    } else {
        contentHeight
    }.coerceIn(minHeight, maxHeight.coerceAtLeast(minHeight))

    val width = coercePanelDimension(intendedWidthPx, defaultWidth, minWidth, maxWidth)
    val height = coercePanelDimension(intendedHeightPx, defaultHeight, minHeight, maxHeight)

    val defaultOffsetX = if (isPortrait) {
        (contentWidth - width) / 2
    } else if (isRtl) {
        0
    } else {
        contentWidth - width
    }.coerceAtLeast(0)

    val defaultOffsetY = if (isPortrait) {
        contentHeight - height
    } else {
        (contentHeight - height) / 2
    }.coerceAtLeast(0)

    val maxOffsetX = (contentWidth - width).coerceAtLeast(0)
    val maxOffsetY = (contentHeight - height).coerceAtLeast(0)
    val offsetX = if (intendedOffsetXPx.isSpecified()) {
        intendedOffsetXPx.roundToInt().coerceIn(0, maxOffsetX)
    } else {
        defaultOffsetX
    }
    val offsetY = if (intendedOffsetYPx.isSpecified()) {
        intendedOffsetYPx.roundToInt().coerceIn(0, maxOffsetY)
    } else {
        defaultOffsetY
    }

    return FloatingPanelLayout(
        x = contentLeft + offsetX,
        y = contentTop + offsetY,
        width = width,
        height = height,
        minWidth = minWidth,
        minHeight = minHeight,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        contentLeft = contentLeft,
        contentTop = contentTop,
        contentWidth = contentWidth,
        contentHeight = contentHeight,
        isPortrait = isPortrait,
    )
}

@Composable
fun BoxScope.FloatingPlayerPanel(
    shouldShow: Boolean,
    title: String,
    panelState: FloatingPlayerPanelState,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    contentPadding: PaddingValues = PaddingValues(),
    navigationIcon: @Composable (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = LocalFloatingPlayerPanelOnDismiss.current,
    focusRequester: FocusRequester,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val configuration = LocalConfiguration.current
    val tokens = rememberPlayerPanelTokens()
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val fallbackWidth = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val fallbackHeight = with(density) { configuration.screenHeightDp.dp.roundToPx() }
    val containerWidth = panelState.viewportWidth.takeIf { it > 0 } ?: fallbackWidth
    val containerHeight = panelState.viewportHeight.takeIf { it > 0 } ?: fallbackHeight

    val layout = resolveFloatingPanelLayout(
        containerWidth = containerWidth,
        containerHeight = containerHeight,
        isPortrait = isPortrait,
        isRtl = isRtl,
        density = density.density,
        insetLeft = with(density) { safeDrawingPadding.calculateLeftPadding(layoutDirection).roundToPx() },
        insetTop = with(density) { safeDrawingPadding.calculateTopPadding().roundToPx() },
        insetRight = with(density) { safeDrawingPadding.calculateRightPadding(layoutDirection).roundToPx() },
        insetBottom = with(density) { safeDrawingPadding.calculateBottomPadding().roundToPx() },
        intendedWidthPx = panelState.intendedWidthPx,
        intendedHeightPx = panelState.intendedHeightPx,
        intendedOffsetXPx = panelState.intendedOffsetXPx,
        intendedOffsetYPx = panelState.intendedOffsetYPx,
    )
    SideEffect {
        if (panelState.lastResolved != layout) {
            panelState.lastResolved = layout
        }
    }

    val panelWidth = with(density) { layout.width.toDp() }
    val panelHeight = with(density) { layout.height.toDp() }
    val panelShape = RoundedCornerShape(tokens.containerCornerRadius)

    AnimatedVisibility(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset { IntOffset(layout.x, layout.y) },
        visible = shouldShow,
        enter = fadeIn() + scaleIn(initialScale = 0.96f),
        exit = fadeOut() + scaleOut(targetScale = 0.96f),
    ) {
        Box(
            modifier = modifier
                .then(
                    if (testTag != null) {
                        Modifier
                            .testTag(testTag)
                            .semantics { contentDescription = testTag }
                    } else {
                        Modifier.testTag("player_floating_panel")
                    },
                )
                .requiredSize(panelWidth, panelHeight)
                .shadow(
                    elevation = FloatingPlayerPanelDefaults.Elevation,
                    shape = panelShape,
                    clip = false,
                )
                .clip(panelShape)
                .background(tokens.containerColor)
                .border(1.dp, tokens.containerBorderColor, panelShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            MiuixTheme(colors = tokens.rememberPanelMiuixColors()) {
                MaterialTheme(colorScheme = tokens.rememberPanelMaterialColorScheme()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        PanelHeader(
                            title = title,
                            titleColor = tokens.contentColor,
                            navigationIcon = navigationIcon,
                            onDismiss = onDismiss,
                            onMove = { dragAmount ->
                                panelState.moveBy(dragAmount.x, dragAmount.y, layout)
                            },
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(contentPadding)
                                .focusRequester(focusRequester),
                            content = content,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(
    title: String,
    titleColor: Color,
    navigationIcon: @Composable (() -> Unit)?,
    onDismiss: (() -> Unit)?,
    onMove: (Offset) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = FloatingPlayerPanelDefaults.HeaderTopPadding, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigationIcon != null) {
            navigationIcon()
        } else {
            Spacer(modifier = Modifier.size(8.dp))
        }
        val latestOnMove = rememberUpdatedState(onMove)
        MiuixText(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        latestOnMove.value(dragAmount)
                    }
                },
            text = title,
            color = titleColor,
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        if (onDismiss != null) {
            val closeDescription = stringResource(R.string.player_panel_close)
            MiuixIconButton(
                modifier = Modifier.testTag("btn_player_panel_close"),
                onClick = onDismiss,
            ) {
                MiuixIcon(
                    imageVector = AppIcons.Close,
                    contentDescription = closeDescription,
                    tint = titleColor,
                )
            }
        } else {
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

private fun Float.isSpecified(): Boolean = !isNaN()

private fun coercePanelDimension(
    intended: Float,
    default: Int,
    min: Int,
    max: Int,
): Int {
    val boundedMax = max.coerceAtLeast(min)
    if (!intended.isSpecified()) return default.coerceIn(min, boundedMax)
    if (!intended.isFinite() || intended >= boundedMax.toFloat()) return boundedMax
    if (intended <= 0f) return min
    return intended.roundToInt().coerceIn(min, boundedMax)
}
