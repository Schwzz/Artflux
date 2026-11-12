package com.swartzz.artflux

import androidx.media3.common.PlaybackException
import coil.network.HttpException
import com.swartzz.artflux.ui.components.MediaLoadMessage
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Media-loading error translation: real HTTP status codes must survive
 * (403/404/429/5xx), and DNS, timeout, and undecodable-video failures
 * must be distinguishable without leaking request details.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MediaLoadMessageTest {

    private fun httpException(code: Int): HttpException =
        HttpException(Response.Builder()
            .request(okhttp3.Request.Builder().url("https://example.org/media").build())
            .protocol(Protocol.HTTP_1_1).code(code).message("x").body(ByteArray(0).toResponseBody()).build())

    private fun messageOf(error: Throwable): String = MediaLoadMessage.forError(error, local = false)

    @Test fun forbiddenAndUnauthorizedAreAccessDenied() {
        assertTrue(messageOf(httpException(403)).contains("refused access"))
        assertTrue(messageOf(httpException(401)).contains("refused access"))
    }

    @Test fun missingFilesAreReportedAsGone() {
        assertTrue(messageOf(httpException(404)).contains("no longer available"))
        assertTrue(messageOf(httpException(410)).contains("no longer available"))
    }

    @Test fun rateLimitsAndServerErrorsAreDistinct() {
        assertTrue(messageOf(httpException(429)).contains("limiting requests"))
        assertTrue(messageOf(httpException(503)).contains("media server"))
    }

    @Test fun dnsFailuresAreDistinguishedFromTimeouts() {
        val dns = java.io.IOException(UnknownHostException("host.example"))
        val timeout = java.io.IOException(SocketTimeoutException("timed out"))
        assertTrue(messageOf(dns).contains("Couldn't find the media server"))
        assertTrue(messageOf(timeout).contains("took too long"))
    }

    @Test fun httpStatusWinsOverCauseExceptions() {
        val layered = java.io.IOException("load failed", httpException(404))
        assertTrue(messageOf(layered).contains("no longer available"))
    }

    @Test fun decoderFailuresAreNotPromisedAsRetryableNetworkIssues() {
        val error = PlaybackException("Decoder init failed", null,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED)
        assertTrue(messageOf(error).contains("can't decode"))
        val exceeds = PlaybackException("Too demanding", null,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES)
        assertTrue(messageOf(exceeds).contains("can't decode"))
    }

    @Test fun remoteHttpVideoErrorsStillUseStatusCode() {
        val error = PlaybackException("Bad status", httpException(403),
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        assertTrue(messageOf(error).contains("refused access"))
    }
}
