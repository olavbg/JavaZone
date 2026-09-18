package com.olavbg.javazone.ui.components

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.olavbg.javazone.model.BackgroundMode
import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Run the background ticker at about 30 fps instead of every frame. */
private const val TICK_RATE_MILLIS = 33L

/**
 * Delay between the reanimate call and the start of the band morph. Matches the scene
 * fade-in delay, so the bands start moving the moment the new screen begins to appear
 * rather than only after the transition has fully settled.
 */
private const val REANIMATE_START_DELAY_MILLIS = 350L

/** How long the smooth band-to-band morph takes. */
private const val MORPH_DURATION_MILLIS = 1200L

/**
 * Total time a reanimate occupies, from the call until the morph has fully played.
 * reanimate() simply returns when it is called sooner than this after the previous
 * start, so a burst of screen changes collapses into exactly one band morph.
 */
private const val REANIMATE_DURATION_MILLIS =
    REANIMATE_START_DELAY_MILLIS + MORPH_DURATION_MILLIS

/** Max rocking angle for the band tilt animation, degrees. */
private const val ROTATION_AMPLITUDE_DEG = 4f

/**
 * Signal consumed by [AnimatedDiagonalBackground]. Bump the value (e.g. on navigation)
 * to smoothly morph the bands into a new random configuration.
 */
val LocalBackgroundReanimate = compositionLocalOf { 0L }

/**
 * Reusable animated two-tone diagonal background.
 *
 * Paints an opaque [baseColor] fill and layers two softly blurred diagonal bands in
 * [tintPrimary] and [tintSecondary]. Each band gets a randomized angle, size, position
 * and drift, driven by an ever-increasing clock with incommensurate frequencies — so the
 * motion is continuous (no loop jump) and never visibly repeats.
 *
 * Performance design:
 *
 * - Each band is rendered ONCE into its own layer (gradient + rotation is baked into the
 *   recorded draw). The slow sine drift is applied as a pure GPU `graphicsLayer`
 *   translation, so nothing is re-recorded or re-rasterized between ticks. The only time
 *   a band layer re-records is during a short (1200 ms) morph between variations.
 * - The ticker only runs while the composition is RESUMED (pauses when backgrounded),
 *   saving both frame time and battery.
 * - If the system "remove animations" (reduce motion) setting is on, it renders a single
 *   static frame and never ticks at all.
 *
 * @param baseColor opaque backdrop painted underneath the bands.
 * @param tintPrimary first pastel tone (rendered as the dominant band).
 * @param tintSecondary second pastel tone (rendered as a lighter opposing band).
 * @param bandScale multiplier for the band peak alphas. Light themes pass a value > 1 so
 *   the bands stay clearly visible through the translucent white surfaces; dark themes keep
 *   the default 1f.
 * @param mode how the background is rendered: [BackgroundMode.Animated] (default) runs the
 *   full ticker, [BackgroundMode.Static] paints one frozen frame, and
 *   [BackgroundMode.None] skips the bands entirely.
 */
