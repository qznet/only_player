package one.only.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import one.only.player.core.database.entities.MediumStateEntity
import one.only.player.core.database.entities.SubtitleCalibrationEntity

@Dao
interface SubtitleCalibrationDao {

    @Upsert
    suspend fun upsert(calibration: SubtitleCalibrationEntity)

    @Upsert
    suspend fun upsertAll(calibrations: List<SubtitleCalibrationEntity>)

    @Query("SELECT * FROM subtitle_calibration WHERE media_uri = :mediaUri AND subtitle_key = :subtitleKey")
    suspend fun get(
        mediaUri: String,
        subtitleKey: String,
    ): SubtitleCalibrationEntity?

    @Query("SELECT * FROM subtitle_calibration WHERE media_uri = :mediaUri")
    suspend fun getByMediaUri(mediaUri: String): List<SubtitleCalibrationEntity>

    @Query("DELETE FROM subtitle_calibration WHERE media_uri = :mediaUri AND subtitle_key = :subtitleKey")
    suspend fun delete(
        mediaUri: String,
        subtitleKey: String,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaState(state: MediumStateEntity)

    @Transaction
    suspend fun getOrCreate(
        mediaUri: String,
        subtitleKey: String,
        trackIndex: Int,
    ): SubtitleCalibrationEntity {
        get(mediaUri, subtitleKey)?.let { return it }
        val legacy = get(mediaUri, "legacy:$trackIndex") ?: get(mediaUri, "legacy")
        val calibration = SubtitleCalibrationEntity(
            mediaUri = mediaUri,
            subtitleKey = subtitleKey,
            delayMilliseconds = legacy?.delayMilliseconds ?: 0L,
            speed = legacy?.speed ?: 1f,
            updatedAt = System.currentTimeMillis(),
        )
        save(calibration)
        if (legacy != null) delete(mediaUri, legacy.subtitleKey)
        return calibration
    }

    @Transaction
    suspend fun save(calibration: SubtitleCalibrationEntity) {
        insertMediaState(MediumStateEntity(uriString = calibration.mediaUri))
        upsert(calibration)
    }
}
