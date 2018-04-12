package com.imkiva.xart.editor.api.skin;

import android.graphics.Color;

import com.imkiva.xart.editor.api.lexer.HighlightSpan;
import com.imkiva.xart.editor.api.lexer.HighlightTokenType;

import java.util.HashMap;

public class Skin {

    private static final int BLACK = 0xFF000000;
    private static final int BLUE = 0xFF0000FF;
    private static final int GREY = 0xFF808080;
    private static final int MAROON = 0xFF800000;
    private static final int INDIGO = 0xFF2A00FF;
    private static final int OLIVE_GREEN = 0xFF3F7F5F;
    private static final int PURPLE = 0xFF7F0055;
    private static final int RED = 0xFFFF0000;
    private static final int WHITE = 0xFFFFFFFF;

    protected HashMap<Colorable, Integer> colorMap = generateDefaultColors();

    protected void setColor(Colorable colorable, int color) {
        colorMap.put(colorable, color);
    }

    public int getColor(Colorable colorable) {
        return colorMap.get(colorable);
    }

    public int getTokenColor(HighlightSpan highlightSpan) {
        HighlightTokenType highlightTokenType = highlightSpan != null ? highlightSpan.getHighlightTokenType() : HighlightTokenType.NORMAL;
        Colorable element = Colorable.FOREGROUND;
        switch (highlightTokenType) {
            case KEYWORD:
                element = Colorable.KEYWORD;
                break;
            case FUNCTION_NAME:
                element = Colorable.NAME;
                break;
            case VAR_NAME:
                element = Colorable.NAME;
                break;
            case SYMBOL:
                element = Colorable.SYMBOL;
                break;
            case COMMENT:
                element = Colorable.COMMENT;
                break;
            case NORMAL:
                element = Colorable.FOREGROUND;
                break;
            case TYPE:
                element = Colorable.KEYWORD;
                break;
            case NOT_USE:
                return highlightSpan.getColor();

        }
        return getColor(element);
    }

    private HashMap<Colorable, Integer> generateDefaultColors() {

        HashMap<Colorable, Integer> colors = new HashMap<Colorable, Integer>(Colorable.values().length);
        colors.put(Colorable.FOREGROUND, BLACK);
        colors.put(Colorable.BACKGROUND, WHITE);
        colors.put(Colorable.SELECTION_FOREGROUND, WHITE);
        colors.put(Colorable.SELECTION_BACKGROUND, MAROON);
        colors.put(Colorable.CURSOR_FOREGROUND, WHITE);
        colors.put(Colorable.CURSOR_BACKGROUND, BLUE);
        colors.put(Colorable.CURSOR_DISABLED, GREY);
        colors.put(Colorable.LINE_HIGHLIGHT, RED);
        colors.put(Colorable.COMMENT, OLIVE_GREEN); // Eclipse 默认颜色
        colors.put(Colorable.KEYWORD, PURPLE);  // Eclipse 默认颜色
        colors.put(Colorable.NAME, Color.parseColor("#795da3"));// 橘色
        colors.put(Colorable.SYMBOL, INDIGO);  // Eclipse 默认颜色
        colors.put(Colorable.LINE_NUMBER_COLOR, Color.GRAY);

        return colors;
    }

    public enum Colorable {
        /**
         * 编辑器前景
         */
        FOREGROUND,
        /**
         * 编辑器背景
         */
        BACKGROUND,
        /**
         * 选择部分的前景
         */
        SELECTION_FOREGROUND,
        /**
         * 选择部分的背景
         */
        SELECTION_BACKGROUND,
        /**
         * 光标前景
         */
        CURSOR_FOREGROUND,
        /**
         * 光标背景
         */
        CURSOR_BACKGROUND,
        /**
         * 光标关闭时的颜色
         */
        CURSOR_DISABLED,
        /**
         * 高亮所在行的颜色
         */
        LINE_HIGHLIGHT,
        /**
         * 注释颜色
         */
        COMMENT,
        /**
         * 关键词颜色
         */
        KEYWORD,
        /**
         * 符号颜色
         */
        SYMBOL,
        /**
         * 行号颜色
         */
        LINE_NUMBER_COLOR,
        /**
         * 如:函数名, 变量名
         */
        NAME,

    }
}
