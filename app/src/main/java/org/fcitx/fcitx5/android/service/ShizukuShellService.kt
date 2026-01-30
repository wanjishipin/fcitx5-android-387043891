/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.service

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import rikka.shizuku.Shizuku
import timber.log.Timber

import android.util.Log

/**
 * Shell service implementation that runs with Shizuku privileges.
 * This allows sending key events to any application.
 * Note: This runs in Shizuku's process, so we use android.util.Log instead of Timber.
 */
class ShellService : IShellService.Stub() {
    
    companion object {
        private const val TAG = "ShellService"
    }
    
    override fun destroy() {
        Log.d(TAG, "ShellService destroyed")
    }
    
    override fun exec(command: String): Int {
        return try {
            Log.d(TAG, "Executing: $command")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val result = process.waitFor()
            Log.d(TAG, "Command result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command: $command", e)
            -1
        }
    }
    
    override fun sendKeyEvent(keyCode: Int): Int {
        Log.d(TAG, "sendKeyEvent: $keyCode")
        return exec("input keyevent $keyCode")
    }
    
    override fun sendText(text: String): Int {
        Log.d(TAG, "sendText: $text")
        val escapedText = text.replace("'", "'\\''")
        return exec("input text '$escapedText'")
    }
}

/**
 * Manager class for Shizuku shell service
 */
object ShizukuShellManager {
    private const val REQUEST_CODE_PERMISSION = 1001
    
    private var shellService: IShellService? = null
    private var isServiceBound = false
    
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Timber.d("Shizuku binder received")
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindService()
        }
    }
    
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Timber.d("Shizuku binder dead")
        shellService = null
        isServiceBound = false
    }
    
    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Timber.d("Shizuku permission granted")
                bindService()
            } else {
                Timber.w("Shizuku permission denied")
            }
        }
    }
    
    private val userServiceArgs = Shizuku.UserServiceArgs(
        ShellService::class.java.componentName
    )
        .daemon(false)
        .processNameSuffix("shell_service")
        .debuggable(true)
        .version(1)
    
    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Timber.d("ShellService connected")
            if (binder != null) {
                shellService = IShellService.Stub.asInterface(binder)
                isServiceBound = true
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.d("ShellService disconnected")
            shellService = null
            isServiceBound = false
        }
    }
    
    fun init() {
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        
        // Check if Shizuku is already running
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                bindService()
            } else {
                requestPermission()
            }
        }
    }
    
    fun destroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
        
        if (isServiceBound) {
            try {
                Shizuku.unbindUserService(userServiceArgs, userServiceConnection, false)
            } catch (e: Exception) {
                Timber.e(e, "Failed to unbind ShellService")
            }
            shellService = null
            isServiceBound = false
        }
    }
    
    fun isAvailable(): Boolean {
        val ping = Shizuku.pingBinder()
        val hasService = shellService != null
        Timber.w("ShizukuShellManager.isAvailable: pingBinder=$ping, shellService=$hasService")
        return ping && hasService
    }
    
    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    fun isShizukuRunning(): Boolean = Shizuku.pingBinder()
    
    fun hasPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }
    
    fun requestPermission() {
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
            }
        }
    }
    
    private fun bindService() {
        try {
            Timber.w("Binding Shizuku UserService with args: ${userServiceArgs}")
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
            Timber.w("bindUserService called successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to bind ShellService")
        }
    }
    
    // Executor for async operations
    private val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
    
    /**
     * Send a key event using Shizuku shell privileges (async, non-blocking)
     */
    fun sendKeyEvent(keyCode: Int): Boolean {
        val service = shellService ?: return false
        executor.execute {
            try {
                val result = service.sendKeyEvent(keyCode)
                Timber.d("Shizuku sendKeyEvent result: $result")
            } catch (e: RemoteException) {
                Timber.e(e, "Failed to send key event via Shizuku")
            }
        }
        return true // Return immediately, execution is async
    }
    
    /**
     * Send text input using Shizuku shell privileges (async, non-blocking)
     */
    fun sendText(text: String): Boolean {
        val service = shellService ?: return false
        executor.execute {
            try {
                val result = service.sendText(text)
                Timber.d("Shizuku sendText result: $result")
            } catch (e: RemoteException) {
                Timber.e(e, "Failed to send text via Shizuku")
            }
        }
        return true
    }
    
    /**
     * Execute a shell command using Shizuku (async, non-blocking)
     */
    fun exec(command: String): Int {
        val service = shellService ?: return -1
        executor.execute {
            try {
                val result = service.exec(command)
                Timber.d("Shizuku exec result: $result")
            } catch (e: RemoteException) {
                Timber.e(e, "Failed to execute command via Shizuku")
            }
        }
        return 0 // Return immediately
    }
}

private val Class<*>.componentName: android.content.ComponentName
    get() = android.content.ComponentName(
        org.fcitx.fcitx5.android.BuildConfig.APPLICATION_ID,
        this.name
    )
