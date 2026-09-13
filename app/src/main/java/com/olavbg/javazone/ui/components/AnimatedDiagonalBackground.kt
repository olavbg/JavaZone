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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

/** Run the background ticker at about 30 fps instead of every frame. */
private const val TICK_RATE_MILLIS = 33L

/**
 * How long the band morph waits after navigation, so it starts once the ~700 ms scene
 * transition has already settled and is not competing with it for frame time.
 */
private const val MORPH_START_DELAY_MILLIS = 760L

/** How long the smooth band-to-band morph takes. */
private const val MORPH_DURATION_MILLIS = 1200L

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
 */
@Composable
fun AnimatedDiagonalBackground(
    baseColor: Color,
    tintPrimary: Color,
    tintSecondary: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animationsEnabled = remember { animatorScaleFactor(context) != 0f }

    // Per-composition band intensities (kept stable so the gradient brushes can be cached).
    val band1Alpha = remember { 0.38f + Random.Default.nextFloat() * 0.10f }
    val band2Alpha = remember { 0.24f + Random.Default.nextFloat() * 0.10f }

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

    // Morph bookkeeping, shared between the ticker loop and the navigation effect.
    var morphArmed by remember { mutableStateOf(false) }
    var morphStartElapsed by remember { mutableFloatStateOf(-1f) } // <0 == no morph scheduled

    // Screen size captured once the layers lay out; used to scale drift to pixels.
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Arm a new morph shortly after the navigation signal, so the bands start moving once
    // the screen transition has already peaked.
    LaunchedEffect(reanimateTick) {
        if (reanimateTick == 0L) return@LaunchedEffect
        fromVariation = toVariation
        toVariation = DiagonalVariation.random()
        mix = 0f
        morphArmed = true
    }

    if (!animationsEnabled) {
        // Respect the system "remove animations" (reduce motion) setting: single static frame.
        Canvas(modifier = modifier) {
            drawRect(color = baseColor)
            drawOrganicBandBase(band1Brush, toVariation.band1, size.width, size.height)
            drawOrganicBandBase(band2Brush, toVariation.band2, size.width, size.height)
        }
        return
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val activeTime = remember { ActiveTimeAccumulator() }

    // Single ticker that advances both the drift clock and (when armed) the morph progress.
    // It wakes only ~30 times per second, pauses when the app is backgrounded, and only
    // updates the GPU layer translations — the band layers never redraw.
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(TICK_RATE_MILLIS)

                val elapsed = activeTime.tick()

                if (morphArmed) {
                    morphArmed = false
                    morphStartElapsed = elapsed + MORPH_START_DELAY_MILLIS / 1000f
                }
                if (morphStartElapsed >= 0f) {
                    val t = ((elapsed - morphStartElapsed) * 1000f / MORPH_DURATION_MILLIS)
                        .coerceIn(0f, 1f)
                    mix = FastOutSlowInEasing.transform(t)
                    if (t >= 1f) morphStartElapsed = -1f // settle on `toVariation`, keep mix = 1
                }

                val w = canvasSize.width.toFloat()
                val h = canvasSize.height.toFloat()
                if (w <= 0f || h <= 0f) continue

                val spec1 = lerp(fromVariation.band1, toVariation.band1, mix)
                val motion1 = driftMotion(spec1, elapsed)
                band1OffsetX = w * viewportDrift(spec1.posX, spec1.driftX, motion1)
                band1OffsetY = h * viewportDrift(spec1.posY, spec1.driftY, motion1)

                val spec2 = lerp(fromVariation.band2, toVariation.band2, mix)
                val motion2 = driftMotion(spec2, elapsed)
                band2OffsetX = w * viewportDrift(spec2.posX, spec2.driftX, motion2)
                band2OffsetY = h * viewportDrift(spec2.posY, spec2.driftY, motion2)
            }
        }
    }

    Box(modifier = modifier.onSizeChanged { canvasSize = it }) {
        // Opaque backdrop, drawn once.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = baseColor)
        }

        // Each band layer re-records only when `mix` changes (during a morph).
        // The slow drift is applied externally as a GPU layer translation.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = band1OffsetX
                    translationY = band1OffsetY
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

