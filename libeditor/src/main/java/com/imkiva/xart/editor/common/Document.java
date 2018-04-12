package com.imkiva.xart.editor.common;

/**
 * A decorator of TextBuffer that adds word-wrap capabilities.
 * <p>
 * Positions for word wrap row breaks are stored here.
 * Word-wrap is enabled by default.
 */
public class Document extends TextBuffer {

    /**
     * Contains info related to printing of characters, display size and so on
     */

    /**
     * A table containing the character offset of every row in the document.
     * Values are valid only in word-wrap mode
     */
    public Document() {
        super();
    }


    @Override
    public synchronized void delete(int charOffset, int totalChars, long timestamp, boolean undoable) {
        super.delete(charOffset, totalChars, timestamp, undoable);

    }

    @Override
    public synchronized void insert(char[] c, int charOffset, long timestamp, boolean undoable) {
        super.insert(c, charOffset, timestamp, undoable);
    }

    @Override
    /**
     * Moves _gapStartIndex by displacement units. Note that displacement can be
     * negative and will move _gapStartIndex to the left.
     *
     * Only UndoStack should use this method to carry out a simple undo/redo
     * of insertions/deletions. No error checking is done.
     */
    synchronized void shiftGapStart(int displacement) {
        super.shiftGapStart(displacement);
    }


}
