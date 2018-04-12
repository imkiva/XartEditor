package com.imkiva.xart.editor;

import android.view.View;

import com.imkiva.xart.language.Language;
import com.imkiva.xart.editor.api.IEditorController;
import com.imkiva.xart.editor.api.listener.OnAutoCompletionListener;
import com.imkiva.xart.editor.api.listener.OnEditActionListener;
import com.imkiva.xart.editor.api.skin.Skin;
import com.imkiva.xart.editor.ui.XartEditor;

public class XartEditorController implements IEditorController {

    private XartEditor xartEditor;

    public XartEditorController(XartEditor xartEditor) {
        this.xartEditor = xartEditor;
    }

    @Override
    public void setText(String text) {
        xartEditor.setText(text);
    }

    @Override
    public void undo() {
        xartEditor.undo();
    }

    @Override
    public void redo() {
        xartEditor.redo();
    }

    @Override
    public boolean canUndo() {
        return xartEditor.canUndo();
    }

    @Override
    public boolean canRedo() {
        return xartEditor.canRedo();
    }

    @Override
    public void moveCursor(int pos) {
        xartEditor.moveCursor(pos);
    }

    @Override
    public void moveCursorUp() {
        xartEditor.moveCursorUp();
    }

    @Override
    public void moveCursorDown() {
        xartEditor.moveCursorDown();
    }

    @Override
    public void moveCursorLeft() {
        xartEditor.moveCursorLeft();
    }

    @Override
    public void moveCursorRight() {
        xartEditor.moveCursorRight();
    }

    @Override
    public int getSelectionStart() {
        return xartEditor.getSelectionStart();
    }

    @Override
    public int getSelectionEnd() {
        return xartEditor.getSelectionEnd();
    }

    @Override
    public int getCursorPosition() {
        return xartEditor.getCursorPosition();
    }

    @Override
    public void setSelection(int start, int end) {
        xartEditor.setSelectionRange(start, end);
    }

    @Override
    public void selectAll() {
        xartEditor.selectAll();
    }

    @Override
    public boolean isSelectText() {
        return xartEditor.isSelectText();
    }

    @Override
    public void setToEditMode() {
        xartEditor.setToEditMode();
    }

    @Override
    public void setToViewMode() {
        xartEditor.setToViewMode();
    }

    @Override
    public void append(String appendText) {
        xartEditor.append(appendText);
    }

    @Override
    public void setEditable(boolean editable) {
        if (editable) {
            setToEditMode();
        } else {
            setToViewMode();
        }
    }

    @Override
    public String getText() {
        return xartEditor.getText();
    }

    @Override
    public String substring(int start, int end) {
        return new String(xartEditor.getDoc().subSequence(start, end));
    }

    @Override
    public void insert(int pos, String content) {
        xartEditor.insert(pos, content);
    }

    @Override
    public void delete(int pos, int num) {
        xartEditor.delete(pos, num);
    }

    @Override
    public void replace(int start, int end, String replace) {
        int delta = end - start;
        xartEditor.delete(start, delta);
        xartEditor.insert(start - delta, replace);
    }

    @Override
    public View getEditView() {
        return xartEditor;
    }

    @Override
    public void setOnAutoCompletionListener(OnAutoCompletionListener onAutoCompletionListener) {
        xartEditor.setOnAutoCompletionListener(onAutoCompletionListener);
    }

    @Override
    public void setOnEditActionListener(OnEditActionListener onEditActionListener) {
        xartEditor.setOnEditActionListener(onEditActionListener);
    }

    @Override
    public OnAutoCompletionListener getOnAutoCompletionListener() {
        return xartEditor.getOnAutoCompletionListener();
    }

    @Override
    public OnEditActionListener getOnEditActionListener() {
        return xartEditor.getOnEditActionListener();
    }

    @Override
    public int getTextLength() {
        return xartEditor.length();
    }

    @Override
    public int getCursorLine() {
        return xartEditor.getCursorLine();
    }

    @Override
    public int getLineHeight() {
        return xartEditor.lineHeight();
    }

    @Override
    public void setLanguage(Language language) {
        xartEditor.setLanguage(language);
    }

    @Override
    public int coordinateToCharIndex(int x, int y) {
        return xartEditor.coordinateToCharIndex(x, y);
    }

    @Override
    public int coordinateToCharIndexStrict(int x, int y) {
        return xartEditor.coordinateToCharIndexStrict(x, y);
    }

    @Override
    public void refreshHighlight() {
        xartEditor.refreshSpans();
        xartEditor.updateLeftPadding();
        xartEditor.invalidate();
    }

    @Override
    public void setTextSize(float textSize) {
        xartEditor.setTextSize(textSize);
    }

    @Override
    public float getTextSize() {
        return xartEditor.getTextSize();
    }

    @Override
    public void setTabSpaces(int space) {
        xartEditor.setTabSpaces(space);
    }

    @Override
    public void setSkin(Skin skin) {
        xartEditor.setSkin(skin);
    }

    @Override
    public Skin getSkin() {
        return xartEditor.getSkin();
    }

}
