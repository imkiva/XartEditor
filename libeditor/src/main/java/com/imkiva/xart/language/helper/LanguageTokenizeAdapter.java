package com.imkiva.xart.language.helper;

import com.imkiva.xart.language.JFlexLexer;
import com.imkiva.xart.language.ParserDefinition;
import com.imkiva.xart.editor.api.adapter.ITokenizeAdapter;
import com.imkiva.xart.editor.api.lexer.Flag;
import com.imkiva.xart.editor.api.lexer.HighlightSpan;
import com.imkiva.xart.editor.api.lexer.TokenConverter;

import java.io.StringReader;
import java.util.List;

public class LanguageTokenizeAdapter implements ITokenizeAdapter {
    private ParserDefinition parserDefinition;

    public LanguageTokenizeAdapter(ParserDefinition parserDefinition) {
        this.parserDefinition = parserDefinition;
    }

    @Override
    public List<HighlightSpan> tokenize(Flag flag, String needToLex) {
        JFlexLexer lexer = parserDefinition.createLexer();
        lexer.yyreset(new StringReader(needToLex));
        return TokenConverter.makeSpans(flag, lexer);
    }
}
