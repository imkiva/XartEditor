package com.imkiva.xart.editor.api;

import android.content.Context;

public interface IEditorFactory {
    IEditorController createEditor(Context context);
}
