package com.swartzz.artflux

import com.swartzz.artflux.data.MediaRequestBuilder
import com.swartzz.artflux.data.TransportSecurity
import com.swartzz.artflux.model.AuthParam
import com.swartzz.artflux.model.AuthType
import com.swartzz.artflux.model.MediaSourceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that authentication values (API keys, Bearer tokens, custom auth headers,
 * and auth query parameters) are withheld from requests to public cleartext HTTP
 * endpoints, while HTTPS and local/private-network HTTP endpoints keep working.
 */
class TransportSecurityTest {

  private fun config(apiUrl: String): MediaSourceConfig = MediaSourceConfig(
    id = "sec-test",
    name = "Security Test Source",
    apiUrl = apiUrl,
    searchParam = "tags",
    authType = AuthType.BEARER_TOKEN,
    authQueryParams = listOf(AuthParam("api_key", "SECRET_KEY_123"), AuthParam("user_id", "42")),
    authHeaderValue = "SECRET_TOKEN_456"
  )

  // --- Policy decisions ---

  @Test
  fun `credentials allowed on https public endpoint`() {
    assertTrue(TransportSecurity.allowsCredentials("https://api.example.com/posts.json"))
  }

  @Test
  fun `credentials withheld on public cleartext http endpoint`() {
    assertFalse(TransportSecurity.allowsCredentials("http://api.example.com/posts.json"))
    assertFalse(TransportSecurity.allowsCredentials("http://insecure-booru.org/index.php"))
  }

  @Test
  fun `credentials allowed on local http endpoints`() {
    assertTrue(TransportSecurity.allowsCredentials("http://localhost:8080/api"))
    assertTrue(TransportSecurity.allowsCredentials("http://127.0.0.1:9000/index.php"))
    assertTrue(TransportSecurity.allowsCredentials("http://192.168.1.24:8080/api"))
    assertTrue(TransportSecurity.allowsCredentials("http://10.0.0.5/api"))
    assertTrue(TransportSecurity.allowsCredentials("http://172.16.4.9/api"))
  }

  @Test
  fun `credentials withheld for invalid api url`() {
    assertFalse(TransportSecurity.allowsCredentials("not a url"))
    assertFalse(TransportSecurity.allowsCredentials(""))
  }

  @Test
  fun `private ipv4 boundary ranges respected`() {
    assertFalse(TransportSecurity.isLocalOrPrivateHost("172.15.0.1"))
    assertFalse(TransportSecurity.isLocalOrPrivateHost("172.32.0.1"))
    assertFalse(TransportSecurity.isLocalOrPrivateHost("11.0.0.1"))
    assertFalse(TransportSecurity.isLocalOrPrivateHost("192.167.1.1"))
    assertFalse(TransportSecurity.isLocalOrPrivateHost("192.169.1.1"))
    assertTrue(TransportSecurity.isLocalOrPrivateHost("172.31.255.255"))
    assertTrue(TransportSecurity.isLocalOrPrivateHost("10.255.255.255"))
    assertTrue(TransportSecurity.isLocalOrPrivateHost("mynas.local"))
    assertTrue(TransportSecurity.isLocalOrPrivateHost("localhost"))
  }

  // --- Request building ---

  @Test
  fun `auth query params and headers are sent on https`() {
    val request = MediaRequestBuilder.buildFetchRequest(config("https://api.example.com/posts.json"))
    assertTrue(request.url.queryParameter("api_key") == "SECRET_KEY_123")
    assertEquals("Bearer SECRET_TOKEN_456", request.header("Authorization"))
  }

  @Test
  fun `auth query params and headers are withheld on public http`() {
    val request = MediaRequestBuilder.buildFetchRequest(config("http://api.example.com/posts.json"))
    assertNull(request.url.queryParameter("api_key"))
    assertNull(request.url.queryParameter("user_id"))
    assertNull(request.header("Authorization"))
  }

  @Test
  fun `auth is preserved on local network http`() {
    val request = MediaRequestBuilder.buildFetchRequest(config("http://192.168.1.24:8080/posts.json"))
    assertEquals("SECRET_KEY_123", request.url.queryParameter("api_key"))
    assertEquals("Bearer SECRET_TOKEN_456", request.header("Authorization"))
  }

  @Test
  fun `request still carries standard headers without auth`() {
    val request = MediaRequestBuilder.buildFetchRequest(config("http://api.example.com/posts.json"), query = "scenery")
    assertEquals("ArtfluxApp/2.0 (Android; MediaBrowser)", request.header("User-Agent"))
    assertEquals("application/json", request.header("Accept"))
    assertTrue(request.url.queryParameterValues("tags").contains("scenery"))
  }
}
