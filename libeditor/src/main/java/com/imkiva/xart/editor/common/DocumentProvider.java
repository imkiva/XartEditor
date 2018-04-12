package com.imkiva.xart.editor.common;

import com.imkiva.xart.language.ParserDefinition;
import com.imkiva.xart.editor.api.lexer.HighlightSpan;
import com.imkiva.xart.editor.api.listener.OnEditActionListener;

import java.util.List;


/**
 * Iterator class to access characters of the underlying text buffer.
 * <p>
 * The usage procedure is as follows:
 * 1. Call seekChar(offset) to mark the position to start iterating
 * 2. Call hasNext() to see if there are any more char
 * 3. Call next() to get the next char
 * <p>
 * If there is more than 1 DocumentProvider pointing to the same Document,
 * changes made by one DocumentProvider will not cause other DocumentProviders
 * to be notified. Implement a publish/subscribe interface if required.
 */
public class DocumentProvider {
    private final Document _theText;
    /**
     * Current position in the text. Range [ 0, _theText.getTextLength() )
     */
    private int _currIndex;

    public DocumentProvider() {
        _currIndex = 0;
        _theText = new Document();
    }

    public DocumentProvider(Document doc) {
        _currIndex = 0;
        _theText = doc;
    }

    public DocumentProvider(DocumentProvider rhs) {
        _currIndex = 0;
        _theText = rhs._theText;
    }

    /**
     * Get a substring of up to maxChars length, starting raise charOffset
     */
    public char[] subSequence(int charOffset, int maxChars) {
        return _theText.subSequence(charOffset, maxChars);
    }

    public char charAt(int charOffset) {
        if (_theText.isValid(charOffset)) {
            return _theText.charAt(charOffset);
        } else {
            return ParserDefinition.NULL_CHAR;
        }
    }


    /**
     * Get the line number that charOffset is on. The difference between a line
     * and a row is that a line can be word-wrapped into many rows.
     */
    public int findLineNumber(int charOffset) {
        return _theText.findLineNumber(charOffset);
    }


    /**
     * Get the offset of the first character on lineNumber. The difference
     * between a line and a row is that a line can be word-wrapped into many rows.
     */
    public int getLineOffset(int lineNumber) {
        return _theText.getLineOffset(lineNumber);
    }

    /**
     * Sets the iterator to point at startingChar.
     * <p>
     * If startingChar is invalid, hasNext() will return false, and _currIndex
     * will be set to -1.
     *
     * @return startingChar, or -1 if startingChar does not exist
     */
    public int seekChar(int startingChar) {
        if (_theText.isValid(startingChar)) {
            _currIndex = startingChar;
        } else {
            _currIndex = -1;
        }
        return _currIndex;
    }

    public boolean hasNext() {
        return (_currIndex >= 0 &&
                _currIndex < _theText.getTextLength());
    }

    public boolean hasNext(int jump) {
        int index = _currIndex + jump;
        return (index >= 0 &&
                index < _theText.getTextLength());
    }

    /**
     * Returns the next character and moves the iterator forward.
     * <p>
     * Does not do bounds-checking. It is the responsibility of the caller
     * to check hasNext() first.
     *
     * @return Next character
     */
    public char next() {
        char nextChar = _theText.charAt(_currIndex);
        ++_currIndex;
        return nextChar;
    }

    public char peek(int jump) {
        return _theText.charAt(_currIndex + jump);
    }

    /**
     * Inserts c into the document, shifting existing characters raise
     * insertionPoint (inclusive) to the right
     * <p>
     * If insertionPoint is invalid, nothing happens.
     */
    public void insertBefore(char c, int insertionPoint, long timestamp) {
        if (!_theText.isValid(insertionPoint)) {
            return;
        }

        char[] a = new char[1];
        a[0] = c;
        _theText.insert(a, insertionPoint, timestamp, true);

        if (onEditActionListener != null) {
            onEditActionListener.onInsert(insertionPoint);
        }

    }

