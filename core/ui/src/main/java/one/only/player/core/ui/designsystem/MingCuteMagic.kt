// 来自 MingCute 官方 SVG 资源：https://github.com/mingcute-design/mingcute-icons
// 路径数据由工具生成，不手动修改
package one.only.player.core.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

internal val MingCuteMagic: ImageVector by lazy {
    ImageVector.Builder(
        name = "MingCute.Magic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = false,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "m15 15l5.418 5.416m-15.2-15.2l5.279 1.393l4.59-2.956l.307 5.451l4.23 3.453l-5.09 1.976l-1.976 5.09l-3.452-4.23l-5.451-.307l2.956-4.59z",
            ).toNodes(),
            pathFillType = PathFillType.NonZero,
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
}