@Composable
fun AnimatedDiagonalBackground(
    baseColor: Color,
    tintPrimary: Color,
    tintSecondary: Color,
    modifier: Modifier = Modifier,
    mode: BackgroundMode = BackgroundMode.Animated,
    bandScale: Float = 1f
) {
    val context = LocalContext.current
    val animationsEnabled = remember { animatorScaleFactor(context) != 0f }

    // Per-composition band intensities (kept stable so the gradient brushes can be cached).
    val band1Alpha = remember { (0.38f + Random.nextFloat() * 0.10f) * bandScale }
    val band2Alpha = remember { (0.24f + Random.nextFloat() * 0.10f) * bandScale }

    val band1Brush = remember(tintPrimary, band1Alpha) {
        bandBrush(tintPrimary, band1Alpha)
    }
    val band2Brush = remember(tintSecondary, band2Alpha) {
        bandBrush(tintSecondary, band2Alpha)
    }

    val reanimateTick = LocalBackgroundReanimate.current

    // Geometric state. The band layers read all of these, so changing them only re-records
    // the band layers (during a morph) — it never triggers recomposition of the app.
    var fromVariation by remember { mutableStateOf(DiagonalVariation.random()) }
    var toVariation by remember { mutableStateOf(fromVariation) }
    var mix by remember { mutableFloatStateOf(1f) } // 1f == fully on `toVariation`

    // Drift offsets, updated each tick and applied as GPU-only layer translations.
    var band1OffsetX by remember { mutableFloatStateOf(0f) }
    var band1OffsetY by remember { mutableFloatStateOf(0f) }
    var band2OffsetX by remember { mutableFloatStateOf(0f) }
    var band2OffsetY by remember { mutableFloatStateOf(0f) }

    // Band tilt (angle animation), updated each tick and applied as a GPU rotation.
    var band1Rotation by remember { mutableFloatStateOf(0f) }
    var band2Rotation by remember { mutableFloatStateOf(0f) }

    // Morph clock in active-time seconds. <0 == no morph in flight; otherwise the moment
    // (in active time) the morph's fast phase starts playing.
    var morphStartElapsed by remember { mutableFloatStateOf(-1f) }

    // Wall-clock stamp of the last reanimate that actually started. reanimate() returns
    // early while less than REANIMATE_DURATION_MILLIS has passed since this stamp.
    var lastReanimateStartMillis by remember { mutableLongStateOf(-1L) }

    // Screen size captured once the layers lay out; used to scale drift to pixels.
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // reanimate(): called on every screen change. If a reanimate is still playing (less
    // than REANIMATE_DURATION_MILLIS since the previous start) it does nothing; otherwise
    // it kicks off a new random band morph.
    fun reanimate() {
        val now = SystemClock.elapsedRealtime()
        if (lastReanimateStartMillis >= 0L &&
            now - lastReanimateStartMillis < REANIMATE_DURATION_MILLIS
        ) {
            return
        }
        lastReanimateStartMillis = now
        fromVariation = toVariation
        toVariation = DiagonalVariation.random()
        mix = 0f
    }

    val animated = mode == BackgroundMode.Animated && animationsEnabled

    // A fresh random placement every time the bands are switched back into view from
    // "Ingen bånd", so toggling the setting always lands on a new layout instead of
    // rediscovering the previous one.
    var bandsWereShown by remember { mutableStateOf(mode != BackgroundMode.None) }
    LaunchedEffect(mode) {
        val shown = mode != BackgroundMode.None
        if (shown && !bandsWereShown) {
            fromVariation = DiagonalVariation.random()
            toVariation = fromVariation
            mix = 1f
        }
        bandsWereShown = shown
    }

    if (!animated) {
        // Single static frame with the ticker AND the reanimate listener both out of the
        // composition, so neither the drift clock nor the morph machinery runs while the
        // bands are hidden or frozen — nothing works in the background.
        Canvas(modifier = modifier) {
            drawRect(color = baseColor)
            if (mode == BackgroundMode.Static) {
                drawOrganicBandBase(band1Brush, toVariation.band1, size.width, size.height)
                drawOrganicBandBase(band2Brush, toVariation.band2, size.width, size.height)
            }
        }
        return
    }

    LaunchedEffect(reanimateTick) {
        if (reanimateTick != 0L) reanimate()
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val activeTime = remember { ActiveTimeAccumulator() }

    // Single ticker that advances both the drift clock and (during a morph) the morph
    // progress. It wakes only ~30 times per second, pauses when the app is backgrounded,
    // and only updates the GPU layer translations — the band layers never redraw.
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(TICK_RATE_MILLIS.milliseconds)

                val elapsed = activeTime.tick()

                if (mix < 1f && morphStartElapsed < 0f) {
                    // A reanimate just started: schedule the morph's fast phase after
                    // the start delay (kept inside active time so a pause doesn't jump it).
                    morphStartElapsed = elapsed + REANIMATE_START_DELAY_MILLIS / 1000f
                }
                if (morphStartElapsed >= 0f) {
                    val t = ((elapsed - morphStartElapsed) * 1000f / MORPH_DURATION_MILLIS)
                        .coerceIn(0f, 1f)
                    mix = FastOutSlowInEasing.transform(t)
                    if (t >= 1f) {
                        morphStartElapsed = -1f // settle on `toVariation`, keep mix = 1
                    }
                }

                val w = canvasSize.width.toFloat()
                val h = canvasSize.height.toFloat()
                if (w <= 0f || h <= 0f) continue

                // A morph interpolates the bands' drifted CENTERS (positions), not the drift
                // sine parameters. The old center is frozen at the morph's start and the new
                // center is evaluated live, so the visible sweep is one monotonic move that
                // can never add a second "kick" (an interpolated sine can swing + then -).
                // When the morph ends the band is exactly on the new spec's live center, so
                // the calm drift resumes without a jump.
                val morphActive = morphStartElapsed >= 0f
                val fromTime = if (morphActive) min(elapsed, morphStartElapsed) else elapsed

                val f1 = fromVariation.band1
                val t1 = toVariation.band1
                val a1x = (f1.posX + f1.driftX * driftMotion(f1, fromTime)).coerceIn(0f, 1f)
                val a1y = (f1.posY + f1.driftY * driftMotion(f1, fromTime)).coerceIn(0f, 1f)
                val b1x = (t1.posX + t1.driftX * driftMotion(t1, elapsed)).coerceIn(0f, 1f)
                val b1y = (t1.posY + t1.driftY * driftMotion(t1, elapsed)).coerceIn(0f, 1f)
                val c1x = lerp(a1x, b1x, mix)
                val c1y = lerp(a1y, b1y, mix)
                val d1x = lerp(f1.posX, t1.posX, mix)
                val d1y = lerp(f1.posY, t1.posY, mix)
                band1OffsetX = w * (c1x - d1x)
                band1OffsetY = h * (c1y - d1y)
                band1Rotation = ROTATION_AMPLITUDE_DEG * lerp(
                    rotationMotion(f1, fromTime), rotationMotion(t1, elapsed), mix
                )

                val f2 = fromVariation.band2
                val t2 = toVariation.band2
                val a2x = (f2.posX + f2.driftX * driftMotion(f2, fromTime)).coerceIn(0f, 1f)
                val a2y = (f2.posY + f2.driftY * driftMotion(f2, fromTime)).coerceIn(0f, 1f)
                val b2x = (t2.posX + t2.driftX * driftMotion(t2, elapsed)).coerceIn(0f, 1f)
                val b2y = (t2.posY + t2.driftY * driftMotion(t2, elapsed)).coerceIn(0f, 1f)
                val c2x = lerp(a2x, b2x, mix)
                val c2y = lerp(a2y, b2y, mix)
                val d2x = lerp(f2.posX, t2.posX, mix)
                val d2y = lerp(f2.posY, t2.posY, mix)
                band2OffsetX = w * (c2x - d2x)
                band2OffsetY = h * (c2y - d2y)
                band2Rotation = ROTATION_AMPLITUDE_DEG * lerp(
                    rotationMotion(f2, fromTime), rotationMotion(t2, elapsed), mix
                )
            }
        }
    }

    Box(modifier = modifier.onSizeChanged { canvasSize = it }) {
        // Opaque backdrop, drawn once.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = baseColor)
        }

        // Each band layer re-records only when `mix` changes (during a morph).
        // The slow drift is applied externally as a GPU layer translation, and the
        // tilt as a GPU rotation around the band's own center.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = band1OffsetX
                    translationY = band1OffsetY
                    rotationZ = band1Rotation
                    transformOrigin = TransformOrigin(
                        lerp(fromVariation.band1.posX, toVariation.band1.posX, mix) + band1OffsetX / size.width,
                        lerp(fromVariation.band1.posY, toVariation.band1.posY, mix) + band1OffsetY / size.height
                    )
                }
        ) {
            drawOrganicBandBase(
                band1Brush,
                lerp(fromVariation.band1, toVariation.band1, mix),
                size.width,
                size.height
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = band2OffsetX
                    translationY = band2OffsetY
                    rotationZ = band2Rotation
                    transformOrigin = TransformOrigin(
                        lerp(fromVariation.band2.posX, toVariation.band2.posX, mix) + band2OffsetX / size.width,
                        lerp(fromVariation.band2.posY, toVariation.band2.posY, mix) + band2OffsetY / size.height
                    )
                }
        ) {
            drawOrganicBandBase(
                band2Brush,
                lerp(fromVariation.band2, toVariation.band2, mix),
                size.width,
                size.height
            )
        }
    }
}

