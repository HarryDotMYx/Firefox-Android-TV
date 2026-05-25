/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import org.mozilla.tv.firefox.databinding.ChannelOnboardingBinding
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import org.mozilla.tv.firefox.R

class ChannelOnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ChannelOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ChannelOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvOnboardingButton.setOnClickListener { _ ->
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    .edit()
                    .putBoolean(TV_ONBOARDING_SHOWN_PREF, true)
                    .apply()
            finish()
        }
    }

    companion object {
        const val TV_ONBOARDING_SHOWN_PREF = "tv_onboarding_shown"
    }
}
