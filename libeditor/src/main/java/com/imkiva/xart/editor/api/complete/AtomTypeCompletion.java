package com.imkiva.xart.editor.api.complete;

public class AtomTypeCompletion extends TextCompletion {

    public AtomTypeCompletion(String title) {
        this(title, "Atom Type");
    }

    public AtomTypeCompletion(String title, String description) {
        super(title, description, CompleteType.ATOM_TYPE);
        setCompleteText(title);
    }

}
