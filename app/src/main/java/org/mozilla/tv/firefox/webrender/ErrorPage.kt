/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.content.Context
import androidx.collection.ArrayMap
import mozilla.components.browser.errorpages.ErrorType
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.utils.HtmlLoader

object ErrorPage {

    @JvmStatic
    fun loadErrorPage(context: Context, desiredURL: String, errorType: ErrorType): String {
        // This is quite hacky: ideally we'd just load the css file directly using a '<link rel="stylesheet"'.
        // However WebView thinks it's still loading the original page, which can be an https:// page.
        // If mixed content blocking is enabled (which is probably what we want in Focus), then webkit
        // will block file:///android_res/ links from being loaded - which blocks our css from being loaded.
        // We could hack around that by enabling mixed content when loading an error page (and reenabling it
        // once that's loaded), but doing that correctly and reliably isn't particularly simple. Loading
        // the css data and stuffing it into our html is much simpler, especially since we're already doing
        // string substitutions.
        // As an added bonus: file:/// URIs are broken if the app-ID != app package, see:
        // https://code.google.com/p/android/issues/detail?id=211768 (this breaks loading css via file:///
        // references when running debug builds, and probably klar too) - which means this wouldn't
        // be possible even if we hacked around the mixed content issues.
        val cssString = HtmlLoader.loadResourceFile(context, R.raw.errorpage_style, null)

        val substitutionMap: MutableMap<String, String> = ArrayMap()
        val resources = context.resources

        substitutionMap["%page-title%"] = resources.getString(R.string.errorpage_title)
        substitutionMap["%button%"] = resources.getString(R.string.errorpage_refresh)
        substitutionMap["%messageShort%"] = resources.getString(errorType.titleRes)
        substitutionMap["%messageLong%"] = resources.getString(errorType.messageRes, desiredURL)
        substitutionMap["%css%"] = cssString

        return HtmlLoader.loadResourceFile(context, R.raw.errorpage, substitutionMap)
    }
}
