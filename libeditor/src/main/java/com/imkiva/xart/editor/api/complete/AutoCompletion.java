package com.imkiva.xart.editor.api.complete;

import android.widget.ImageView;
import android.widget.TextView;

import com.imkiva.xart.ServiceManager;
import com.imkiva.xart.editor.api.IEditorController;

public abstract class AutoCompletion {

    public String title;
    public String description;

    public AutoCompletion(String title) {
        this.title = title;
    }

    public AutoCompletion(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public abstract void onComplete(IEditorController editor, String filter, int filterTimePos);

    public abstract CompleteType getCompleteType();

    public void applyView(TextView title, TextView desc, ImageView imageView) {
        IDefaultCompleteApply apply = ServiceManager.get().getService(IDefaultCompleteApply.class);
        apply.applyView(getCompleteType(), title, desc, imageView);
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public enum CompleteType {
        KEYWORD,
        FUNCTION,
        FIELD,
        LOCAL_VAR,
        PARAM,
        IMPORT,
        ATOM_TYPE,
        OTHER
    }

}
