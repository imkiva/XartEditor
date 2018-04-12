package com.imkiva.xart.editor;

import android.content.Context;

import com.imkiva.xart.editor.api.IEditorController;
import com.imkiva.xart.editor.api.IEditorFactory;
import com.imkiva.xart.editor.ui.XartEditor;

public class XartEditorFactory implements IEditorFactory {
    @Override
    public IEditorController createEditor(Context context) {
        return new XartEditorController(new XartEditor(context));
    }
}
