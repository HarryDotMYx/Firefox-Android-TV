/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.onboarding

import org.mozilla.tv.firefox.databinding.ReceiveTabPreboardingBinding
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import org.mozilla.tv.firefox.FirefoxApplication
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration

/**
 * Manages an onboarding screen, which is shown once to users upon app start in order
 * to educate them about receive tab functionality.
 */
class ReceiveTabPreboardingActivity : AppCompatActivity() {

    private lateinit var binding: ReceiveTabPreboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ReceiveTabPreboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.descriptionText.text = resources.getString(
            R.string.fxa_preboarding_instruction1,
            resources.getString(R.string.firefox_tv_brand_name_short),
            resources.getString(R.string.firefox_tv_brand_name)
        )

        binding.buttonSignIn.setOnClickListener {
            TelemetryIntegration.INSTANCE.fxaPreboardingSignInButtonClickEvent()
            @Suppress("DEPRECATION") // Couldn't work out a better way to do this. If you
            // think of one, please replace this
            (application as FirefoxApplication).mainActivityCommandBus
                .onNext(MainActivity.Command.BEGIN_LOGIN)
            finish()
        }

        binding.buttonNotNow.setOnClickListener {
            finish()
            TelemetryIntegration.INSTANCE.fxaPreboardingDismissButtonClickEvent()
        }

        setOnboardReceiveTabsShown()
    }

    private fun setOnboardReceiveTabsShown() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(ONBOARD_RECEIVE_TABS_SHOWN_PREF, true)
                .apply()
    }

    companion object {
        const val ONBOARD_RECEIVE_TABS_SHOWN_PREF = "onboard_receive_tabs_shown"
    }
}
