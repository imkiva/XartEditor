package com.imkiva.xart.language;

import com.imkiva.xart.language.helper.JFlexLexerAdapter;
import com.imkiva.xart.language.helper.LanguageTokenizeAdapter;
import com.imkiva.xart.editor.api.adapter.ITokenizeAdapter;

public class ParserDefinition {
    public final static char EOF = '\uFFFF';
    public final static char NULL_CHAR = '\u0000';
    public final static char NEWLINE = '\n';
    public final static char BACKSPACE = '\b';
    public final static char TAB = '\t';

    private Language language;

    public ParserDefinition(Language language) {
        this.language = language;
    }

    public Language getLanguage() {
        return language;
    }

    public boolean isWhitespace(char c) {
        return (c == ' ' || c == '\n' || c == '\t' ||
                c == '\r' || c == '\f' || c == EOF);
    }

    public boolean isSentenceTerminatorForced() {
        return false;
    }

    public boolean isSentenceTerminator(char c) {
        return isSentenceTerminatorForced() && (c == '.');
    }

    public JFlexLexer createLexer() {
        return new JFlexLexerAdapter();
    }

    public ITokenizeAdapter getTokenizeAdapter() {
        return new LanguageTokenizeAdapter(this);
    }
}
