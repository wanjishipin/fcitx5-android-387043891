/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.service;

interface IShellService {
    void destroy() = 16777114;
    
    /**
     * Execute a shell command and return the exit code
     */
    int exec(String command) = 1;
    
    /**
     * Send a key event to the system
     */
    int sendKeyEvent(int keyCode) = 2;
    
    /**
     * Send text input to the system
     */
    int sendText(String text) = 3;
}
