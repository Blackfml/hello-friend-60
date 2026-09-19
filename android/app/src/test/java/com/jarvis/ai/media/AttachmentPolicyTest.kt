package com.jarvis.ai.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentPolicyTest {
    @Test
    fun supportsCommonGeminiMedia() {
        assertTrue(AttachmentPolicy.isSupported("image/jpeg"))
        assertTrue(AttachmentPolicy.isSupported("audio/mpeg"))
        assertTrue(AttachmentPolicy.isSupported("video/mp4"))
        assertTrue(AttachmentPolicy.isSupported("application/pdf"))
    }

    @Test
    fun rejectsUnknownType() {
        assertFalse(AttachmentPolicy.isSupported("application/x-executable"))
    }
}
