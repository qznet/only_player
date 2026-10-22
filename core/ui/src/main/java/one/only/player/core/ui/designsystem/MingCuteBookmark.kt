// 来自 MingCute 官方 SVG 资源：https://github.com/mingcute-design/mingcute-icons
// 路径数据由工具生成，不手动修改
package one.only.player.core.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

internal val MingCuteBookmark: ImageVector by lazy {
    ImageVector.Builder(
        name = "MingCute.Bookmark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = false,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M6 6a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v14.066a.5.5 0 0 1-.777.416l-4.946-3.297a.5.5 0 0 0-.554 0l-4.946 3.297A.5.5 0 0 1 6 20.066z",
            ).toNodes(),
            pathFillType = PathFillType.NonZero,
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
        )
    }.build()
}