private fun bandBrush(color: Color, peakAlpha: Float): Brush = Brush.horizontalGradient(
    colorStops = arrayOf(
        0f to Color.Transparent,
        0.20f to color.copy(alpha = peakAlpha * 0.35f),
        0.40f to color.copy(alpha = peakAlpha * 0.80f),
        0.50f to color.copy(alpha = peakAlpha),
        0.60f to color.copy(alpha = peakAlpha * 0.80f),
        0.80f to color.copy(alpha = peakAlpha * 0.35f),
        1f to Color.Transparent
    )
)

/**
 * Draws a band at its base position (no drift). Drift is applied externally as a GPU layer
 * translation, so this is only re-recorded when the variation specification changes.
 */
private fun DrawScope.drawOrganicBandBase(
    brush: Brush,
    spec: BandSpec,
    width: Float,
    height: Float
) {
    val center = Offset(width * spec.posX, height * spec.posY)
    val bandHeight = (width + height) * spec.heightFactor

    rotate(degrees = spec.angleDeg, pivot = center) {
        val length = hypot(width, height) * 1.6f
        drawRect(
            brush = brush,
            topLeft = Offset(center.x - length / 2f, center.y - bandHeight / 2f),
            size = Size(length, bandHeight)
        )
    }
}

/**
 * Accumulates active (foreground) ticking time so pausing/resuming never jumps the drift.
 * Ticks that are too far apart (the app was backgrounded in between) are discarded, so
 * the drift clock freezes while backgrounded and resumes seamlessly where it left off.
 */
private class ActiveTimeAccumulator {
    private var accumulatedMillis = 0L
    private var lastTick = 0L

    /** Anything above this gap means the app was backgrounded between two ticks. */
    private val maxTickGapMillis = 250L

    fun tick(): Float {
        val now = SystemClock.elapsedRealtime()
        if (lastTick != 0L) {
            val delta = now - lastTick
            if (delta < maxTickGapMillis) {
                accumulatedMillis += delta
            }
        }
        lastTick = now
        return accumulatedMillis / 1000f
    }
}

/** Reads the system "remove animations" (reduce motion) setting; 0 == animations off. */
private fun animatorScaleFactor(context: Context): Float = try {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
} catch (_: Exception) {
    1f
}