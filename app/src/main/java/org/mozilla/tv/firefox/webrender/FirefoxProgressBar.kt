/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import org.mozilla.tv.firefox.databinding.FirefoxProgressBarBinding
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import mozilla.components.browser.session.Session
import org.mozilla.tv.firefox.R

private const val HIDE_ANIMATION_DURATION_MILLIS = 250L

class FirefoxProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle), Session.Observer {

    private val binding: FirefoxProgressBarBinding = FirefoxProgressBarBinding.inflate(LayoutInflater.from(context), this, true)

    fun initialize(webRenderFrag: WebRenderFragment) {
        webRenderFrag.session.register(this, webRenderFrag)
    }

    override fun onLoadingStateChanged(session: Session, loading: Boolean) {
        if (loading) {
            showBar()
        } else {
            hideBar()
        }
    }

    override fun onUrlChanged(session: Session, url: String) {
        binding.url.text = url
    }

    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    private fun showBar() {
        visibility = View.VISIBLE
        (binding.progressAnimation.background as AnimationDrawable).start()
        animate().cancel()
        alpha = 1f
    }

    private fun hideBar() {
        this.animate()
                .withEndAction {
                    this.visibility = View.GONE
                    (this.binding.progressAnimation.background as AnimationDrawable).stop()
                }
                .setDuration(HIDE_ANIMATION_DURATION_MILLIS)
                .alpha(0f)
                .start()
    }
}
