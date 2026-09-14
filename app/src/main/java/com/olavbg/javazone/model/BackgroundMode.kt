package com.olavbg.javazone.model

/**
 * How the decorative diagonal background is rendered. Stored in settings and read by the
 * theme, so changing it takes effect immediately without restarting the app.
 */
enum class BackgroundMode {
    /** No diagonal bands at all; only the solid background color. */
    None,

    /** Diagonal bands rendered once, frozen in place with no motion. */
    Static,

    /** Bands drift, tilt and morph on screen changes (the default). */
    Animated
}