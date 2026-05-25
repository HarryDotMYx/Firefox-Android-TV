/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import org.mozilla.tv.firefox.databinding.SettingsTileBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.utils.URLs

class SettingsChannelAdapter(
    private val loadUrl: (String) -> Unit,
    private val showSettings: (SettingsScreen) -> Unit
) : RecyclerView.Adapter<SettingsTileHolder>() {
    private val settingsItems = arrayOf(
        SettingsItem(
            SettingsScreen.DATA_COLLECTION,
            R.drawable.ic_data_collection,
            R.string.preference_mozilla_telemetry2,
            R.id.settings_tile_telemetry),
        SettingsItem(
            SettingsScreen.CLEAR_COOKIES,
            R.drawable.mozac_ic_delete,
            R.string.settings_cookies_dialog_title,
            R.id.settings_tile_cleardata),
        SettingsItem(
            SettingsButton.ABOUT,
            R.drawable.mozac_ic_info,
            R.string.menu_about,
            R.id.settings_tile_about),
        SettingsItem(
            SettingsButton.PRIVACY_POLICY,
            R.drawable.mozac_ic_globe,
            R.string.preference_privacy_notice,
            R.id.settings_tile_privacypolicy)
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsTileHolder {
        val binding = SettingsTileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SettingsTileHolder(binding)
    }

    override fun getItemCount(): Int {
        return settingsItems.size
    }

    override fun onBindViewHolder(holder: SettingsTileHolder, position: Int) {
        val itemData = settingsItems[position]
        holder.iconView.setImageResource(itemData.imgRes)
        holder.titleView.setText(itemData.titleRes)
        holder.binding.settingsCardview.setOnClickListener {
            when (val type = itemData.type) {
                SettingsScreen.DATA_COLLECTION -> showSettings(type as SettingsScreen)
                SettingsScreen.CLEAR_COOKIES -> showSettings(type as SettingsScreen)
                SettingsButton.ABOUT -> loadUrl(URLs.URL_ABOUT)
                SettingsButton.PRIVACY_POLICY -> loadUrl(URLs.PRIVACY_NOTICE_URL)
            }
            TelemetryIntegration.INSTANCE.settingsTileClickEvent(itemData.type)
        }
        holder.itemView.contentDescription = holder.itemView.context.getString(itemData.titleRes)
        holder.itemView.id = itemData.viewId // Add ids for testing
    }
}

class SettingsTileHolder(val binding: SettingsTileBinding) : RecyclerView.ViewHolder(binding.root) {
    val iconView: ImageView = binding.settingsIcon
    val titleView: TextView = binding.settingsTitle
}

// We differentiate between Settings tiles that lead to other Settings screens, or are just buttons
interface SettingsTile
enum class SettingsScreen : SettingsTile {
    DATA_COLLECTION, CLEAR_COOKIES, FXA_PROFILE
}
enum class SettingsButton : SettingsTile {
        ABOUT, PRIVACY_POLICY
}

private data class SettingsItem(val type: SettingsTile, val imgRes: Int, val titleRes: Int, val viewId: Int)
