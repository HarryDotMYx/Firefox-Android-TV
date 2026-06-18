/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.components.locale

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.SystemClock
import android.util.Log
import androidx.preference.PreferenceManager
import io.sentry.Sentry
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.generated.LocaleList
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * This class manages persistence, application, and otherwise handling of
 * user-specified locales.
 *
 * Of note:
 *
 * * It's a singleton, because its scope extends to that of the application,
 *   and definitionally all changes to the locale of the app must go through
 *   this.
 * * It's lazy.
 * * It relies on using the SharedPreferences file owned by the app for performance.
 */
class LocaleManager {

    // These are volatile because we don't impose restrictions
    // over which thread calls our methods.
    @Volatile
    private var currentLocale: Locale? = null

    @Volatile
    private var systemLocale: Locale = Locale.getDefault()

    private val inited = AtomicBoolean(false)
    private var systemLocaleChanged = false
    private var receiver: BroadcastReceiver? = null

    /**
     * Ensure that you call this early in your application startup,
     * and with a context that's sufficiently long-lived (typically
     * the application context).
     *
     * Calling multiple times is harmless.
     */
    fun initialize(context: Context) {
        if (!inited.compareAndSet(false, true)) {
            return
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val current = systemLocale

                // We don't trust Locale.getDefault() here, because we make a
                // habit of mutating it! Use the one Android supplies, because
                // that gets regularly reset.
                // The default value of systemLocale is fine, because we haven't
                // yet swizzled Locale during static initialization.
                @Suppress("DEPRECATION")
                systemLocale = context.resources.configuration.locale
                systemLocaleChanged = true

                Log.d(LOG_TAG, "System locale changed from $current to $systemLocale")
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_LOCALE_CHANGED))
    }

    fun systemLocaleDidChange(): Boolean {
        return systemLocaleChanged
    }

    /**
     * Every time the system gives us a new configuration, it
     * carries the external locale. Fix it.
     */
    fun correctLocale(context: Context, res: Resources, config: Configuration) {
        val current = getCurrentLocale(context)

        // I know it's tempting to short-circuit here if the config seems to be
        // up-to-date, but the rest is necessary.

        @Suppress("DEPRECATION")
        config.locale = current

        // The following two lines are heavily commented in case someone
        // decides to chase down performance improvements and decides to
        // question what's going on here.
        // Both lines should be cheap, *but*...

        // This is unnecessary for basic string choice, but it almost
        // certainly comes into play when rendering numbers, deciding on RTL,
        // etc. Take it out if you can prove that's not the case.
        Locale.setDefault(current)

        // This seems to be a no-op, but every piece of documentation under the
        // sun suggests that it's necessary, and it certainly makes sense.
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, null)
    }

    /**
     * See the original Javadoc: detects and either applies or undoes system locale changes.
     *
     * If the current activity locale is correct, returns null.
     */
    fun onSystemConfigurationChanged(
        context: Context,
        resources: Resources,
        configuration: Configuration,
        currentActivityLocale: Locale?
    ): Locale? {
        if (!isMirroringSystemLocale(context)) {
            correctLocale(context, resources, configuration)
        }

        @Suppress("DEPRECATION")
        val changed = configuration.locale
        if (changed == currentActivityLocale) {
            return null
        }

        return changed
    }

    fun getAndApplyPersistedLocale(context: Context): String? {
        initialize(context)

        val t1 = SystemClock.uptimeMillis()
        val localeCode = getPersistedLocale(context) ?: return null

        // Note that we don't tell Gecko about this. We notify Gecko when the
        // locale is set, not when we update Java.
        val resultant = updateLocale(context, localeCode)

        if (resultant == null) {
            // Update the configuration anyway.
            updateConfiguration(context, currentLocale)
        }

        val t2 = SystemClock.uptimeMillis()
        Log.i(LOG_TAG, "Locale read and update took: ${t2 - t1}ms.")
        return resultant
    }

    /**
     * Returns the set locale if it changed.
     *
     * Always persists and notifies Gecko.
     */
    fun setSelectedLocale(context: Context, localeCode: String): String? {
        val resultant = updateLocale(context, localeCode)

        // We always persist and notify Gecko, even if nothing seemed to
        // change. This might happen if you're picking a locale that's the same
        // as the current OS locale. The OS locale might change next time we
        // launch, and we need the Gecko pref and persisted locale to have been
        // set by the time that happens.
        persistLocale(context, localeCode)

        return resultant
    }

    fun resetLocaleIfChanged(context: Context) {
        if (currentLocale !== systemLocale) {
            resetToSystemLocale(context)
        }
    }

    fun resetToSystemLocale(context: Context) {
        // Wipe the pref.
        val settings = getSharedPreferences(context)
        settings.edit().remove(PREF_LOCALE).apply()

        // Apply the system locale.
        updateLocale(context, systemLocale)
    }

    /**
     * This is public to allow for an activity to force the
     * current locale to be applied if necessary (e.g., when
     * a new activity launches).
     */
    fun updateConfiguration(context: Context, locale: Locale?) {
        val res = context.resources
        val config = res.configuration

        // We should use setLocale, but it's unexpectedly missing
        // on real devices.
        @Suppress("DEPRECATION")
        config.locale = locale

        config.setLayoutDirection(locale)

        @Suppress("DEPRECATION")
        res.updateConfiguration(config, null)
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        if (PREF_LOCALE == null) {
            PREF_LOCALE = context.resources.getString(R.string.pref_key_locale)
        }

        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * @return the persisted locale in Java format: "en_US".
     */
    private fun getPersistedLocale(context: Context): String? {
        val settings = getSharedPreferences(context)
        val locale = settings.getString(PREF_LOCALE, "")

        return if (locale == "") null else locale
    }

    private fun persistLocale(context: Context, localeCode: String) {
        val settings = getSharedPreferences(context)
        settings.edit().putString(PREF_LOCALE, localeCode).apply()
    }

    /**
     *  Note: If getCurrentLocale is used prior to a locale selection
     *  through an Intent (ie. as a commandline argument),
     *  there could be odd behaviour with differing
     *  locale information.
     */
    fun getCurrentLocale(context: Context): Locale {
        currentLocale?.let { return it }

        val current = getPersistedLocale(context)
        if (current != null) {
            currentLocale = Locales.parseLocaleCode(current)
        }

        if (currentLocale == null) {
            currentLocale = context.resources.configuration.locales.get(0)
        }

        // In a very small number of cases, this locale will still be null. Most of our
        // userbase uses English as a primary language, so we default to that as a fallback
        if (currentLocale == null) {
            Sentry.captureException(AssertionError("Selected locale not available. Falling back to EN"))
            currentLocale = Locale.US
        }

        return currentLocale!!
    }

    fun currentLanguageIsEnglish(context: Context): Boolean {
        return getCurrentLocale(context).language == "en"
    }

    /**
     * Updates the Java locale and the Android configuration.
     *
     * Returns the persisted locale if it differed.
     *
     * Does not notify Gecko.
     *
     * @param localeCode a locale string in Java format: "en_US".
     * @return if it differed, a locale string in Java format: "en_US".
     */
    private fun updateLocale(context: Context, localeCode: String): String? {
        // Fast path.
        val defaultLocale = Locale.getDefault()
        Log.d("LOCALE", "Trying to check locale")
        if (defaultLocale.toString() == localeCode) {
            Log.d("LOCALE", "Early return")
            return null
        }

        val locale = Locales.parseLocaleCode(localeCode)

        return updateLocale(context, locale)
    }

    /**
     * @return the Java locale string: e.g., "en_US".
     */
    private fun updateLocale(context: Context, locale: Locale): String? {
        // Fast path.
        if (Locale.getDefault() == locale) {
            return null
        }

        Locale.setDefault(locale)
        currentLocale = locale

        // Update resources.
        updateConfiguration(context, locale)

        return locale.toString()
    }

    fun isMirroringSystemLocale(context: Context): Boolean {
        return getPersistedLocale(context) == null
    }

    companion object {
        private const val LOG_TAG = "GeckoLocales"

        private var PREF_LOCALE: String? = null

        private const val FALLBACK_LOCALE_TAG = "en-US"

        private val instance = AtomicReference<LocaleManager>()

        @JvmStatic
        fun getInstance(): LocaleManager {
            instance.get()?.let { return it }

            val localeManager = LocaleManager()
            return if (instance.compareAndSet(null, localeManager)) {
                localeManager
            } else {
                instance.get()!!
            }
        }

        /**
         * Returns a list of supported locale codes
         */
        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun getPackagedLocaleTags(context: Context): Collection<String> {
            return LocaleList.BUNDLED_LOCALES
        }

        @JvmStatic
        fun getFallbackLocaleTag(): String {
            return FALLBACK_LOCALE_TAG
        }
    }
}
