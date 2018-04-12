package com.imkiva.xart.editor.api.lexer;

public final class HighlightSpan {
    private int offset;
    private HighlightTokenType highlightTokenType;
    private int color;

    public static final HighlightSpan FIRST_TOKEN = new HighlightSpan(0, HighlightTokenType.NORMAL);

    public HighlightSpan(int offset, HighlightTokenType color) {
        this.offset = offset;
        highlightTokenType = color;
    }

    public HighlightSpan(int _offset, int color) {
        this.offset = _offset;
        this.color = color;
        this.highlightTokenType = HighlightTokenType.NOT_USE;
    }

    public final int getOffset() {
        return offset;
    }

    public final void setOffset(int value) {
        offset = value;
    }

    public final HighlightTokenType getHighlightTokenType() {
        return highlightTokenType;
    }

    public final void setHighlightTokenType(HighlightTokenType value) {
        highlightTokenType = value;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
