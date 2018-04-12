package com.imkiva.xart.editor.api.lexer;

import com.imkiva.xart.language.JFlexLexer;
import com.imkiva.xart.language.LanguageToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TokenConverter {

    /**
     * @param lexer 词法分析器
     * @return 词法分析结果
     */
    public static List<HighlightSpan> makeSpans(Flag flag, JFlexLexer lexer) {
        List<HighlightSpan> highlightSpans = new ArrayList<HighlightSpan>(50);
        LanguageToken languageToken;
        try {
            while (((languageToken = lexer.advance()) != null) && !flag.isSet()) {
                switch (languageToken.type) {
                    case KEYWORD2:
                    case KEYWORD:
                        highlightSpans.add(new HighlightSpan(languageToken.start, HighlightTokenType.KEYWORD));
                        break;
                    case OPERATOR:
                        highlightSpans.add(new HighlightSpan(languageToken.start, HighlightTokenType.KEYWORD));
                        break;
                    case STRING2:
                    case STRING:
                        highlightSpans.add(new HighlightSpan(languageToken.start, HighlightTokenType.SYMBOL));
                        break;
                    case NUMBER:
                        highlightSpans.add(new HighlightSpan(languageToken.start, HighlightTokenType.SYMBOL));
                        break;
                    case TYPE3:
                    case TYPE2:
                    case TYPE:
                        highlightSpans.add(new HighlightSpan(languageToken.start, HighlightTokenType.TYPE));
                        break;
                    case IDENTIFIER:
                        highlightSpans.add(new HighlightSpan(languageToken.start, HighlightTokenType.NORMAL));
                        break;
                    case COMMENT2:
                    case COMMENT:
                        highlightSpans.add(new HighlightSpan(languageToken.start, HighlightTokenType.COMMENT));
                        break;
                    default:
                        highlightSpans.add(new HighlightSpan(languageToken.start, HighlightTokenType.NORMAL));
                        break;
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            lexer.yyclose();
        } catch (IOException ignored) {
        }

        if (highlightSpans.size() == 0) {
            highlightSpans.add(HighlightSpan.FIRST_TOKEN);
        }
        return highlightSpans;

    }


}
