package com.imkiva.xart.language;

public class Language {
    public String getName() {
        return "Unknown";
    }

    public String getDescription() {
        return "Unknown language.";
    }

    public ParserDefinition getParserDefinition() {
        return new ParserDefinition(this);
    }
}
