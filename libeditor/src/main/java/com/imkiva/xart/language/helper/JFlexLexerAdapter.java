package com.imkiva.xart.language.helper;

import com.imkiva.xart.language.JFlexLexer;
import com.imkiva.xart.language.LanguageToken;

import java.io.IOException;
import java.io.Reader;

public class JFlexLexerAdapter extends JFlexLexer {
    @Override
    public void yyreset(Reader reader) {
    }

    @Override
    public LanguageToken advance() throws IOException {
        return null;
    }

    @Override
    public char yycharat(int pos) {
        return 0;
    }

    @Override
    public int yylength() {
        return 0;
    }

    @Override
    public String yytext() {
        return null;
    }

    @Override
    public int yychar() {
        return 0;
    }
}
