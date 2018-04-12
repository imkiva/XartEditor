package com.imkiva.xart.editor.api.complete;

import android.graphics.drawable.Drawable;

import java.util.List;

public interface IAutoCompleteController {

    void setOnCompleteTextListener(OnCompleteTextListener onCompleteTextListener);

    OnCompleteTextListener getOnCompleteTextListener();

    void setKeywords(List<AutoCompletion> keywords);

    void setCompleteList(List<AutoCompletion> completeList);

    void popup(String filterKeywords);

    void dismiss();

    boolean isShowing();

    void updateLocation();

    void setBackground(Drawable drawable);

    void setBackgroundColor(int color);


    public interface OnCompleteTextListener {
        void onCompleteText(String filter, AutoCompletion completion);
    }
}
