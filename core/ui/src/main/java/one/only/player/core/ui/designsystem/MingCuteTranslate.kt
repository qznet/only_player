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

internal val MingCuteTranslate: ImageVector by lazy {
    ImageVector.Builder(
        name = "MingCute.Translate",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = false,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M4 6h10M9 4v2m3 0c0 4-3 8-8 10m2.559-7c.985 2.628 3.237 5.024 6.441 6.561M11 20l4.5-10L20 20m-7.65-3h6.3",
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
