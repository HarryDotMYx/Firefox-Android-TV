/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gradle.ext

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project

fun Project.androidExtension(): BaseExtension {
    return extensions.getByType(BaseExtension::class.java)
}
