/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.content.Context
import android.util.Patterns
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import org.mozilla.tv.firefox.utils.publicsuffix.PublicSuffix
import java.net.URI

/** Methods for formatting a domain URI. */
object FormattedDomain {

    /**
     * Returns the domain for the given URI, formatted by the other available parameters.
     *
     * A public suffix is a top-level domain. For the input, "https://github.com", you can specify
     * `shouldIncludePublicSuffix`:
     * - true: "github.com"
     * - false: "github"
     *
     * The subdomain count is the number of subdomains you want to include; the domain will always be included. For
     * the input, "https://m.blog.github.io/", excluding the public suffix and the subdomain count:
     * - 0: "github"
     * - 1: "blog.github.com"
     * - 2: "m.blog.github.com"
     *
     * ipv4 & ipv6 urls will return the address directly.
     *
     * This implementation is influenced by Firefox iOS and can be used in place of some URI formatting functions:
     * - hostSLD: exclude publicSuffix, 0 subdomains
     * - baseDomain: include publicSuffix, 0 subdomains
     *
     * @param context the Activity context.
     * @param uri the URI whose host we should format.
     * @param shouldIncludePublicSuffix true if the public suffix should be included, false otherwise.
     * @param subdomainCount The number of subdomains to include.
     *
     * @return the formatted domain, or the empty String if the host cannot be found.
     */
    @JvmStatic
    @WorkerThread // calls PublicSuffix methods.
    fun format(
        context: Context,
        uri: URI,
        shouldIncludePublicSuffix: Boolean,
        @IntRange(from = 0) subdomainCount: Int
    ): String {
        require(subdomainCount >= 0) { "Expected subdomainCount >= 0." }

        val host = uri.host
        if (host.isNullOrEmpty()) {
            return "" // There's no host so there's no domain to retrieve.
        }

        if (isIPv4(host) ||
            isIPv6(uri) ||
            !host.contains(".")
        ) { // If this is just a hostname and not a FQDN, use the entire hostname.
            return host
        }

        val domainStr = PublicSuffix.getPublicSuffix(context, host, subdomainCount + 1)
        if (domainStr.isEmpty()) {
            // There is no public suffix found so we assume the whole host is a domain.
            return stripSubdomains(host, subdomainCount)
        }

        if (!shouldIncludePublicSuffix) {
            // We could be slightly more efficient if we wrote a new algorithm rather than using PublicSuffix twice
            // but I don't think it's worth the time and it'd complicate the code with more independent branches.
            return PublicSuffix.stripPublicSuffix(context, domainStr)
        }
        return domainStr
    }

    @JvmStatic
    fun stripCommonPrefixes(host: String): String {
        // In contrast to desktop, we also strip mobile subdomains,
        // since its unlikely users are intentionally typing them
        val start = when {
            host.startsWith("www.") -> 4
            host.startsWith("mobile.") -> 7
            host.startsWith("m.") -> 2
            else -> 0
        }

        return host.substring(start)
    }

    /** Strips any subdomains from the host over the given limit. */
    private fun stripSubdomains(host: String, desiredSubdomainCount: Int): String {
        var includedSubdomainCount = 0
        for (i in host.length - 1 downTo 0) {
            if (host[i] == '.') {
                if (includedSubdomainCount >= desiredSubdomainCount) {
                    return host.substring(i + 1)
                }

                includedSubdomainCount += 1
            }
        }

        // There are fewer subdomains than the total we'll accept so return them all!
        return host
    }

    @JvmStatic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun isIPv4(host: String): Boolean {
        return Patterns.IP_ADDRESS.matcher(host).matches()
    }

    // impl via FFiOS: https://github.com/mozilla-mobile/firefox-ios/blob/deb9736c905cdf06822ecc4a20152df7b342925d/Shared/Extensions/NSURLExtensions.swift#L292
    private fun isIPv6(uri: URI): Boolean {
        val host = uri.host
        return !host.isNullOrEmpty() && host.contains(":")
    }
}
