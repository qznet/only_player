package one.only.player.core.data.mappers

import one.only.player.core.database.entities.SubtitleCalibrationEntity
import one.only.player.core.model.SubtitleCalibration

fun SubtitleCalibrationEntity.toSubtitleCalibration(): SubtitleCalibration = SubtitleCalibration(
    delayMilliseconds = delayMilliseconds,
    speed = speed,
)
