/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.settings

import org.mozilla.tv.firefox.databinding.SettingsScreenButtonsBinding
import org.mozilla.tv.firefox.databinding.SettingsScreenFxaProfileBinding
import org.mozilla.tv.firefox.databinding.SettingsScreenSwitchBinding
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.sentry.Sentry
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.architecture.FirefoxViewModelProviders
import org.mozilla.tv.firefox.channels.SettingsScreen
import org.mozilla.tv.firefox.channels.SettingsTile
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.fxa.FxaRepo
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.utils.PicassoWrapper
import org.mozilla.tv.firefox.utils.RoundCornerTransformation
import org.mozilla.tv.firefox.utils.ServiceLocator

const val KEY_SETTINGS_TYPE = "KEY_SETTINGS_TYPE"

/** The settings for the app. */
class SettingsFragment : Fragment() {
    enum class Action {
        SESSION_CLEARED
    }

    private var screenBinding: Any? = null

    var compositeDisposable = CompositeDisposable()
    private lateinit var serviceLocator: ServiceLocator

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        serviceLocator = context!!.serviceLocator

        val settingsVM = FirefoxViewModelProviders.of(this@SettingsFragment).get(SettingsViewModel::class.java)
        val type: SettingsTile = SettingsScreen.valueOf(arguments!!.getString(KEY_SETTINGS_TYPE)!!)
        val view = when (type) {
            SettingsScreen.DATA_COLLECTION -> setupDataCollectionScreen(inflater, container, settingsVM)
            SettingsScreen.CLEAR_COOKIES -> setupClearCookiesScreen(inflater, container, settingsVM)
            SettingsScreen.FXA_PROFILE -> setupFxaProfileScreen(inflater, container)
            else -> {
                Sentry.captureException(IllegalStateException("Unexpected Settings type received: $type"))
                return container!!
            }
        }
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            serviceLocator.screenController.handleBack(parentFragmentManager)
        }

        return view
    }

    private fun setupDataCollectionScreen(
        inflater: LayoutInflater,
        parentView: ViewGroup?,
        settingsViewModel: SettingsViewModel
    ): View {
        val binding = SettingsScreenSwitchBinding.inflate(inflater, parentView, false)
        screenBinding = binding
        settingsViewModel.dataCollectionEnabled.observe(viewLifecycleOwner, Observer<Boolean> { state ->
            binding.toggle.isChecked = state ?: return@Observer
        })
        binding.toggle.setOnClickListener {
            settingsViewModel.setDataCollectionEnabled(binding.toggle.isChecked)
        }
        binding.description.text = resources.getString(R.string.settings_telemetry_description,
                resources.getString(R.string.firefox_tv_brand_name))
        return binding.root
    }

    private fun setupClearCookiesScreen(
        inflater: LayoutInflater,
        parentView: ViewGroup?,
        settingsViewModel: SettingsViewModel
    ): View {
        settingsViewModel.events.observe(viewLifecycleOwner, Observer {
            it?.consume { event ->
                when (event) {
                    Action.SESSION_CLEARED -> {
                        activity?.recreate()
                    }
                }
                true
            }
        })

        val binding = SettingsScreenButtonsBinding.inflate(inflater, parentView, false)
        screenBinding = binding
        binding.confirmAction.setOnClickListener {
            settingsViewModel.clearBrowsingData(serviceLocator.engineViewCache)
            serviceLocator.screenController.handleBack(parentFragmentManager)
        }
        binding.cancelAction.setOnClickListener {
            serviceLocator.screenController.handleBack(parentFragmentManager)
        }
        return binding.root
    }

    private fun setupFxaProfileScreen(
        inflater: LayoutInflater,
        parentView: ViewGroup?
    ): View {
        val binding = SettingsScreenFxaProfileBinding.inflate(inflater, parentView, false)
        screenBinding = binding

        setupFxaText(binding)
        setupFxaProfileClickListeners(binding)
        observeFxaProfile(binding)
            .forEach { compositeDisposable.add(it) }

        val fxaRepo = serviceLocator.fxaRepo
        binding.buttonFirefoxTabs.setOnClickListener {
            fxaRepo.showFxaOnboardingScreen(context!!)
        }

        return binding.root
    }

    private fun setupFxaText(binding: SettingsScreenFxaProfileBinding) {
        val appName = resources.getString(R.string.app_name)
        binding.buttonFirefoxTabs.text = resources.getString(R.string.fxa_settings_primary_button, appName)
        // Username is positioned and styled differently, so it is left blank here
        // and set on another TextView
        binding.signedInAs.text = resources.getString(R.string.fxa_settings_body, "")
    }

    private fun setupFxaProfileClickListeners(binding: SettingsScreenFxaProfileBinding) {
        val screenController = serviceLocator.screenController
        val fxaRepo = serviceLocator.fxaRepo
        val telemetryIntegration = TelemetryIntegration.INSTANCE

        binding.buttonFirefoxTabs.setOnClickListener {
            // TODO show send tab tutorial
            telemetryIntegration.fxaProfileShowOnboardingButtonClickEvent()
        }
        binding.buttonSignOut.setOnClickListener {
            fxaRepo.logout()
            screenController.handleBack(parentFragmentManager)
            telemetryIntegration.fxaProfileSignOutButtonClickEvent()
        }
        binding.backButton.setOnClickListener {
            screenController.handleBack(parentFragmentManager)
        }
    }

    private fun observeFxaProfile(binding: SettingsScreenFxaProfileBinding): List<Disposable> {
        val accountState = context!!.serviceLocator.fxaRepo.accountState

        return listOf(
            accountState
                .ofType(FxaRepo.AccountState.AuthenticatedWithProfile::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    binding.userDisplayName.text = it.profile.displayName
                    it.profile.avatarSetStrategy
                        .setTransformation(RoundCornerTransformation(binding.avatarImage.width.toFloat()))
                        .invoke(binding.avatarImage)
                },
            accountState
                .filter { it::class.java != FxaRepo.AccountState.AuthenticatedWithProfile::class.java }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    binding.userDisplayName.text = ""
                    binding.signedInAs.text = resources.getString(R.string.fxa_settings_body_no_display_name)
                    PicassoWrapper.client.load(R.drawable.ic_default_avatar).into(binding.avatarImage)
                }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        screenBinding = null
        compositeDisposable.clear()
    }

    companion object {
        const val FRAGMENT_TAG = "settings"

        fun newInstance(type: SettingsScreen): SettingsFragment {
            return SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_SETTINGS_TYPE, type.toString())
                }
            }
        }
    }
}
