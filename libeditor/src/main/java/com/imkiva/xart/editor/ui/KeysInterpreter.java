/*
 * Copyright (c) 2013 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.imkiva.xart.editor.ui;

import android.view.KeyEvent;

import com.imkiva.xart.language.ParserDefinition;


public class KeysInterpreter {


    public static char keyEventToPrintableChar(KeyEvent event) {
        char c = ParserDefinition.NULL_CHAR;

        if (isNewline(event)) {
            c = ParserDefinition.NEWLINE;
        } else if (isBackspace(event)) {
            c = ParserDefinition.BACKSPACE;
        } else if (isTab(event)) {
            c = ParserDefinition.TAB;
        } else if (isSpace(event)) {
            c = ' ';
        } else if (event.isPrintingKey()) {
            c = (char) event.getUnicodeChar(event.getMetaState());
        }

        return c;
    }

    private static boolean isTab(KeyEvent event) {
        return (event.isShiftPressed() &&
                (event.getKeyCode() == KeyEvent.KEYCODE_SPACE)) ||
                (event.getKeyCode() == KeyEvent.KEYCODE_TAB);
    }

    private static boolean isBackspace(KeyEvent event) {
        return (event.getKeyCode() == KeyEvent.KEYCODE_DEL);
    }

    private static boolean isNewline(KeyEvent event) {
        return (event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
    }

    private static boolean isSpace(KeyEvent event) {
        return (event.getKeyCode() == KeyEvent.KEYCODE_SPACE);
    }

    public static boolean isNavigationKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        return keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT;
    }
}
