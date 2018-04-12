package com.imkiva.xart.editor.render;

import android.graphics.Color;

public class SyntaxTip {

    public static int WARN = Color.YELLOW;
    public static int ERROR = Color.RED;
    public static int NORMAL = Color.GREEN;

    public boolean isEnd = true;

    public int tipsColor = NORMAL;
    public int lineNumberColor = NORMAL;
    public String tipsMessage = "";
    public boolean underline = true;

    public SyntaxTip(int tipsColor, String tipsMessage) {
        this.tipsColor = tipsColor;
        this.tipsMessage = tipsMessage;
    }

    public SyntaxTip(int tipsColor, String tipsMessage, boolean underline) {
        this.tipsColor = tipsColor;
        this.tipsMessage = tipsMessage;
        this.underline = underline;
    }

    public SyntaxTip(int tipsColor, int lineNumberColor, String tipsMessage, boolean underline) {
        this.tipsColor = tipsColor;
        this.lineNumberColor = lineNumberColor;
        this.tipsMessage = tipsMessage;
        this.underline = underline;
    }

    public SyntaxTip(int tipsColor, int lineNumberColor, String tipsMessage) {
        this.tipsColor = tipsColor;
        this.lineNumberColor = lineNumberColor;
        this.tipsMessage = tipsMessage;
    }

    public static SyntaxTip create(int color, String tip) {
        return new SyntaxTip(color, tip);
    }

    public static SyntaxTip create(String tip) {
        return new SyntaxTip(NORMAL, tip);
    }


}
