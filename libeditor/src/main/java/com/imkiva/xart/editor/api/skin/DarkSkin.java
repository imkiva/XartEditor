package com.imkiva.xart.editor.api.skin;

public class DarkSkin extends LightSkin {

    private static DarkSkin INSTANCE = new DarkSkin();

    private DarkSkin() {
        super();
        setColor(Colorable.BACKGROUND, OFF_BLACK);
        setColor(Colorable.FOREGROUND, OFF_WHITE);
        setColor(Colorable.CURSOR_BACKGROUND, OFF_WHITE);
    }

    public static DarkSkin getInstance() {
        return INSTANCE;
    }
}
