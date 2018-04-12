package com.imkiva.xart.editor.api.complete;

import android.widget.ImageView;
import android.widget.TextView;

public interface IDefaultCompleteApply {

    void applyView(AutoCompletion.CompleteType type, TextView title, TextView desc, ImageView imageView);
}