private class BandSpec(
    val angleDeg: Float,
    val posX: Float,
    val posY: Float,
    val heightFactor: Float,
    val driftX: Float,
    val driftY: Float,
    val omega: Float,
    val harmonicRatio: Float,
    val phaseOffset: Float
)

private class DiagonalVariation(val band1: BandSpec, val band2: BandSpec) {
    companion object {
        fun random(): DiagonalVariation {
            val r = Random.Default
            return DiagonalVariation(
                band1 = BandSpec(
                    angleDeg = -40f + r.nextFloat() * 80f,
                    posX = 0.05f + r.nextFloat() * 0.90f,
                    posY = 0.05f + r.nextFloat() * 0.90f,
                    heightFactor = 0.10f + r.nextFloat() * 0.08f,
                    driftX = 0.15f + r.nextFloat() * 0.18f,
                    driftY = 0.08f + r.nextFloat() * 0.14f,
                    omega = 0.06f + r.nextFloat() * 0.05f,
                    harmonicRatio = 2.23f + r.nextFloat() * 0.40f,
                    phaseOffset = r.nextFloat() * (2f * PI).toFloat()
                ),
                band2 = BandSpec(
                    angleDeg = -40f + r.nextFloat() * 80f,
                    posX = 0.05f + r.nextFloat() * 0.90f,
                    posY = 0.05f + r.nextFloat() * 0.90f,
                    heightFactor = 0.10f + r.nextFloat() * 0.08f,
                    driftX = 0.15f + r.nextFloat() * 0.18f,
                    driftY = 0.10f + r.nextFloat() * 0.12f,
                    omega = 0.05f + r.nextFloat() * 0.04f,
                    harmonicRatio = 1.73f + r.nextFloat() * 0.30f,
                    phaseOffset = r.nextFloat() * (2f * PI).toFloat()
                )
            )
        }
    }
}

private fun lerp(start: BandSpec, stop: BandSpec, t: Float): BandSpec = BandSpec(
    angleDeg = lerp(start.angleDeg, stop.angleDeg, t),
    posX = lerp(start.posX, stop.posX, t),
    posY = lerp(start.posY, stop.posY, t),
    heightFactor = lerp(start.heightFactor, stop.heightFactor, t),
    driftX = lerp(start.driftX, stop.driftX, t),
    driftY = lerp(start.driftY, stop.driftY, t),
    omega = lerp(start.omega, stop.omega, t),
    harmonicRatio = lerp(start.harmonicRatio, stop.harmonicRatio, t),
    phaseOffset = lerp(start.phaseOffset, stop.phaseOffset, t)
)

/**
 * Two sines with an irrational frequency ratio never repeat and never jump, regardless of
 * how long the composition lives.
 */
private fun driftMotion(spec: BandSpec, elapsed: Float): Float =
    sin(spec.omega * elapsed + spec.phaseOffset) * 0.70f +
        sin(spec.omega * spec.harmonicRatio * elapsed + spec.phaseOffset * 1.7f) * 0.30f

/**
 * Translates the drift into a layer offset that keeps the band's center inside the viewport
 * (0..1 as a fraction of the screen). Without this, a deep drift sweep can push a band most
 * of the way off-screen so only a sliver remains visible.
 */
private fun viewportDrift(pos: Float, drift: Float, motion: Float): Float =
    (pos + drift * motion).coerceIn(0f, 1f) - pos

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

/** Accumulates active (foreground) ticking time so pausing/resuming never jumps the drift. */
private class ActiveTimeAccumulator {
    private var accumulatedMillis = 0L
    private var lastTick = 0L

    fun tick(): Float {
        val now = SystemClock.elapsedRealtime()
        if (lastTick != 0L) {
            accumulatedMillis += (now - lastTick)
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