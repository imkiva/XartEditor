package com.imkiva.xart.language;

public interface ILanguageManager {
    void registerLanguage(Language language);

    Language getLanguage(String langName);
}
