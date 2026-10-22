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

internal val MingCuteCode: ImageVector by lazy {
    ImageVector.Builder(
        name = "MingCute.Code",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = false,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M14.216 4.106L9.784 19.894M6.5 7.757L2.257 12L6.5 16.243m11-8.486L21.743 12L17.5 16.243",
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
