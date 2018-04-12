package com.imkiva.xart.editor.api.listener;

public interface OnEditActionListener {
    void onPaste(String text);

    void onInsert(int pos);

    void onDelete(int pos, int nums);

    void onUpdateCursor();
}
