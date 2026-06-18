/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.components.locale

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import java.util.Locale

abstract class LocaleAwareAppCompatActivity : AppCompatActivity() {

    @Volatile
    private var lastLocale: Locale? = null

    /**
     * Is called whenever the application locale has changed. Your Activity must either update
     * all localised Strings, or replace itself with an updated version.
     */
    abstract fun applyLocale()

    override fun onCreate(savedInstanceState: Bundle?) {
        Locales.initializeLocale(this)

        lastLocale = LocaleManager.getInstance().getCurrentLocale(applicationContext)

        LocaleManager.getInstance().updateConfiguration(this, lastLocale)

        super.onCreate(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val localeManager = LocaleManager.getInstance()

        localeManager.correctLocale(this, resources, resources.configuration)

        val changed = localeManager.onSystemConfigurationChanged(this, resources, newConfig, lastLocale)

        if (changed != null) {
            LocaleManager.getInstance().updateConfiguration(this, changed)
            applyLocale()
            setLayoutDirection(window.decorView, changed)
        }

        super.onConfigurationChanged(newConfig)
    }

    /**
     * Force set layout direction to RTL or LTR by Locale.
     */
    private fun setLayoutDirection(view: View, locale: Locale) {
        when (TextUtilsCompat.getLayoutDirectionFromLocale(locale)) {
            ViewCompat.LAYOUT_DIRECTION_RTL ->
                ViewCompat.setLayoutDirection(view, ViewCompat.LAYOUT_DIRECTION_RTL)
            else ->
                ViewCompat.setLayoutDirection(view, ViewCompat.LAYOUT_DIRECTION_LTR)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        onConfigurationChanged(resources.configuration)
    }

    override fun onResume() {
        super.onResume()
        (applicationContext as LocaleAwareApplication).onActivityResume()
    }

    override fun onPause() {
        super.onPause()
        (applicationContext as LocaleAwareApplication).onActivityPause()
    }
}
