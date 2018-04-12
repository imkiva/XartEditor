package com.imkiva.xart.editor.api.complete;

import com.imkiva.xart.editor.api.IEditorController;

public interface IAutoCompleteFactory {

    IAutoCompleteController create(IEditorController editor);

}
