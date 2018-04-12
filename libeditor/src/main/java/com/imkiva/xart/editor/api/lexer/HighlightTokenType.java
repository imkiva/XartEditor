package com.imkiva.xart.editor.api.lexer;

public enum HighlightTokenType {
    /**
     * 关键词
     */
    KEYWORD,
    /**
     * 函数名
     */
    FUNCTION_NAME,
    /**
     * 变量名
     */
    VAR_NAME,
    /**
     * 符号
     */
    SYMBOL,
    /**
     * 注释
     */
    COMMENT,

    /**
     * 非关键词
     */
    NORMAL,

    /**
     * 类型声明
     */
    TYPE,

    /**
     * 忽略Token
     */
    NOT_USE


}
