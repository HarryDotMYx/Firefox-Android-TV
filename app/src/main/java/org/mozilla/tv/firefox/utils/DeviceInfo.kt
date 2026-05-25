/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.os.Build

/**
 * Contains information about the device the app is running on.
 */
class DeviceInfo {

    /**
     * Translate Android TV model codes into device names
     */
    fun getDeviceModel(): String {
        // Amazon does not localize their device names, so we do not need to
        val deviceCodeMap = mapOf(
            "AFTA" to "Android TV Cube",
            "AFTN" to "Android TV 4K",
            "AFTS" to "Android TV",
            "AFTB" to "Android TV",
            "AFTMM" to "Android TV Stick 4K",
            "AFTT" to "Android TV Stick",
            "AFTM" to "Android TV Stick",
            "AFTRS" to "Android TV Edition - Element 4K",
            "AFTKMST12" to "Android TV Edition - Toshiba 4K",
            "AFTBAMR311" to "Android TV Edition - Toshiba HD",
            "AFTJMST12" to "Android TV Edition - Insignia 4K",
            "AFTEAMR311" to "Android TV Edition - Insignia HD"
        )

        return deviceCodeMap.getOrElse(Build.MODEL) { "Android TV" }
    }
}
