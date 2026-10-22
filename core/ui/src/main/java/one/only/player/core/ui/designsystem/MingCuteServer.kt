// 来自 MingCute 官方 SVG 资源：https://github.com/mingcute-design/mingcute-icons
// 路径数据由工具生成，不手动修改
package one.only.player.core.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

internal val MingCuteServer: ImageVector by lazy {
    ImageVector.Builder(
        name = "MingCute.Server",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = false,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M16 16a1 1 0 1 0 0 2zm.002 2a1 1 0 0 0 0-2zM13 16a1 1 0 1 0 0 2zm.002 2a1 1 0 0 0 0-2zM6 4v1h12V3H6zm13 1h-1v14h2V5zm-1 15v-1H6v2h12zM5 19h1V5H4v14zM5 9v1h14V8H5zm0 5v1h14v-2H5zm11 3v1h.002v-2H16zm-3 0v1h.002v-2H13zm-7 3v-1H4a2 2 0 0 0 2 2zm13-1h-1v2a2 2 0 0 0 2-2zM18 4v1h2a2 2 0 0 0-2-2zM6 4V3a2 2 0 0 0-2 2h2z",
            ).toNodes(),
            pathFillType = PathFillType.NonZero,
            fill = SolidColor(Color.Black),
            stroke = null,
        )
    }.build()
}
