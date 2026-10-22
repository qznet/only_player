package one.only.player.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

// 校准值按视频与字幕组合保存，外挂字幕使用 uri，内嵌字幕使用轨道标识
@Entity(
    tableName = "subtitle_calibration",
    primaryKeys = ["media_uri", "subtitle_key"],
    foreignKeys = [
        ForeignKey(
            entity = MediumStateEntity::class,
            parentColumns = ["uri"],
            childColumns = ["media_uri"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["media_uri"]),
    ],
)
data class SubtitleCalibrationEntity(
    @ColumnInfo(name = "media_uri")
    val mediaUri: String,
    @ColumnInfo(name = "subtitle_key")
    val subtitleKey: String,
    @ColumnInfo(name = "delay_ms")
    val delayMilliseconds: Long = 0,
    @ColumnInfo(name = "speed")
    val speed: Float = 1f,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
