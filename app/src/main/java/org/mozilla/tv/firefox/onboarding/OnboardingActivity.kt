/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.onboarding

import org.mozilla.tv.firefox.databinding.ActivityOnboardingBinding
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.serviceLocator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setContent()

        binding.contentOnboarding.enableTurboMode.setOnClickListener { _ ->
            setTurboMode(true)
            finish()
        }

        binding.contentOnboarding.disableTurboMode.setOnClickListener { _ ->
            setTurboMode(false)
            setResult(Activity.RESULT_OK, Intent())
            finish()
        }

        setOnboardShown()
    }

    private fun setContent() {
        val content = serviceLocator.experimentsProvider.getTurboModeOnboarding()

        binding.contentOnboarding.disableTurboMode.text = resources.getString(content.disableButtonTextId)
        binding.contentOnboarding.enableTurboMode.text = resources.getString(content.enableButtonTextId)
        binding.contentOnboarding.onboardingMainText.text = resources.getString(content.descriptionId)
        binding.contentOnboarding.turboModeTitle.text = resources.getString(content.titleId)
        binding.contentOnboarding.turboImageView.setImageResource(content.imageId)
        binding.contentOnboarding.turboImageView.contentDescription = resources.getString(content.imageContentDescriptionId)
    }

    private fun setTurboMode(turboModeEnabled: Boolean) {
        serviceLocator.turboMode.isEnabled = turboModeEnabled
    }

    private fun setOnboardShown() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(ONBOARD_SHOWN_PREF, true)
                .apply()
    }

    companion object {
        const val ONBOARD_SHOWN_PREF = "onboard_shown"
    }
}
