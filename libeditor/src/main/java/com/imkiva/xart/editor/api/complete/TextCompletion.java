package com.imkiva.xart.editor.api.complete;


import com.imkiva.xart.editor.api.IEditorController;

public abstract class TextCompletion extends AutoCompletion {

    public String completeText;
    public CompleteType completeType;


    public TextCompletion(String title, CompleteType completeType) {
        super(title);
        this.completeType = completeType;
    }

    public TextCompletion(String title, String description, CompleteType completeType) {
        super(title, description);
        this.completeType = completeType;
    }

    public TextCompletion(String title, String description, String completeText, CompleteType completeType) {
        super(title, description);
        this.completeText = completeText;
        this.completeType = completeType;
    }


    public TextCompletion(String title, String completeText) {
        super(title);
        this.completeText = completeText;
        this.completeType = CompleteType.OTHER;
    }

    @Override
    public void onComplete(IEditorController editor, String filter, int filterTimePos) {
        if (completeText != null && completeText.length() > 0) {
            int length = filter.length();
            if (filter.endsWith(".")) {
                length--;
            }
            editor.delete(filterTimePos - length, length);
            int baseOffset = filterTimePos - length;
            editor.moveCursor(baseOffset);
            editor.append(completeText);
        }
    }

    @Override
    public CompleteType getCompleteType() {
        return completeType;
    }


    public String getCompleteText() {
        return completeText;
    }

    public void setCompleteText(String completeText) {
        this.completeText = completeText;
    }


}
