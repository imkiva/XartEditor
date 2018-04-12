package com.imkiva.xart.editor.api.complete;

public class ParamCompletion extends TextCompletion {

    public ParamCompletion(String title, String desc) {
        super(title, desc, CompleteType.PARAM);
        setCompleteText(title);
    }

    public ParamCompletion(String title) {
        this(title, "Param");
    }

}
