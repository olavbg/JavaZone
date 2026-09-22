package com.olavbg.javazone.ui.components

import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Vertical drift window (fraction of screen height) the band centers are confined to.
 * Keeping them here holds the bands mostly toward the middle of the screen; with the
 * band's own thickness the edges may still dip slightly past the top/bottom, but never
 * slide far off.
 */
internal const val BAND_CENTER_Y_MIN = 0.15f
internal const val BAND_CENTER_Y_MAX = 0.85f

internal class BandSpec(
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

internal class DiagonalVariation(val band1: BandSpec, val band2: BandSpec) {
    companion object {
        fun random(): DiagonalVariation {
            val r = Random.Default
            return DiagonalVariation(
                band1 = BandSpec(
                    angleDeg = -40f + r.nextFloat() * 80f,
                    posX = 0.05f + r.nextFloat() * 0.90f,
                    posY = BAND_CENTER_Y_MIN + r.nextFloat() * (BAND_CENTER_Y_MAX - BAND_CENTER_Y_MIN),
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
                    posY = BAND_CENTER_Y_MIN + r.nextFloat() * (BAND_CENTER_Y_MAX - BAND_CENTER_Y_MIN),
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

internal fun lerp(start: BandSpec, stop: BandSpec, t: Float): BandSpec = BandSpec(
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
internal fun driftMotion(spec: BandSpec, elapsed: Float): Float =
    sin(spec.omega * elapsed + spec.phaseOffset) * 0.70f +
        sin(spec.omega * spec.harmonicRatio * elapsed + spec.phaseOffset * 1.7f) * 0.30f

/**
 * Slow rocking used for the band's angle. Deliberately desynchronized from
 * [driftMotion] (shifted phase and slower harmonics) so the tilt doesn't just
 * mirror the horizontal/vertical translation.
 */
internal fun rotationMotion(spec: BandSpec, elapsed: Float): Float =
    sin(spec.omega * 0.55f * elapsed + spec.phaseOffset + 1.3f) * 0.85f +
        sin(spec.omega * spec.harmonicRatio * 0.22f * elapsed + spec.phaseOffset) * 0.15f