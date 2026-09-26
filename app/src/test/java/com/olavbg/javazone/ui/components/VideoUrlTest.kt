package com.olavbg.javazone.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoUrlTest {

    @Test
    fun resolvesNumericVimeoIdToEmbedUrl() {
        val result = resolveVideoEmbedUrl("1008064975")
        assertEquals("https://player.vimeo.com/video/1008064975?autoplay=1", result)
    }

    @Test
    fun resolvesVimeoUrlToEmbedUrl() {
        val result = resolveVideoEmbedUrl("https://vimeo.com/1008064975")
        assertEquals("https://player.vimeo.com/video/1008064975?autoplay=1", result)
    }

    @Test
    fun resolvesVimeoUrlWithQueryParamsToEmbedUrl() {
        val result = resolveVideoEmbedUrl("https://vimeo.com/1008064975?share=copy")
        assertEquals("https://player.vimeo.com/video/1008064975?autoplay=1", result)
    }

    @Test
    fun resolvesVimeoPlayerUrlToEmbedUrl() {
        val result = resolveVideoEmbedUrl("https://player.vimeo.com/video/1008064975")
        assertEquals("https://player.vimeo.com/video/1008064975?autoplay=1", result)
    }

    @Test
    fun resolvesYouTubeUrlToEmbedUrl() {
        val result = resolveVideoEmbedUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertEquals("https://www.youtube.com/embed/dQw4w9WgXcQ?autoplay=1", result)
    }

    @Test
    fun resolvesShortYouTubeUrlToEmbedUrl() {
        val result = resolveVideoEmbedUrl("https://youtu.be/dQw4w9WgXcQ")
        assertEquals("https://www.youtube.com/embed/dQw4w9WgXcQ?autoplay=1", result)
    }

    @Test
    fun resolvesStandardVideoUrlFallback() {
        val result = resolveVideoUrl("1008064975")
        assertEquals("https://vimeo.com/1008064975", result)
    }
}
