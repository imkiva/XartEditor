package com.imkiva.xart.editor.api.complete;

import com.imkiva.xart.editor.api.IEditorController;

public abstract class FunctionCompletion extends TextCompletion {


    public FunctionCompletion(String functionName, String functionDesc) {
        super(functionName, functionDesc, CompleteType.FUNCTION);
        setCompleteText(functionName);
    }


    public String getEndIdentityStart() {
        return "(";
    }

    public String getEndIdentityEnd() {
        return ")";
    }


    /**
     * @return 是否包含参数
     */
    public abstract boolean isContainParams();

    @Override
    public void onComplete(IEditorController editor, String filter, int filterTimePos) {
        super.onComplete(editor, filter, filterTimePos);
        afterComplete(editor);
    }

    /**
     * 当FunctionName补全完以后,
     * 接下来要补全括号等等.
     *
     * @param editor Editor
     */
    public void afterComplete(IEditorController editor) {
        editor.append(getEndIdentityStart());
        if (!isContainParams()) {
            editor.append(getEndIdentityEnd());
        }

    }
}
