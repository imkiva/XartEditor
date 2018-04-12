package com.imkiva.xart.editor.api.complete;

public class FieldCompletion extends TextCompletion {

    public FieldCompletion(String title) {
        this(title, "Field");
    }

    public FieldCompletion(String title, String description) {
        super(title, description, CompleteType.FIELD);
        setCompleteText(title);
    }

}
