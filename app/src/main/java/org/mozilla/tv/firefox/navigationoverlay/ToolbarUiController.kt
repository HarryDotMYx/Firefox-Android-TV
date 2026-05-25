/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.view.forEach
import androidx.fragment.app.FragmentManager
import io.reactivex.disposables.Disposable
import org.mozilla.tv.firefox.databinding.FragmentNavigationOverlayOrigBinding
import org.mozilla.tv.firefox.databinding.FragmentNavigationOverlayTopNavBinding
import org.mozilla.tv.firefox.databinding.TooltipBinding
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.support.ktx.android.view.hideKeyboard
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.experiments.ExperimentsProvider
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.utils.URLs
import org.mozilla.tv.firefox.utils.ViewUtils
import org.mozilla.tv.firefox.widget.IgnoreFocusMovementMethod
import org.mozilla.tv.firefox.widget.InlineAutocompleteEditText

private const val NAVIGATION_BUTTON_ENABLED_ALPHA = 1.0f
private const val NAVIGATION_BUTTON_DISABLED_ALPHA = 0.3f
private const val WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT

/**
 * An encapsulation of the toolbar to set up and respond to UI operations.
 */
class ToolbarUiController(
    private val toolbarViewModel: ToolbarViewModel,
    private val exitFirefox: () -> Unit,
    private val onNavigationEvent: (NavigationEvent, String?, InlineAutocompleteEditText.AutocompleteResult?) -> Unit,
    private val experimentsProvider: ExperimentsProvider
) {

    private var hasUserChangedURLSinceEditTextFocused = false

    private lateinit var tooltip: PopupWindow
    private lateinit var tooltipBinding: TooltipBinding
    private lateinit var binding: FragmentNavigationOverlayOrigBinding
    private lateinit var topNavBinding: FragmentNavigationOverlayTopNavBinding

    fun onCreateView(layout: View) {
        binding = FragmentNavigationOverlayOrigBinding.bind(layout)
        topNavBinding = FragmentNavigationOverlayTopNavBinding.bind(binding.topNavContainer)
        val toolbarClickListener = ToolbarOnClickListener()
        binding.topNavContainer.forEach {
            it.nextFocusDownId = binding.navUrlInput.id
            if (it.isFocusable) it.setOnClickListener(toolbarClickListener)

            it.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) showTooltip(view)
                else if (::tooltip.isInitialized) tooltip.dismiss() // Hide the tooltip when the button is not focused
            }
        }

        val layoutInflater = LayoutInflater.from(layout.context)
        tooltipBinding = TooltipBinding.inflate(layoutInflater)
        tooltip = PopupWindow(tooltipBinding.root, WRAP_CONTENT, WRAP_CONTENT, false)

        setupUrlInput()
    }

    private fun showTooltip(navBarButton: View) {
        tooltipBinding.tooltip.text = navBarButton.contentDescription
        tooltip.isClippingEnabled = false

        // The measurement of the popup happens in onGlobalLayout. We need to update the position of the
        // popup after this measurement has happened. Beforehand, the measuredWidth will be 0.
        tooltipBinding.root.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    tooltip.update(
                        navBarButton,
                        0 - (tooltipBinding.root.measuredWidth - navBarButton.width) / 2,
                        10,
                        -1,
                        -1
                    )
                    tooltipBinding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )
        tooltip.showAsDropDown(navBarButton)
    }

    private fun setupUrlInput() = with(binding.navUrlInput) {
        setOnCommitListener {
            val userInput = text.toString()
            if (userInput == URLs.APP_URL_HOME) {
                // If the input points to home, we short circuit and hide the keyboard, returning
                // the user to the home screen
                this.hideKeyboard()
                return@setOnCommitListener
            }

            if (userInput.isNotEmpty()) {
                val cachedAutocompleteResult = lastAutocompleteResult // setText clears the reference so we cache it here.
                setText(cachedAutocompleteResult.text)
                onNavigationEvent.invoke(NavigationEvent.LOAD_URL, userInput, cachedAutocompleteResult)
            }
        }
        this.movementMethod = IgnoreFocusMovementMethod()
        val autocompleteProvider = ShippedDomainsProvider().apply {
            initialize(
                    context = context
            )
        }
        setOnFilterListener { searchText, view ->
            val result = autocompleteProvider.getAutocompleteSuggestion(searchText)
            if (result != null)
                view?.onAutocomplete(InlineAutocompleteEditText.AutocompleteResult(result.text, result.source, result.totalItems))
        }

        setOnUserInputListener { hasUserChangedURLSinceEditTextFocused = true }
        setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) hasUserChangedURLSinceEditTextFocused = false }
    }

    fun observeToolbarState(
        layout: View,
        fragmentManager: FragmentManager
    ): List<Disposable> {
        fun updateOverlayButtonState(isEnabled: Boolean, overlayButton: ImageButton) {
            overlayButton.isEnabled = isEnabled
            overlayButton.isFocusable = isEnabled
            overlayButton.alpha =
                    if (isEnabled) NAVIGATION_BUTTON_ENABLED_ALPHA else NAVIGATION_BUTTON_DISABLED_ALPHA
        }

        val context = layout.context
        val serviceLocator = context.serviceLocator
        val turboButtonContent = experimentsProvider.getTurboModeToolbar()

        topNavBinding.turboButton.setImageResource(turboButtonContent.imageId)

        val stateDisposable = toolbarViewModel.state.subscribe {
            if (it == null) return@subscribe
            updateOverlayButtonState(it.backEnabled, topNavBinding.navButtonBack)
            updateOverlayButtonState(it.forwardEnabled, topNavBinding.navButtonForward)
            updateOverlayButtonState(it.pinEnabled, topNavBinding.pinButton)
            updateOverlayButtonState(it.refreshEnabled, topNavBinding.navButtonReload)
            updateOverlayButtonState(it.desktopModeEnabled, topNavBinding.desktopModeButton)

            topNavBinding.pinButton.isChecked = it.pinChecked
            topNavBinding.pinButton.contentDescription =
                if (it.pinChecked)
                    context.resources.getString(R.string.unpin_label)
                else
                    context.resources.getString(R.string.pin_label)

            topNavBinding.desktopModeButton.isChecked = it.desktopModeChecked
            topNavBinding.turboButton.isChecked = it.turboChecked

            val resources = layout.context.resources
            val turboText = if (it.turboChecked) {
                resources.getString(turboButtonContent.enabledTextId)
            } else {
                resources.getString(turboButtonContent.disabledTextId)
            }

            topNavBinding.turboButton.contentDescription = turboText
            if (topNavBinding.turboButton.hasFocus()) {
                tooltipBinding.tooltip.text = turboText
            }

            if (!hasUserChangedURLSinceEditTextFocused) {
                // The url can get updated in the background, e.g. if a loading page is redirected. We
                // don't want a url update to interrupt the user typing so we don't update the url from
                // the background if the user has already updated the url themselves.
                //
                // We revert this state when the view is unfocused: it ensures the URL is usually accurate
                // (for security reasons) and it's simple compared to other options which keep more state.
                //
                // One problem this solution has is that if the URL is updated in the background rapidly,
                // sometimes key events will be dropped, but I don't think there's much we can do about this:
                // we can't determine if the keyboard is up or not and focus isn't a good indicator because
                // we can focus the EditText without opening the soft keyboard and the user won't even know
                // these are inaccurate!
                binding.navUrlInput.setText(it.urlBarText)
            }
        }

        val eventDisposable = toolbarViewModel.events.subscribe {
            it?.consume {
                when (it) {
                    is ToolbarViewModel.Action.ShowTopToast -> ViewUtils.showCenteredTopToast(context, it.textId)
                    is ToolbarViewModel.Action.ShowBottomToast -> ViewUtils.showCenteredBottomToast(context, it.textId)
                    is ToolbarViewModel.Action.SetOverlayVisible -> serviceLocator.screenController
                        .showNavigationOverlay(fragmentManager, it.visible)
                    ToolbarViewModel.Action.ExitFirefox -> exitFirefox()
                }
                true
            }
        }
        return listOf(stateDisposable, eventDisposable)
    }

    private inner class ToolbarOnClickListener : View.OnClickListener {
        override fun onClick(view: View?) {
            val event = NavigationEvent.fromViewClick(view?.id)
                    ?: return

            when (event) {
                NavigationEvent.BACK -> toolbarViewModel.backButtonClicked()
                NavigationEvent.FORWARD -> toolbarViewModel.forwardButtonClicked()
                NavigationEvent.RELOAD -> toolbarViewModel.reloadButtonClicked()
                NavigationEvent.PIN_ACTION -> toolbarViewModel.pinButtonClicked()
                NavigationEvent.TURBO -> toolbarViewModel.turboButtonClicked()
                NavigationEvent.DESKTOP_MODE -> toolbarViewModel.desktopModeButtonClicked()
                NavigationEvent.EXIT_FIREFOX -> toolbarViewModel.exitFirefoxButtonClicked()
                else -> Unit // Nothing to do.
            }
            onNavigationEvent.invoke(event, null, null)
        }
    }
}
