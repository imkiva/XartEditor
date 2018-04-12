package com.imkiva.xart.editor.api;

import android.view.View;

import com.imkiva.xart.language.Language;
import com.imkiva.xart.editor.api.listener.OnAutoCompletionListener;
import com.imkiva.xart.editor.api.listener.OnEditActionListener;
import com.imkiva.xart.editor.api.skin.Skin;

public interface IEditorController {
    void setText(String text);

    void undo();

    void redo();

    boolean canUndo();

    boolean canRedo();

    void moveCursor(int pos);

    void moveCursorUp();

    void moveCursorDown();

    void moveCursorLeft();

    void moveCursorRight();

    int getSelectionStart();

    int getSelectionEnd();

    int getCursorPosition();

    void setSelection(int start, int end);

    void selectAll();

    boolean isSelectText();

    void setToEditMode();

    void setToViewMode();

    void append(String appendText);

    void setEditable(boolean editable);

    String getText();

    String substring(int start, int end);

    void insert(int pos, String content);

    void delete(int pos, int num);

    void replace(int start, int end, String replace);

    View getEditView();

    void setOnAutoCompletionListener(OnAutoCompletionListener onAutoCompletionListener);

    void setOnEditActionListener(OnEditActionListener onEditActionListener);

    OnAutoCompletionListener getOnAutoCompletionListener();

    OnEditActionListener getOnEditActionListener();

    int getTextLength();

    int getCursorLine();

    int getLineHeight();

    void setLanguage(Language language);

    int coordinateToCharIndex(int x, int y);

    int coordinateToCharIndexStrict(int x, int y);

    void refreshHighlight();

    void setTextSize(float textSize);

    float getTextSize();

    void setTabSpaces(int space);

    void setSkin(Skin skin);

    Skin getSkin();
}
