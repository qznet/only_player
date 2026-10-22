// Generated from MingCute Core SVG assets. Source: https://github.com/mingcute-design/mingcute-icons
// Do not edit paths by hand.
package one.only.player.core.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

internal val MingCuteRainbow: ImageVector by lazy {
    ImageVector.Builder(
        name = "MingCute.Rainbow",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = false,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString("M2 17V16C2 10.4772 6.47715 6 12 6C17.5228 6 22 10.4772 22 16V17M18 17V16C18 12.6863 15.3137 10 12 10C8.68629 10 6 12.6863 6 16V17M10 17V16C10 14.8954 10.8954 14 12 14C13.1046 14 14 14.8954 14 16V17").toNodes(),
            pathFillType = PathFillType.NonZero,
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Miter,
        )
    }.build()
}
