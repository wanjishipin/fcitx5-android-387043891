/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.behavior

import android.os.Environment
import androidx.lifecycle.lifecycleScope
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceFragment
import org.fcitx.fcitx5.android.utils.addPreference
import kotlinx.coroutines.launch
import java.io.File

class ClipboardSettingsFragment : ManagedPreferenceFragment(AppPrefs.getInstance().clipboard) {

    override fun onPreferenceUiCreated(screen: androidx.preference.PreferenceScreen) {
        // Add load from SD card button
        screen.addPreference("Load from SD card") {
            val externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val clipboardDir = File(externalDir, "FcitxClipboard")
            val dbFile = File(clipboardDir, "clbdb")
            if (dbFile.exists()) {
                lifecycleScope.launch {
                    ClipboardManager.restoreFromExternalStorage()
                }
                android.widget.Toast.makeText(
                    requireContext(),
                    "Loading clipboard from SD card...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "No clbdb found in ${dbFile.absolutePath}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