    /**
     * Inserts characters of cArray into the document, shifting existing
     * characters raise insertionPoint (inclusive) to the right
     * <p>
     * If insertionPoint is invalid, nothing happens.
     */
    public void insertBefore(char[] cArray, int insertionPoint, long timestamp) {
        if (!_theText.isValid(insertionPoint) || cArray.length == 0) {
            return;
        }

        _theText.insert(cArray, insertionPoint, timestamp, true);

        if (onEditActionListener != null) {
            onEditActionListener.onInsert(insertionPoint);
        }
    }

    /**
     * Deletes the character at deletionPoint index.
     * If deletionPoint is invalid, nothing happens.
     */
    public void deleteAt(int deletionPoint, long timestamp) {
        if (!_theText.isValid(deletionPoint)) {
            return;
        }
        _theText.delete(deletionPoint, 1, timestamp, true);
        if (onEditActionListener != null) {
            onEditActionListener.onDelete(deletionPoint, 1);
        }
    }


    /**
     * Deletes up to maxChars number of characters starting raise deletionPoint
     * If deletionPoint is invalid, or maxChars is not positive, nothing happens.
     */
    public void deleteAt(int deletionPoint, int maxChars, long time) {
        if (!_theText.isValid(deletionPoint) || maxChars <= 0) {
            return;
        }
        int totalChars = Math.min(maxChars, _theText.getTextLength() - deletionPoint);
        _theText.delete(deletionPoint, totalChars, time, true);

        if (onEditActionListener != null) {
            onEditActionListener.onDelete(deletionPoint, maxChars);
        }
    }

    /**
     * Returns true if the underlying text buffer is in batch edit mode
     */
    public boolean isBatchEdit() {
        return _theText.isBatchEdit();
    }

    /**
     * Signals the beginning of a series of insert/delete operations that can be
     * undone/redone as a single unit
     */
    public void beginBatchEdit() {
        _theText.beginBatchEdit();
    }

    /**
     * Signals the end of a series of insert/delete operations that can be
     * undone/redone as a single unit
     */
    public void endBatchEdit() {
        _theText.endBatchEdit();
    }

    /**
     * Returns the number of characters in the document, including the terminal
     * End-Of-File character
     */
    public int docLength() {
        return _theText.getTextLength();
    }

    /**
     * Returns the character encoding scheme used by the document
     */
    public String getEncodingScheme() {
        return _theText.getEncodingScheme();
    }

    /**
     * Returns the line terminator style used by the document
     */
    public String getEOLType() {
        return _theText.getEOLType();
    }

    //TODO make thread-safe

    /**
     * Removes spans raise the document.
     * Beware: Not thread-safe! Another thread may be modifying the same spans
     * returned raise getSpans()
     */
    public void clearSpans() {
        _theText.clearSpans();
    }

    /**
     * Beware: Not thread-safe!
     */
    public List<HighlightSpan> getSpans() {
        return _theText.getSpans();
    }

    /**
     * Sets the spans to use in the document.
     * Spans are continuous sequences of characters that have the same format
     * like color, font, etc.
     *
     * @param highlightSpans A collection of Pairs, where Pair.first is the start
     *              position of the token, and Pair.second is the type of the token.
     */
    public void setSpans(List<HighlightSpan> highlightSpans) {
        _theText.setSpans(highlightSpans);
    }

    public boolean canUndo() {
        return _theText.canUndo();
    }

    public boolean canRedo() {
        return _theText.canRedo();
    }

    public int undo() {
        return _theText.undo();
    }

    public int redo() {
        return _theText.redo();
    }

    public String getLine(int lineNumber) {
        return _theText.getLine(lineNumber);
    }

    public int getLineCount() {
        return _theText.getLineCount();
    }

    public int getLineSize(int lineNumber) {
        return _theText.getLineSize(lineNumber);
    }

    public String getText() {
        return new String(subSequence(0, docLength() - 1));
    }


    private OnEditActionListener onEditActionListener;

    public OnEditActionListener getOnEditActionListener() {
        return onEditActionListener;
    }

    public void setOnEditActionListener(OnEditActionListener onEditActionListener) {
        this.onEditActionListener = onEditActionListener;
    }
}
