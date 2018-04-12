package com.imkiva.xart.language;/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License 
 *       at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 */

import java.io.IOException;
import java.io.Reader;

/**
 * This is a default, and abstract implemenatation of a Lexer using JFLex
 * with some utility methods that Lexers can implement.
 *
 * @author Ayman Al-Sairafi
 */
public abstract class JFlexLexer {

    protected int tokenStart;
    protected int tokenLength;
    protected int offset;

    /**
     * Helper method to create and return a new Token from of TokenType
     * tokenStart and tokenLength will be modified to the newStart and
     * newLength params
     *
     * @param type
     * @param tStart
     * @param tLength
     * @param newStart
     * @param newLength
     * @return
     */
    protected LanguageToken token(LanguageTokenType type, int tStart, int tLength,
                                  int newStart, int newLength) {
        tokenStart = newStart;
        tokenLength = newLength;
        return new LanguageToken(type, tStart + offset, tLength);
    }

    /**
     * Create and return a Token of given type from start with length
     * offset is added to start
     *
     * @param type
     * @param start
     * @param length
     * @return
     */
    protected LanguageToken token(LanguageTokenType type, int start, int length) {
        return new LanguageToken(type, start + offset, length);
    }

    /**
     * Create and return a Token of given type.  start is obtained from {@link yychar()}
     * and length from {@link yylength()}
     * offset is added to start
     *
     * @param type
     * @return
     */
    protected LanguageToken token(LanguageTokenType type) {
        return new LanguageToken(type, yychar() + offset, yylength());
    }

    /**
     * Create and return a Token of given type and pairValue.
     * start is obtained from {@link yychar()}
     * and length from {@link yylength()}
     * offset is added to start
     *
     * @param type
     * @param pairValue
     * @return
     */
    protected LanguageToken token(LanguageTokenType type, int pairValue) {
        return new LanguageToken(type, yychar() + offset, yylength(), (byte) pairValue);
    }


    /**
     * This will be called to reset the the lexer.
     * This is created automatically by JFlex.
     *
     * @param reader
     */
    public abstract void yyreset(Reader reader);

    /**
     * This is called to return the next Token from the Input Reader
     *
     * @return next token, or null if no more tokens.
     * @throws IOException
     */
    public abstract LanguageToken advance() throws IOException;

    /**
     * Returns the character at position <tt>pos</tt> from the
     * matched text.
     * <p>
     * It is equivalent to yytext().charAt(pos), but faster
     *
     * @param pos the position of the character to fetch.
     *            A value from 0 to yylength()-1.
     * @return the character at position pos
     */
    public abstract char yycharat(int pos);

    /**
     * Returns the length of the matched text region.
     * This method is automatically implemented by JFlex lexers
     *
     * @return
     */
    public abstract int yylength();

    /**
     * Returns the text matched by the current regular expression.
     * This method is automatically implemented by JFlex lexers
     *
     * @return
     */
    public abstract String yytext();

    /**
     * Return the char number from beginning of input stream.
     * This is NOT implemented by JFLex, so the code must be
     * added to create this and return the private yychar field
     *
     * @return
     */
    public abstract int yychar();

    public void yyclose() throws IOException {
    }
}
