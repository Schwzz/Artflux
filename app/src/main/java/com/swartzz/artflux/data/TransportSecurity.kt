package com.swartzz.artflux.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Transport-security policy shared by browsing requests and diagnostics.
 *
 * Credentials are always allowed on HTTPS. On plain-HTTP endpoints they are
 * allowed only for local/private network hosts (for example a self-hosted API
 * on a home LAN), where sending an API key in cleartext carries no public
 * exposure. On public cleartext HTTP hosts, credentials are withheld rather
 * than transmitted in the clear.
 */
object TransportSecurity {

    const val SECURITY_WARNING =
        "Credentials are not sent over unencrypted HTTP connections on public hosts. " +
            "Use an HTTPS endpoint, or keep HTTP only for a local/private network source."

    /**
     * True when authentication values (query parameters or headers) may be attached
     * to requests made against [apiUrl].
     */
    fun allowsCredentials(apiUrl: String): Boolean {
        val url = apiUrl.toHttpUrlOrNull() ?: return false
        if (url.scheme.equals("https", ignoreCase = true)) return true
        return isLocalOrPrivateHost(url.host)
    }

    /**
     * Returns [apiUrl] when credentials may be attached, otherwise a copy with the
     * sensitive configuration stripped from the request configuration.
     */
    fun <T> guard(apiUrl: String, isPresent: Boolean, applyCredentials: () -> T, applyWithoutCredentials: () -> T): T {
        return if (!isPresent || allowsCredentials(apiUrl)) applyCredentials() else applyWithoutCredentials()
    }

    /** Hosts on a local or private network, where cleartext is an accepted tradeoff. */
    fun isLocalOrPrivateHost(host: String): Boolean {
        val name = host.trim().lowercase()
        if (name.isEmpty()) return false
        val labels = name.split(".")
        return when {
            name == "localhost" -> true
            name.endsWith(".localhost") -> true
            name.endsWith(".local") -> true
            labels.size == 4 && labels.all { it.toIntOrNull() in 0..255 } -> isPrivateIpv4(labels.map { it.toInt() })
            else -> false
        }
    }

    private fun isPrivateIpv4(octets: List<Int>): Boolean {
        val (a, b, _, _) = octets
        return a == 10 || (a == 192 && b == 168) || (a == 172 && b in 16..31) || (a == 127) || (a == 169 && b == 254)
    }
}
