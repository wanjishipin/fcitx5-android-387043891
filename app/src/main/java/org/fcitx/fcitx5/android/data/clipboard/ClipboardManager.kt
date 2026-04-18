/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.clipboard

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.annotation.Keep
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardDao
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardDatabase
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.utils.WeakHashSet
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.clipboardManager
import timber.log.Timber

object ClipboardManager : ClipboardManager.OnPrimaryClipChangedListener,
    CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default) {
    private lateinit var clbDb: ClipboardDatabase
    private lateinit var clbDao: ClipboardDao
    private lateinit var clipboardTextFile: java.io.File

    fun interface OnClipboardUpdateListener {
        fun onUpdate(entry: ClipboardEntry)
    }

    private val clipboardManager = appContext.clipboardManager

    private val mutex = Mutex()

    var itemCount: Int = 0
        private set

    private suspend fun updateItemCount() {
        itemCount = clbDao.itemCount()
    }

    private val onUpdateListeners = WeakHashSet<OnClipboardUpdateListener>()

    var transformer: ((String) -> String)? = null

    fun addOnUpdateListener(listener: OnClipboardUpdateListener) {
        onUpdateListeners.add(listener)
    }

    fun removeOnUpdateListener(listener: OnClipboardUpdateListener) {
        onUpdateListeners.remove(listener)
    }

    private val enabledPref = AppPrefs.getInstance().clipboard.clipboardListening

    @Keep
    private val enabledListener = ManagedPreference.OnChangeListener<Boolean> { _, value ->
        if (value) {
            clipboardManager.addPrimaryClipChangedListener(this)
        } else {
            clipboardManager.removePrimaryClipChangedListener(this)
        }
    }

    private val limitPref = AppPrefs.getInstance().clipboard.clipboardHistoryLimit

    @Keep
    private val limitListener = ManagedPreference.OnChangeListener<Int> { _, _ ->
        launch { removeOutdated() }
    }

    var lastEntry: ClipboardEntry? = null

    private fun updateLastEntry(entry: ClipboardEntry) {
        lastEntry = entry
        onUpdateListeners.forEach { it.onUpdate(entry) }
    }

    fun init(context: Context) {
        // Store database and text file in external storage: /storage/emulated/0/Documents/FcitxClipboard/
        // This directory survives app uninstall and is accessible by shell
        // For Android 11+ (API 30+), MANAGE_EXTERNAL_STORAGE permission is needed
        var dbPath: String = "clbdb"
        try {
            val externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val clipboardDir = java.io.File(externalDir, "FcitxClipboard")
            Timber.d("Checking external clipboard dir: ${clipboardDir.absolutePath}")
            if (clipboardDir.exists() || clipboardDir.mkdirs()) {
                dbPath = java.io.File(clipboardDir, "clbdb").absolutePath
                clipboardTextFile = java.io.File(clipboardDir, "clipboard.txt")
                Timber.d("Using external storage: ${clipboardDir.absolutePath}")
            } else {
                Timber.w("Failed to create external clipboard directory, using internal storage")
                dbPath = "clbdb"
                clipboardTextFile = java.io.File(context.filesDir, "clipboard.txt")
            }
        } catch (e: Exception) {
            Timber.w(e, "External storage not available, using internal storage")
            dbPath = "clbdb"
            clipboardTextFile = java.io.File(context.filesDir, "clipboard.txt")
        }

        Timber.d("Clipboard database path: $dbPath")
        Timber.d("Clipboard text file: ${clipboardTextFile.absolutePath}")

        clbDb = Room
            .databaseBuilder(context, ClipboardDatabase::class.java, dbPath)
            // allow wipe the database instead of crashing when downgrade
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            // use TRUNCATE journal mode for reliable backup (single file instead of wal+shm)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
        clbDao = clbDb.clipboardDao()
        Timber.d("Clipboard database initialized, clip listening enabled: ${enabledPref.getValue()}")
        enabledListener.onChange(enabledPref.key, enabledPref.getValue())
        enabledPref.registerOnChangeListener(enabledListener)
        limitListener.onChange(limitPref.key, limitPref.getValue())
        limitPref.registerOnChangeListener(limitListener)
        launch {
            updateItemCount()
            // Try to restore from text file if database is empty
            restoreFromTextFileIfNeeded()
            syncToTextFile()
        }
    }

    /**
     * Restore clipboard entries from text file if database is empty but text file exists
     */
    private suspend fun restoreFromTextFileIfNeeded() {
        try {
            val currentCount = clbDao.itemCount()
            if (currentCount > 0) {
                Timber.d("Database has $currentCount entries, no need to restore")
                return
            }

            if (!clipboardTextFile.exists()) {
                Timber.d("No text file to restore from")
                return
            }

            val text = clipboardTextFile.readText()
            if (text.isBlank()) {
                Timber.d("Text file is empty")
                return
            }

            val lines = text.split("\n").filter { it.isNotBlank() }
            if (lines.isEmpty()) {
                Timber.d("No content in text file to restore")
                return
            }

            Timber.d("Restoring ${lines.size} entries from text file")
            clbDb.withTransaction {
                for (line in lines) {
                    val entry = ClipboardEntry(
                        text = line,
                        timestamp = System.currentTimeMillis()
                    )
                    clbDao.insert(entry)
                }
            }
            updateItemCount()
            Timber.d("Successfully restored clipboard from text file")
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore from text file")
        }
    }

    /**
     * Sync all clipboard entries to a plain text file for shell access
     */
    private suspend fun syncToTextFile() {
        try {
            val entries = clbDao.getAllUnpinned()
            val text = entries.joinToString("\n") { it.text }
            clipboardTextFile.writeText(text)
            Timber.d("Synced ${entries.size} clipboard entries to ${clipboardTextFile.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync clipboard to text file")
        }
    }

    suspend fun get(id: Int) = clbDao.get(id)

    suspend fun haveUnpinned() = clbDao.haveUnpinned()

    fun allEntries() = clbDao.allEntries()

    fun searchEntries(query: String) = clbDao.searchEntries(query)

    suspend fun pin(id: Int) = clbDao.updatePinStatus(id, true)

    suspend fun unpin(id: Int) = clbDao.updatePinStatus(id, false)

    suspend fun updateText(id: Int, text: String) {
        lastEntry?.let {
            if (id == it.id) updateLastEntry(it.copy(text = text))
        }
        clbDao.updateText(id, text)
    }

    suspend fun delete(id: Int) {
        clbDao.markAsDeleted(id)
        updateItemCount()
    }

    suspend fun deleteAll(skipPinned: Boolean = true): IntArray {
        val ids = if (skipPinned) {
            clbDao.findUnpinnedIds()
        } else {
            clbDao.findAllIds()
        }
        clbDao.markAsDeleted(*ids)
        updateItemCount()
        return ids
    }

    suspend fun undoDelete(vararg ids: Int) {
        clbDao.undoDelete(*ids)
        updateItemCount()
    }

    suspend fun realDelete() {
        clbDao.realDelete()
    }

    suspend fun nukeTable() {
        withContext(coroutineContext) {
            clbDb.clearAllTables()
            updateItemCount()
        }
    }

    private var lastClipTimestamp = -1L
    private var lastClipHash = 0

    override fun onPrimaryClipChanged() {
        val clip = clipboardManager.primaryClip ?: return
        /**
         * skip duplicate ClipData
         * https://developer.android.com/reference/android/content/ClipboardManager.OnPrimaryClipChangedListener#onPrimaryClipChanged()
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timestamp = clip.description.timestamp
            if (timestamp == lastClipTimestamp) return
            lastClipTimestamp = timestamp
        } else {
            val timestamp = System.currentTimeMillis()
            val hash = clip.hashCode()
            if (timestamp - lastClipTimestamp < 100L && hash == lastClipHash) return
            lastClipTimestamp = timestamp
            lastClipHash = hash
        }
        launch {
            mutex.withLock {
                val entry = ClipboardEntry.fromClipData(clip, transformer) ?: return@withLock
                if (entry.text.isBlank()) return@withLock
                try {
                    clbDao.find(entry.text, entry.sensitive)?.let {
                        updateLastEntry(it.copy(timestamp = entry.timestamp))
                        clbDao.updateTime(it.id, entry.timestamp)
                        return@withLock
                    }
                    val insertedEntry = clbDb.withTransaction {
                        val rowId = clbDao.insert(entry)
                        removeOutdated()
                        // new entry can be deleted immediately if clipboard limit == 0
                        clbDao.get(rowId) ?: entry
                    }
                    updateLastEntry(insertedEntry)
                    updateItemCount()
                    syncToTextFile()
                } catch (exception: Exception) {
                    Timber.w("Failed to update clipboard database: $exception")
                    updateLastEntry(entry)
                }
            }
        }
    }

    private suspend fun removeOutdated() {
        val limit = limitPref.getValue()
        val unpinned = clbDao.getAllUnpinned()
        if (unpinned.size > limit) {
            // the last one we will keep
            val last = unpinned
                .sortedBy { it.id }
                .getOrNull(unpinned.size - limit)
            // delete all unpinned before that, or delete all when limit <= 0
            clbDao.markUnpinnedAsDeletedEarlierThan(last?.timestamp ?: System.currentTimeMillis())
        }
    }

}