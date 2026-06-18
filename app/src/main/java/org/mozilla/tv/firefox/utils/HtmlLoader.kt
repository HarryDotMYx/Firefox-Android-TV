/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.content.Context
import androidx.annotation.RawRes
import java.io.IOException
import java.nio.charset.StandardCharsets

object HtmlLoader {

    /**
     * Load a given (html or css) resource file into a String. The input can contain tokens that will
     * be replaced with localised strings.
     *
     * @param substitutionTable A table of substitions, e.g. %shortMessage% -> "Error loading page..."
     *                          Can be null, in which case no substitutions will be made.
     * @return The file content, with all substitutions having being made.
     */
    @JvmStatic
    fun loadResourceFile(
        context: Context,
        @RawRes resourceID: Int,
        substitutionTable: Map<String, String>?
    ): String {
        try {
            context.resources.openRawResource(resourceID)
                .bufferedReader(StandardCharsets.UTF_8)
                .use { fileReader ->
                    val outputBuffer = StringBuilder()
                    fileReader.forEachLine { rawLine ->
                        var line = rawLine
                        if (substitutionTable != null) {
                            for ((key, value) in substitutionTable) {
                                line = line.replace(key, value)
                            }
                        }
                        outputBuffer.append(line)
                    }
                    return outputBuffer.toString()
                }
        } catch (e: IOException) {
            throw IllegalStateException("Unable to load error page data", e)
        }
    }
}
