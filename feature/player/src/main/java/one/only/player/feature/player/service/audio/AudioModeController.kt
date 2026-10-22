@file:OptIn(UnstableApi::class)

package one.only.player.feature.player.service.audio

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import kotlin.math.abs
import one.only.player.core.model.PlayerPreferences

/**
 * Keeps the audio path on the mode the rate calls for: bitstream at 1x, decoded otherwise.
 *
 * Two halves, and neither works alone. The flag in the sink decides what the renderer is allowed
 * to accept; the reselect makes the renderer ask again, which is the only moment the answer is
 * read. A rate cannot go out before that: a bitstream cannot be stretched, so the old
 * configuration would be asked to do something it cannot.
 */
internal class AudioModeController(
    private val preferences: () -> PlayerPreferences,
    private val onReselectRequested: (delayMs: Long) -> Unit,
    private val onRateReady: (player: Player, speed: Float) -> Unit,
) {

    private companion object {
        private const val TAG = "AudioMode"
        /** Hard crossings come in pairs when a rate steps back and forth through 1x. */
        private const val MIN_INTERVAL_MS = 1200L
        private const val VERIFY_DELAY_MS = 700L
        /** Long enough for a rebuild to report back; past it the latch is presumed lost. */
        private const val LATCH_TIMEOUT_MS = 2100L
        private const val MAX_RETRIES = 2
        private const val NORMAL_TOLERANCE = 0.005f
    }

    private val handler = Handler(Looper.getMainLooper())

    /** Set by the renderers factory every time a player is built. */
    @Volatile
    private var sink: AudioPassthroughGateSink? = null

    private var player: Player? = null
    private var lastSwitchMs = 0L
    private var pendingSpeed = 0f
    private var retries = 0
    private var reselectInFlight = false

    /** Read by the service's onTracksChanged: true means the disable is still waiting to come back. */
    val isReselectInFlight: Boolean get() = reselectInFlight

    private val verifyRunnable = Runnable { verify() }

    fun attach(sink: AudioPassthroughGateSink) {
        this.sink = sink
    }

    fun attach(player: Player) {
        this.player = player
        reselectInFlight = false
        pendingSpeed = 0f
        retries = 0
        // A fresh player means a fresh sink: put it on the mode this player's rate asks for, before
        // anything configures it. Harmless if nothing has been configured yet.
        sink?.setForceDecode(!isBitstreamAllowed(player.playbackParameters.speed))
    }

    /**
     * The one place a rate is handed to the player.
     *
     * @return false when the rate could not go out yet — it waits for the rebuild, which is
     *     expected and not an error.
     */
    fun requestSpeed(player: Player, speed: Float): Boolean {
        this.player = player
        val sink = sink ?: return applyNow(player, speed)
        // Nothing loaded: there is no configuration to change, and the flag the sink was built with
        // already matches this rate (attach keeps it in step). Without this guard the rate the
        // service applies on STATE_IDLE would ask for a rebuild of a player holding no media.
        if (player.mediaItemCount == 0) return applyNow(player, speed)
        val prefs = preferences()
        if (!prefs.isAudioPassthroughEnabled) {
            // Everything decodes: one mode for the whole session, decided when the sink was built.
            sink.setForceDecode(true)
            return applyNow(player, speed)
        }
        if (!prefs.isPassthroughAtNormalSpeedOnly) {
            // The viewer chose the bitstream over the rate. A bitstream cannot be stretched, so a
            // rate other than 1x is simply not delivered — the panel's buttons stop doing anything,
            // which is what "keep the bitstream at every speed" means.
            sink.setForceDecode(false)
            if (!isNormal(speed)) {
                Log.i(TAG, "rate $speed ignored: the bitstream is kept at every speed")
                return false
            }
            return applyNow(player, speed)
        }
        val normal = isNormal(speed)
        // The return value is not the test: the flag may already hold this answer from an earlier
        // step on the same side of 1x. What matters is the configuration the sink is built with —
        // and a sink that has never been configured has none to change.
        sink.setForceDecode(!normal)
        if (!sink.hasConfigured || sink.isPassthrough == normal) return applyNow(player, speed)

        retries = 0
        pendingSpeed = speed
        Log.i(TAG, "mode=${if (normal) "pass-through" else "decoded"} at $speed playing=${player.isPlaying}")
        if (!player.isPlaying) {
            // Nothing is configuring the sink right now, and a reselect against a paused player
            // leaves a stale track behind. onIsPlayingChanged picks this up on resume.
            return false
        }
        val now = SystemClock.uptimeMillis()
        val since = now - lastSwitchMs
        if (since < MIN_INTERVAL_MS) {
            onReselectRequested(MIN_INTERVAL_MS - since)
            return false
        }
        lastSwitchMs = now
        reselect(player)
        return false
    }

    /** The rebuild has landed: hand the waiting rate over. */
    fun onReselectSettled(player: Player) {
        if (!reselectInFlight) return
        reselectInFlight = false
        if (pendingSpeed != 0f) {
            val speed = pendingSpeed
            pendingSpeed = 0f
            onRateReady(player, speed)
        }
        handler.removeCallbacks(verifyRunnable)
        handler.postDelayed(verifyRunnable, VERIFY_DELAY_MS)
    }

    /** Playback resumed: a mode chosen while paused could not reconfigure anything then. */
    fun onPlayingChanged(player: Player, isPlaying: Boolean) {
        if (!isPlaying || reselectInFlight || pendingSpeed == 0f) return
        reselect(player)
    }

    /**
     * A rebuild that a crossing inside [MIN_INTERVAL_MS] of the last one still owes, arriving when
     * the window is over. It does not re-read the rate: the rate it is waiting for is
     * [pendingSpeed], and the player still reports the old one — reading it back would decide the
     * opposite mode and undo what the second crossing asked for.
     */
    fun flushPendingReselect(player: Player) {
        if (reselectInFlight || pendingSpeed == 0f) return
        lastSwitchMs = SystemClock.uptimeMillis()
        reselect(player)
    }

    fun cancel() {
        handler.removeCallbacks(verifyRunnable)
        reselectInFlight = false
        pendingSpeed = 0f
        player = null
        sink = null
    }

    /**
     * Make the renderer configure the sink again, so the answer it asked is asked once more.
     * Disabling the type reaches the renderer's onDisabled and releases the track; onTracksChanged
     * puts the type back, rebuilt from that moment's parameters.
     */
    private fun reselect(player: Player) {
        if (reselectInFlight) return
        reselectInFlight = true
        Log.i(TAG, "reselect")
        // A latch with no exit disables the feature for the rest of the session: if the renderer
        // never reports back, this fires and lets it go again.
        handler.removeCallbacks(verifyRunnable)
        handler.postDelayed(verifyRunnable, LATCH_TIMEOUT_MS)
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
    }

    /**
     * Look at what the rebuild produced, a beat later. A select that never reported back is also
     * cleared here: its latch refuses every later reselect, so leaving it set would quietly
     * disable the feature for the rest of the session.
     */
    private fun verify() {
        val player = player ?: return
        val sink = sink ?: return
        if (reselectInFlight) {
            Log.w(TAG, "reselect never reported, releasing it")
            reselectInFlight = false
            if (!player.currentTracks.isTypeSelected(C.TRACK_TYPE_AUDIO)) {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .build()
            }
            return
        }
        val mime = audioMime(player) ?: return
        if (mime == MimeTypes.AUDIO_RAW) return
        val speed = player.playbackParameters.speed
        val wantDecode = !isNormal(speed)
        // isPassthrough and wantDecode are complementary: equal means the sink is bitstreaming a
        // rate that cannot be stretched (or decoding one that should not be), which is the failure.
        if (sink.isPassthrough != wantDecode) return
        if (!player.isPlaying) return
        if (retries >= MAX_RETRIES) {
            Log.w(TAG, "still on the wrong side of the bitstream at $speed after $retries retries")
            return
        }
        retries++
        pendingSpeed = speed
        lastSwitchMs = SystemClock.uptimeMillis()
        reselect(player)
    }

    private fun applyNow(player: Player, speed: Float): Boolean {
        // A rate that could go out now is a rate nothing is waiting for any more.
        pendingSpeed = 0f
        player.setPlaybackSpeed(speed)
        return false
    }

    private fun isNormal(speed: Float) = abs(speed - 1f) <= NORMAL_TOLERANCE

    private fun isBitstreamAllowed(speed: Float): Boolean {
        val prefs = preferences()
        if (!prefs.isAudioPassthroughEnabled) return false
        if (!prefs.isPassthroughAtNormalSpeedOnly) return true
        return isNormal(speed)
    }

    /**
     * The mime of the audio track that is actually playing. media3 1.11.x's `Tracks.Group` has
     * `type`, `length`, `isSelected` and `getTrackFormat(index)`, so the selected group's first
     * format is the answer.
     */
    private fun audioMime(player: Player): String? {
        val group = player.currentTracks.groups.firstOrNull {
            it.type == C.TRACK_TYPE_AUDIO && it.isSelected
        } ?: return null
        for (index in 0 until group.length) {
            group.getTrackFormat(index).sampleMimeType?.let { return it }
        }
        return null
    }
}
