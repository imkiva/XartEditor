package com.imkiva.xart.editor.common;

import com.imkiva.xart.language.ParserDefinition;
import com.imkiva.xart.editor.api.lexer.Flag;
import com.imkiva.xart.editor.api.lexer.HighlightSpan;
import com.imkiva.xart.editor.api.lexer.HighlightTokenType;
import com.imkiva.xart.editor.model.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class TextBuffer {
    // gap size must be > 0 to insert into full buffers successfully
    protected final static int MIN_GAP_SIZE = 50;
    protected char[] _contents;
    protected int _gapStartIndex;
    /**
     * One past end of gap
     */
    protected int _gapEndIndex;
    protected int _lineCount;
    protected String _originalFormat;
    protected String _originalEOLType;
    /**
     * Continuous seq of chars that have the same format (color, font, etc.)
     */
    protected List<HighlightSpan> _Pairs;
    /**
     * The number of times memory is allocated for the buffer
     */
    private int _allocMultiplier;
    private TextBufferCache _cache;
    private UndoStack _undoStack;


    public TextBuffer() {
        _contents = new char[MIN_GAP_SIZE + 1]; // extra char for EOF
        _contents[MIN_GAP_SIZE] = ParserDefinition.EOF;
        _allocMultiplier = 1;
        _gapStartIndex = 0;
        _gapEndIndex = MIN_GAP_SIZE;
        _lineCount = 1;
        _cache = new TextBufferCache();
        _undoStack = new UndoStack(this);
        _originalFormat = EncodingScheme.TEXT_ENCODING_UTF8;
        _originalEOLType = EncodingScheme.LINE_BREAK_LF;
    }

    /**
     * Calculate the implementation size of the char array needed to store
     * textSize number of characters.
     * The implementation size may be greater than textSize because of buffer
     * space, cached characters and so on.
     *
     * @param textSize
     * @return The size, measured in number of chars, required by the
     * implementation to store textSize characters, or -1 if the request
     * cannot be satisfied
     */
    public static int memoryNeeded(int textSize) {
        long bufferSize = textSize + MIN_GAP_SIZE + 1; // extra char for EOF
        if (bufferSize < Integer.MAX_VALUE) {
            return (int) bufferSize;
        }
        return -1;
    }

    synchronized public void setBuffer(char[] newBuffer, String encoding,
                                       String EOLstyle, int textSize, int lineCount) {
        _originalFormat = encoding;
        _originalEOLType = EOLstyle;
        _contents = newBuffer;
        initGap(textSize);
        _lineCount = lineCount;
        _allocMultiplier = 1;
    }


    synchronized public void write(OutputStream byteStream, String encoding,
                                   String EOLstyle, Flag abort)
            throws IOException {
        String enc = encoding;
        if (encoding.equals(EncodingScheme.TEXT_ENCODING_AUTO)) {
            enc = _originalFormat;
        }
        String EOL = EOLstyle;
        if (EOLstyle.equals(EncodingScheme.LINE_BREAK_AUTO)) {
            EOL = _originalEOLType;
        }

        _originalFormat = enc;
        _originalEOLType = EOL;
    }


    /**
     * Returns a string of text corresponding to the line with index lineNumber.
     *
     * @param lineNumber The index of the line of interest
     * @return The text on lineNumber, or an empty string if the line does not exist
     */
    synchronized public String getLine(int lineNumber) {
        int startIndex = getLineOffset(lineNumber);

        if (startIndex < 0) {
            return "";
        }
        int lineSize = getLineSize(lineNumber);

        return new String(subSequence(startIndex, lineSize));
    }

    /**
     * Get the offset of the first character of the line with index lineNumber.
     * The offset is counted from the beginning of the text.
     *
     * @param lineNumber The index of the line of interest
     * @return The character offset of lineNumber, or -1 if the line does not exist
     */
    synchronized public int getLineOffset(int lineNumber) {
        if (lineNumber < 0) {
            return -1;
        }

        // start search from nearest known lineIndex~charOffset pair
        Pair cachedEntry = _cache.getNearestLine(lineNumber);
        int cachedLine = cachedEntry.getFirst();
        int cachedOffset = cachedEntry.getSecond();

        int offset;
        if (lineNumber > cachedLine) {
            offset = findCharOffset(lineNumber, cachedLine, cachedOffset);
        } else if (lineNumber < cachedLine) {
            offset = findCharOffsetBackward(lineNumber, cachedLine, cachedOffset);
        } else {
            offset = cachedOffset;
        }

        if (offset >= 0) {
            // seek successful
            _cache.updateEntry(lineNumber, offset);
        }

        return offset;
    }

    /*
     * Precondition: startOffset is the offset of startLine
     */
    private int findCharOffset(int targetLine, int startLine, int startOffset) {
        int workingLine = startLine;
        int offset = logicalToRealIndex(startOffset);


        while ((workingLine < targetLine) && (offset < _contents.length)) {
            if (_contents[offset] == ParserDefinition.NEWLINE) {
                ++workingLine;
            }
            ++offset;

            // skip the gap
            if (offset == _gapStartIndex) {
                offset = _gapEndIndex;
            }
        }

        if (workingLine != targetLine) {
            return -1;
        }
        return realToLogicalIndex(offset);
    }

    /*
     * Precondition: startOffset is the offset of startLine
     */
    private int findCharOffsetBackward(int targetLine, int startLine, int startOffset) {
        if (targetLine == 0) {
            return 0;
        }


        int workingLine = startLine;
        int offset = logicalToRealIndex(startOffset);
        while (workingLine > (targetLine - 1) && offset >= 0) {
            // skip behind the gap
            if (offset == _gapEndIndex) {
                offset = _gapStartIndex;
            }
            --offset;

            if (_contents[offset] == ParserDefinition.NEWLINE) {
                --workingLine;
            }

        }

        int charOffset;
        if (offset >= 0) {
            // now at the '\n' of the line before targetLine
            charOffset = realToLogicalIndex(offset);
            ++charOffset;
        } else {
            charOffset = -1;
        }

        return charOffset;
    }

    /**
     * Get the line number that charOffset is on
     *
     * @return The line number that charOffset is on, or -1 if charOffset is invalid
     */
    synchronized public int findLineNumber(int charOffset) {
        if (!isValid(charOffset)) {
            return -1;
        }

        Pair cachedEntry = _cache.getNearestCharOffset(charOffset);
        int line = cachedEntry.getFirst();
        int offset = logicalToRealIndex(cachedEntry.getSecond());
        int targetFirst = logicalToRealIndex(charOffset);
        int lastKnownLine = -1;
        int lastKnownCharOffset = -1;

        if (targetFirst > offset) {
            // search forward
            while ((offset < targetFirst) && (offset < _contents.length)) {
                if (_contents[offset] == ParserDefinition.NEWLINE) {
                    ++line;
                    lastKnownLine = line;
                    lastKnownCharOffset = realToLogicalIndex(offset) + 1;
                }

                ++offset;
                // skip the gap
                if (offset == _gapStartIndex) {
                    offset = _gapEndIndex;
                }
            }
        } else if (targetFirst < offset) {
            // search backward
            while ((offset > targetFirst) && (offset > 0)) {
                // skip behind the gap
                if (offset == _gapEndIndex) {
                    offset = _gapStartIndex;
                }
                --offset;

                if (_contents[offset] == ParserDefinition.NEWLINE) {
                    lastKnownLine = line;
                    lastKnownCharOffset = realToLogicalIndex(offset) + 1;
                    --line;
                }
            }
        }


        if (offset == targetFirst) {
            if (lastKnownLine != -1) {
                // cache the lookup entry
                _cache.updateEntry(lastKnownLine, lastKnownCharOffset);
            }
            return line;
        } else {
            return -1;
        }
    }


    /**
     * Finds the number of char on the specified line.
     * All valid lines contain at least one char, which may be a non-printable
     * one like \n, \t or EOF.
     *
     * @return The number of chars in lineNumber, or 0 if the line does not exist.
     */
    synchronized public int getLineSize(int lineNumber) {
        int lineLength = 0;
        int pos = getLineOffset(lineNumber);

        if (pos != -1) {
            pos = logicalToRealIndex(pos);
            //TODO consider adding check for (pos < _contents.length) in case EOF is not properly set
            while (_contents[pos] != ParserDefinition.NEWLINE &&
                    _contents[pos] != ParserDefinition.EOF) {
                ++lineLength;
                ++pos;

                // skip the gap
                if (pos == _gapStartIndex) {
                    pos = _gapEndIndex;
                }
            }
            ++lineLength; // account for the line terminator char
        }

        return lineLength;
    }

    /**
     * Gets the char at charOffset
     * Does not do bounds-checking.
     *
     * @return The char at charOffset. If charOffset is invalid, the result
     * is undefined.
     */
    synchronized public char charAt(int charOffset) {
        return _contents[logicalToRealIndex(charOffset)];
    }

    /**
     * Gets up to maxChars number of chars starting at charOffset
     *
     * @return The chars starting from charOffset, up to a maximum of maxChars.
     * An empty array is returned if charOffset is invalid or maxChars is
     * non-positive.
     */
    synchronized public char[] subSequence(int charOffset, int maxChars) {
        if (!isValid(charOffset) || maxChars <= 0) {
            return new char[0];
        }
        int totalChars = maxChars;
        if ((charOffset + totalChars) > getTextLength()) {
            totalChars = getTextLength() - charOffset;
        }
        int realIndex = logicalToRealIndex(charOffset);
        char[] chars = new char[totalChars];

        for (int i = 0; i < totalChars; ++i) {
            chars[i] = _contents[realIndex];
            ++realIndex;
            // skip the gap
            if (realIndex == _gapStartIndex) {
                realIndex = _gapEndIndex;
            }
        }

        return chars;
    }

    /**
     * Gets charCount number of consecutive characters starting from _gapStartIndex.
     * <p>
     * Only UndoStack should use this method. No error checking is done.
     */
    char[] gapSubSequence(int charCount) {
        char[] chars = new char[charCount];

        System.arraycopy(_contents, _gapStartIndex, chars, 0, charCount);

        return chars;
    }

    /**
     * Insert all characters in c into position charOffset.
     * <p>
     * No error checking is done
     */
    public synchronized void insert(char[] c, int charOffset, long timestamp,
                                    boolean undoable) {
        if (undoable) {
            _undoStack.captureInsert(charOffset, c.length, timestamp);
        }

        int insertIndex = logicalToRealIndex(charOffset);

        // shift gap to insertion point
        if (insertIndex != _gapEndIndex) {
            if (isBeforeGap(insertIndex)) {
                shiftGapLeft(insertIndex);
            } else {
                shiftGapRight(insertIndex);
            }
        }

        if (c.length >= gapSize()) {
            growBufferBy(c.length - gapSize());
        }

        for (char aC : c) {
            if (aC == ParserDefinition.NEWLINE) {
                ++_lineCount;
            }
            _contents[_gapStartIndex] = aC;
            ++_gapStartIndex;
        }

        _cache.invalidateCache(charOffset);
    }

    /**
     * Deletes up to totalChars number of char starting from position
     * charOffset, inclusive.
     * <p>
     * No error checking is done
     */
    public synchronized void delete(int charOffset, int totalChars, long timestamp,
                                    boolean undoable) {
        if (undoable) {
            _undoStack.captureDelete(charOffset, totalChars, timestamp);
        }

        int newGapStart = charOffset + totalChars;

        // shift gap to deletion point
        if (newGapStart != _gapStartIndex) {
            if (isBeforeGap(newGapStart)) {
                shiftGapLeft(newGapStart);
            } else {
                shiftGapRight(newGapStart + gapSize());
            }
        }

        // increase gap size
        for (int i = 0; i < totalChars; ++i) {
            --_gapStartIndex;
            if (_contents[_gapStartIndex] == ParserDefinition.NEWLINE) {
                --_lineCount;
            }
        }

        _cache.invalidateCache(charOffset);
    }

    /**
     * Moves _gapStartIndex by displacement units. Note that displacement can be
     * negative and will move _gapStartIndex to the left.
     * <p>
     * Only UndoStack should use this method to carry out a simple undo/redo
     * of insertions/deletions. No error checking is done.
     */
    synchronized void shiftGapStart(int displacement) {
        if (displacement >= 0) {
            _lineCount += countNewlines(_gapStartIndex, displacement);
        } else {
            _lineCount -= countNewlines(_gapStartIndex + displacement, -displacement);
        }

        _gapStartIndex += displacement;
        _cache.invalidateCache(realToLogicalIndex(_gapStartIndex - 1) + 1);
    }

    //does NOT skip the gap when examining consecutive positions
    private int countNewlines(int start, int totalChars) {
        int newlines = 0;
        for (int i = start; i < (start + totalChars); ++i) {
            if (_contents[i] == ParserDefinition.NEWLINE) {
                ++newlines;
            }
        }

        return newlines;
    }

    /**
     * Adjusts gap so that _gapStartIndex is at newGapStart
     */
    final protected void shiftGapLeft(int newGapStart) {
        while (_gapStartIndex > newGapStart) {
            --_gapEndIndex;
            --_gapStartIndex;
            _contents[_gapEndIndex] = _contents[_gapStartIndex];
        }
    }

    /**
     * Adjusts gap so that _gapEndIndex is at newGapEnd
     */
    final protected void shiftGapRight(int newGapEnd) {
        while (_gapEndIndex < newGapEnd) {
            _contents[_gapStartIndex] = _contents[_gapEndIndex];
            ++_gapStartIndex;
            ++_gapEndIndex;
        }
    }

    /**
     * Create a gap at the start of _contents[] and tack a EOF at the end.
     * Precondition: real contents are from _contents[0] to _contents[contentsLength-1]
     */
    protected void initGap(int contentsLength) {
        int toPosition = _contents.length - 1;
        _contents[toPosition--] = ParserDefinition.EOF; // mark end of file
        int fromPosition = contentsLength - 1;
        while (fromPosition >= 0) {
            _contents[toPosition--] = _contents[fromPosition--];
        }
        _gapStartIndex = 0;
        _gapEndIndex = toPosition + 1; // went one-past in the while loop
    }

    /**
     * Copies _contents into a buffer that is larger by
     * minIncrement + INITIAL_GAP_SIZE * _allocCount bytes.
     * <p>
     * _allocMultiplier doubles on every call to this method, to avoid the
     * overhead of repeated allocations.
     */
    protected void growBufferBy(int minIncrement) {
        //TODO handle new size > MAX_INT or allocation failure
        int increasedSize = minIncrement + MIN_GAP_SIZE * _allocMultiplier;
        char[] temp = new char[_contents.length + increasedSize];
        int i = 0;
        while (i < _gapStartIndex) {
            temp[i] = _contents[i];
            ++i;
        }

        i = _gapEndIndex;
        while (i < _contents.length) {
            temp[i + increasedSize] = _contents[i];
            ++i;
        }

        _gapEndIndex += increasedSize;
        _contents = temp;
        _allocMultiplier <<= 1;
    }

    /**
     * Returns the total number of characters in the text, including the
     * EOF sentinel char
     */
    final synchronized public int getTextLength() {
        return _contents.length - gapSize();
    }

    synchronized public int getLineCount() {
        return _lineCount;
    }

    final synchronized public boolean isValid(int charOffset) {
        return (charOffset >= 0 && charOffset < getTextLength());
    }

    final protected int gapSize() {
        return _gapEndIndex - _gapStartIndex;
    }

    final protected int logicalToRealIndex(int i) {
        if (isBeforeGap(i)) {
            return i;
        } else {
            return i + gapSize();
        }
    }

    final protected int realToLogicalIndex(int i) {
        if (isBeforeGap(i)) {
            return i;
        } else {
            return i - gapSize();
        }
    }

    final protected boolean isBeforeGap(int i) {
        return i < _gapStartIndex;
    }

    public String getEncodingScheme() {
        return _originalFormat;
    }

    public String getEOLType() {
        return _originalEOLType;
    }

    public void clearSpans() {
        _Pairs = new ArrayList<HighlightSpan>();
        _Pairs.add(new HighlightSpan(0, HighlightTokenType.NORMAL));
    }

    public List<HighlightSpan> getSpans() {
        return _Pairs;
    }

    /**
     * Sets the Pairs to use in the document.
     * Pairs are continuous sequences of characters that have the same format
     * like color, font, etc.
     *
     * @param highlightSpans A collection of Pairs, where Pair.first is the start
     *              position of the token, and Pair.second is the type of the token.
     */
    public void setSpans(List<HighlightSpan> highlightSpans) {
        _Pairs = highlightSpans;
    }

    /**
     * Returns true if in batch edit mode
     */
    public boolean isBatchEdit() {
        return _undoStack.isBatchEdit();
    }

    /**
     * Signals the beginning of a series of insert/delete operations that can be
     * undone/redone as a single unit
     */
    public void beginBatchEdit() {
        _undoStack.beginBatchEdit();
    }

    /**
     * Signals the end of a series of insert/delete operations that can be
     * undone/redone as a single unit
     */
    public void endBatchEdit() {
        _undoStack.endBatchEdit();
    }

    public boolean canUndo() {
        return _undoStack.canUndo();
    }

    public boolean canRedo() {
        return _undoStack.canRedo();
    }

    public int undo() {
        return _undoStack.undo();
    }

    public int redo() {
        return _undoStack.redo();
    }
}
