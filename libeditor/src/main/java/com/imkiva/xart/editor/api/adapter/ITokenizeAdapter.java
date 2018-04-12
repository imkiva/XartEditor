package com.imkiva.xart.editor.api.adapter;

import com.imkiva.xart.editor.api.lexer.Flag;
import com.imkiva.xart.editor.api.lexer.HighlightSpan;
import com.imkiva.xart.editor.api.lexer.HighlightTokenType;

import java.util.Collections;
import java.util.List;

public interface ITokenizeAdapter {


    List<HighlightSpan> DEFAULT_HIGHLIGHT_SPANS = Collections.singletonList(new HighlightSpan(0, HighlightTokenType.NORMAL));

    /**
     * 词法分析
     *
     * @param flag      是否需要终止解析
     * @param needToLex 需要词法分析的文本副本
     */
    List<HighlightSpan> tokenize(Flag flag, String needToLex);

}
