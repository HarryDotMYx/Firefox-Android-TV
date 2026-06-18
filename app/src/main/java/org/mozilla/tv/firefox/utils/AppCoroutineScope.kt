/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * An application-lifetime [CoroutineScope] for intentional fire-and-forget work that is **not**
 * tied to any UI lifecycle — e.g. warming up an on-disk cache, or persisting/removing a file in
 * the background.
 *
 * This is the recommended replacement for [kotlinx.coroutines.GlobalScope] (which requires opting
 * into the delicate coroutines API). A [SupervisorJob] is used so a failure in one task does not
 * cancel the others. Work defaults to [Dispatchers.Default]; pass an explicit dispatcher (e.g.
 * [Dispatchers.Main]) when launching if needed.
 */
object AppCoroutineScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default)
