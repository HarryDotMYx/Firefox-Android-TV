/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.session

import io.mockk.mockk
import io.mockk.verify
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.session.SessionUseCases
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.helpers.FirefoxRobolectricTestRunner
import org.mozilla.tv.firefox.utils.TurboMode
import org.mozilla.tv.firefox.utils.URLs

@RunWith(FirefoxRobolectricTestRunner::class)
class SessionRepoTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var sessionUseCases: SessionUseCases
    private lateinit var turboMode: TurboMode
    private lateinit var sessionRepo: SessionRepo

    @Before
    fun setup() {
        sessionManager = mockk(relaxed = true)
        sessionUseCases = mockk(relaxed = true)
        turboMode = mockk(relaxed = true)
        sessionRepo = SessionRepo(sessionManager, sessionUseCases, turboMode)
    }

    @Test
    fun `WHEN addSession is called with a url THEN a matching selected session is added`() {
        val url = "https://example.com/"

        val session = sessionRepo.addSession(url)

        assertEquals(url, session.url)
        verify(exactly = 1) { sessionManager.add(session, selected = true) }
    }

    @Test
    fun `WHEN addSession is called with no url THEN the home url session is added and selected`() {
        val session = sessionRepo.addSession()

        assertEquals(URLs.APP_URL_HOME, session.url)
        verify(exactly = 1) { sessionManager.add(session, selected = true) }
    }

    @Test
    fun `WHEN removeSession is called THEN the session is removed from the manager`() {
        val session = Session(initialUrl = "https://example.com/")

        sessionRepo.removeSession(session)

        verify(exactly = 1) { sessionManager.remove(session) }
    }
}
